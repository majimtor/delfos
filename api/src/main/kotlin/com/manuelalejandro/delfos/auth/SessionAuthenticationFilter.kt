package com.manuelalejandro.delfos.auth

import com.manuelalejandro.delfos.api.Problems
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.json.JsonMapper

/**
 * Reads the session token from the bearer header or the session cookie and, when it is valid,
 * authenticates the request. A cookie-authenticated mutating request without the client header
 * is rejected as a CSRF attempt before reaching any handler.
 */
@Component
class SessionAuthenticationFilter(
    private val sessionService: SessionService,
    private val jsonMapper: JsonMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val (token, transport) = tokenOf(request) ?: return chain.doFilter(request, response)
        val principal = sessionService.authenticate(token, transport) ?: return chain.doFilter(request, response)
        if (transport == SessionTransport.COOKIE && isMutating(request) && request.getHeader(CLIENT_HEADER).isNullOrBlank()) {
            reject(request, response)
            return
        }
        SecurityContextHolder.getContext().authentication = SessionAuthentication(principal)
        chain.doFilter(request, response)
    }

    private fun tokenOf(request: HttpServletRequest): Pair<String, SessionTransport>? {
        val authorization = request.getHeader("Authorization")
        if (authorization != null && authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            return authorization.substring(BEARER_PREFIX.length).trim() to SessionTransport.BEARER
        }
        val cookie = request.cookies?.firstOrNull { it.name == COOKIE_NAME }?.value?.takeIf { it.isNotBlank() }
        return cookie?.let { it to SessionTransport.COOKIE }
    }

    private fun isMutating(request: HttpServletRequest): Boolean =
        request.method !in setOf(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name())

    private fun reject(request: HttpServletRequest, response: HttpServletResponse) {
        val problem = Problems.of(HttpStatus.FORBIDDEN, ProblemCode.CSRF_REJECTED, "Cookie-authenticated requests that change state must send the $CLIENT_HEADER header.", request.requestURI)
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        jsonMapper.writeValue(response.outputStream, problem)
    }

    companion object {
        const val COOKIE_NAME = "__Host-session"
        const val CLIENT_HEADER = "X-Delfos-Client"
        private const val BEARER_PREFIX = "Bearer "
    }
}
