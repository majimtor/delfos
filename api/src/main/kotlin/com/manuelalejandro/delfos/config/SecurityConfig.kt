package com.manuelalejandro.delfos.config

import com.manuelalejandro.delfos.api.Problems
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import tools.jackson.databind.json.JsonMapper

/**
 * Stateless API: no HTTP session, no form login, no Spring CSRF token. The session token of the
 * contract and the client header for cookie-authenticated requests are enforced by the session
 * filter, which arrives with the authentication feature. Until then every manager operation is
 * unauthenticated and answers 401.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity, jsonMapper: JsonMapper): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/api/v1/health", "/api/v1/public/**", "/actuator/health/**").permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(ProblemEntryPoint(jsonMapper)) }
        return http.build()
    }

    private class ProblemEntryPoint(private val jsonMapper: JsonMapper) : AuthenticationEntryPoint {
        override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
            val problem = Problems.of(HttpStatus.UNAUTHORIZED, ProblemCode.UNAUTHENTICATED, "A valid session is required.", request.requestURI)
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
            jsonMapper.writeValue(response.outputStream, problem)
        }
    }
}
