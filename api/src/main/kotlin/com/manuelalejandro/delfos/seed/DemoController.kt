package com.manuelalejandro.delfos.seed

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.DemoApi
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import jakarta.servlet.http.HttpServletRequest
import java.security.MessageDigest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * The demo reset: applies the seed again on request. The operation exists only where a reset token
 * is configured; the token is a shared secret of the deployment, not a session, and is compared in
 * constant time. The scheduled workflow of the public demo calls it once a day.
 */
@RestController
class DemoController(
    private val seed: DemoSeed,
    private val request: HttpServletRequest,
    @Value("\${delfos.seed.reset-token:}") private val resetToken: String,
) : DemoApi {

    override fun resetDemo(): ResponseEntity<Unit> {
        if (resetToken.isBlank()) {
            throw ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "This instance has no demo reset.")
        }
        val presented = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
        if (presented.isNullOrEmpty() || !MessageDigest.isEqual(presented.toByteArray(), resetToken.toByteArray())) {
            throw ApiProblemException(HttpStatus.UNAUTHORIZED, ProblemCode.UNAUTHENTICATED, "The demo reset requires its token.")
        }
        seed.seed()
        return ResponseEntity.noContent().build()
    }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }
}
