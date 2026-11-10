package com.manuelalejandro.delfos.auth

import java.time.Instant
import java.util.UUID
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

/** How the session token reached the server; decides whether the client header is required. */
enum class SessionTransport { COOKIE, BEARER }

/** The manager behind a valid session. Comes from the session, never from a request body. */
data class SessionPrincipal(
    val accountId: UUID,
    val username: String,
    val isAdmin: Boolean,
    val sessionId: UUID,
    val expiresAt: Instant,
    val transport: SessionTransport,
)

class SessionAuthentication(private val session: SessionPrincipal) : AbstractAuthenticationToken(authoritiesOf(session)) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): SessionPrincipal = session

    private companion object {
        fun authoritiesOf(principal: SessionPrincipal): List<GrantedAuthority> = buildList {
            add(SimpleGrantedAuthority("ROLE_MANAGER"))
            if (principal.isAdmin) add(SimpleGrantedAuthority("ROLE_ADMIN"))
        }
    }
}

object CurrentSession {

    /** The principal of the request; controllers only run behind the security filter, so it exists. */
    fun require(): SessionPrincipal =
        (SecurityContextHolder.getContext().authentication as? SessionAuthentication)?.principal
            ?: error("No session principal in the security context")
}
