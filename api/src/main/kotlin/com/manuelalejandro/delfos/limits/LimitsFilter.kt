package com.manuelalejandro.delfos.limits

import com.manuelalejandro.delfos.api.Problems
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import java.time.Clock
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.json.JsonMapper

@ConfigurationProperties("delfos.limits")
class LimitsProperties(
    /** Requests per minute and client address on the public operations. */
    val publicPerMinute: Int = 120,
    /** Login attempts per minute and client address; the demo reset shares this limit. */
    val loginPerMinute: Int = 10,
    /** Largest request body accepted, in bytes. */
    val maxBodyBytes: Long = 256 * 1024,
)

/** Thrown while reading a body that exceeds the limit; rendered as `413 PAYLOAD_TOO_LARGE`. */
class PayloadTooLargeException(limit: Long) : RuntimeException("The request body exceeds $limit bytes.")

/**
 * The first filter of every request: rejects bodies above the size limit and throttles the
 * public operations and the login per client address, each kind in its own window, before
 * authentication and before any handler. The address comes from the proxy headers when the
 * server runs behind one.
 */
class LimitsFilter(
    private val properties: LimitsProperties,
    private val limiter: RateLimiter,
    private val jsonMapper: JsonMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val declared = request.getHeader(HttpHeaders.CONTENT_LENGTH)?.toLongOrNull()
        if (declared != null && declared > properties.maxBodyBytes) {
            reject(request, response, HttpStatus.CONTENT_TOO_LARGE, ProblemCode.PAYLOAD_TOO_LARGE, "The request body exceeds ${properties.maxBodyBytes} bytes.")
            return
        }
        val kind = kindOf(request)
        if (kind != null) {
            // One window per kind and address, so answering a survey never spends the login attempts.
            val decision = limiter.check("${kind.name}:${request.remoteAddr}", limitOf(kind))
            if (!decision.allowed) {
                response.setHeader(HttpHeaders.RETRY_AFTER, decision.retryAfterSeconds.toString())
                reject(request, response, HttpStatus.TOO_MANY_REQUESTS, ProblemCode.RATE_LIMITED, "Too many requests; retry after ${decision.retryAfterSeconds} seconds.")
                return
            }
        }
        chain.doFilter(CappedRequest(request, properties.maxBodyBytes), response)
    }

    private enum class Kind { PUBLIC, LOGIN }

    private fun kindOf(request: HttpServletRequest): Kind? = when {
        request.requestURI.startsWith("/api/v1/public/") -> Kind.PUBLIC
        request.requestURI == "/api/v1/auth/sessions" && request.method == HttpMethod.POST.name() -> Kind.LOGIN
        request.requestURI == "/api/v1/demo/reset" && request.method == HttpMethod.POST.name() -> Kind.LOGIN
        else -> null
    }

    private fun limitOf(kind: Kind): Int = when (kind) {
        Kind.PUBLIC -> properties.publicPerMinute
        Kind.LOGIN -> properties.loginPerMinute
    }

    private fun reject(request: HttpServletRequest, response: HttpServletResponse, status: HttpStatus, code: ProblemCode, detail: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        jsonMapper.writeValue(response.outputStream, Problems.of(status, code, detail, request.requestURI))
    }

    /** Caps what a handler can read, for bodies sent without a declared length. */
    private class CappedRequest(request: HttpServletRequest, private val limit: Long) : HttpServletRequestWrapper(request) {
        override fun getInputStream(): ServletInputStream = CappedStream(super.getInputStream(), limit)
    }

    private class CappedStream(private val delegate: ServletInputStream, private val limit: Long) : ServletInputStream() {
        private var read = 0L

        override fun read(): Int = delegate.read().also { if (it >= 0) count(1) }

        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len).also { if (it > 0) count(it) }

        private fun count(n: Int) {
            read += n
            if (read > limit) throw PayloadTooLargeException(limit)
        }

        override fun isFinished(): Boolean = delegate.isFinished
        override fun isReady(): Boolean = delegate.isReady
        override fun setReadListener(listener: ReadListener) = delegate.setReadListener(listener)
        override fun available(): Int = delegate.available()
        override fun close() = delegate.close()
    }
}

@Configuration
@EnableConfigurationProperties(LimitsProperties::class)
class LimitsConfig {

    @Bean
    fun rateLimiter(clock: Clock): RateLimiter = RateLimiter(clock)

    @Bean
    fun limitsFilter(properties: LimitsProperties, limiter: RateLimiter, jsonMapper: JsonMapper): FilterRegistrationBean<LimitsFilter> =
        FilterRegistrationBean(LimitsFilter(properties, limiter, jsonMapper)).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/api/*")
        }
}
