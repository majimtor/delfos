package com.manuelalejandro.delfos.auth

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.AuthApi
import com.manuelalejandro.delfos.api.generated.model.CreateSessionRequest
import com.manuelalejandro.delfos.api.generated.model.Me
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.api.generated.model.Session
import java.time.Duration
import java.time.ZoneOffset
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(private val sessionService: SessionService) : AuthApi {

    override fun createSession(createSessionRequest: CreateSessionRequest, xDelfosClient: String?): ResponseEntity<Session> {
        val transport = when (createSessionRequest.transport) {
            CreateSessionRequest.Transport.BEARER -> SessionTransport.BEARER
            else -> SessionTransport.COOKIE
        }
        if (transport == SessionTransport.COOKIE && xDelfosClient.isNullOrBlank()) {
            throw ApiProblemException(HttpStatus.FORBIDDEN, ProblemCode.CSRF_REJECTED, "Logging in with the cookie transport requires the ${SessionAuthenticationFilter.CLIENT_HEADER} header.")
        }
        val created = sessionService.login(createSessionRequest.username, createSessionRequest.password, transport)
        val principal = created.principal
        val body = Session(
            accountId = principal.accountId,
            username = principal.username,
            isAdmin = principal.isAdmin,
            expiresAt = principal.expiresAt.atOffset(ZoneOffset.UTC),
            token = created.token.takeIf { transport == SessionTransport.BEARER },
        )
        val response = ResponseEntity.status(HttpStatus.CREATED)
        if (transport == SessionTransport.COOKIE) {
            response.header(HttpHeaders.SET_COOKIE, sessionCookie(created.token, SessionService.ABSOLUTE_TIMEOUT).toString())
        }
        return response.body(body)
    }

    override fun deleteCurrentSession(xDelfosClient: String?): ResponseEntity<Unit> {
        val principal = CurrentSession.require()
        sessionService.revoke(principal.sessionId)
        val response = ResponseEntity.noContent()
        if (principal.transport == SessionTransport.COOKIE) {
            response.header(HttpHeaders.SET_COOKIE, sessionCookie("", Duration.ZERO).toString())
        }
        return response.build()
    }

    override fun getMe(): ResponseEntity<Me> {
        val principal = CurrentSession.require()
        return ResponseEntity.ok(Me(principal.accountId, principal.username, principal.isAdmin, principal.expiresAt.atOffset(ZoneOffset.UTC)))
    }

    private fun sessionCookie(value: String, maxAge: Duration): ResponseCookie =
        ResponseCookie.from(SessionAuthenticationFilter.COOKIE_NAME, value)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/")
            .maxAge(maxAge)
            .build()
}
