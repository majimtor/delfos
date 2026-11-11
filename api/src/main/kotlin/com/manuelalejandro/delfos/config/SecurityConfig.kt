package com.manuelalejandro.delfos.config

import com.manuelalejandro.delfos.api.Problems
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.auth.SessionAuthenticationFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.json.JsonMapper

/**
 * Stateless API: no HTTP session, no form login, no Spring CSRF token. Authentication is the
 * opaque session token of the contract, read by [SessionAuthenticationFilter], which also enforces
 * the client header on cookie-authenticated mutating requests.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity, jsonMapper: JsonMapper, sessionFilter: SessionAuthenticationFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(sessionFilter, AnonymousAuthenticationFilter::class.java)
            .authorizeHttpRequests {
                it.requestMatchers("/api/v1/health", "/api/v1/public/**", "/actuator/health/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/auth/sessions").permitAll()
                // The demo reset checks its own shared token; it is not a session.
                it.requestMatchers(HttpMethod.POST, "/api/v1/demo/reset").permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(ProblemEntryPoint(jsonMapper)) }
        return http.build()
    }

    /** CORS only for the configured web origin; without one the API is same-origin only. */
    @Bean
    fun corsConfigurationSource(@Value("\${delfos.web-origin:}") webOrigin: String): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        if (webOrigin.isNotBlank()) {
            val config = CorsConfiguration().apply {
                allowedOrigins = listOf(webOrigin)
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION, HttpHeaders.IF_NONE_MATCH, SessionAuthenticationFilter.CLIENT_HEADER)
                exposedHeaders = listOf(HttpHeaders.ETAG, HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER)
                allowCredentials = true
                maxAge = CORS_MAX_AGE_SECONDS
            }
            source.registerCorsConfiguration("/api/**", config)
        }
        return source
    }

    private class ProblemEntryPoint(private val jsonMapper: JsonMapper) : AuthenticationEntryPoint {
        override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
            val problem = Problems.of(HttpStatus.UNAUTHORIZED, ProblemCode.UNAUTHENTICATED, "A valid session is required.", request.requestURI)
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
            jsonMapper.writeValue(response.outputStream, problem)
        }
    }

    private companion object {
        const val CORS_MAX_AGE_SECONDS = 3600L
    }
}
