package com.manuelalejandro.delfos.auth

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.domain.AccountStatus
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import com.manuelalejandro.delfos.persistence.SessionEntity
import com.manuelalejandro.delfos.persistence.SessionRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Opaque session tokens: 256 random bits handed to the client once, stored only as a SHA-256 hash.
 * A session dies after [IDLE_TIMEOUT] without requests, after [ABSOLUTE_TIMEOUT] from creation,
 * on logout, and when its account is disabled.
 */
@Service
class SessionService(
    private val accounts: AccountRepository,
    private val sessions: SessionRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) {

    class CreatedSession(val token: String, val principal: SessionPrincipal)

    private val random = SecureRandom()

    /** A hash compared when the username is unknown, so both failures take the same time. */
    private val unknownUserHash: String = requireNotNull(passwordEncoder.encode(UUID.randomUUID().toString()))

    @Transactional
    fun login(username: String, password: String, transport: SessionTransport): CreatedSession {
        val account = accounts.findByUsernameIgnoreCase(username)
        val matches = passwordEncoder.matches(password, account?.passwordHash ?: unknownUserHash)
        if (account == null || !matches) {
            throw ApiProblemException(HttpStatus.UNAUTHORIZED, ProblemCode.INVALID_CREDENTIALS, "The credentials are not valid.")
        }
        if (account.status != AccountStatus.ACTIVE) {
            throw ApiProblemException(HttpStatus.UNAUTHORIZED, ProblemCode.ACCOUNT_DISABLED, "The account is disabled.")
        }
        val now = clock.instant()
        val token = newToken()
        val session = sessions.save(
            SessionEntity(
                id = UUID.randomUUID(),
                accountId = account.id,
                tokenHash = hash(token),
                createdAt = now,
                lastSeenAt = now,
                expiresAt = now.plus(ABSOLUTE_TIMEOUT),
            ),
        )
        return CreatedSession(token, principalOf(session, account, transport))
    }

    /** The principal of a token, or null when the token is unknown, expired, revoked or its account disabled. */
    @Transactional
    fun authenticate(token: String, transport: SessionTransport): SessionPrincipal? {
        val session = sessions.findByTokenHash(hash(token)) ?: return null
        val now = clock.instant()
        if (session.revokedAt != null || now >= session.expiresAt || now >= session.lastSeenAt.plus(IDLE_TIMEOUT)) return null
        val account = accounts.findById(session.accountId).orElse(null) ?: return null
        if (account.status != AccountStatus.ACTIVE) return null
        if (Duration.between(session.lastSeenAt, now) >= LAST_SEEN_WRITE_INTERVAL) {
            session.lastSeenAt = now
        }
        return principalOf(session, account, transport)
    }

    @Transactional
    fun revoke(sessionId: UUID) {
        sessions.findById(sessionId).ifPresent { if (it.revokedAt == null) it.revokedAt = clock.instant() }
    }

    private fun principalOf(session: SessionEntity, account: AccountEntity, transport: SessionTransport) =
        SessionPrincipal(account.id, account.username, account.isAdmin, session.id, session.expiresAt, transport)

    private fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        val IDLE_TIMEOUT: Duration = Duration.ofMinutes(30)
        val ABSOLUTE_TIMEOUT: Duration = Duration.ofHours(12)
        private val LAST_SEEN_WRITE_INTERVAL: Duration = Duration.ofMinutes(1)
        private const val TOKEN_BYTES = 32

        fun hash(token: String): String =
            MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        fun expiresAt(session: SessionEntity, now: Instant): Instant = minOf(session.expiresAt, session.lastSeenAt.plus(IDLE_TIMEOUT)).coerceAtLeast(now)
    }
}
