package com.manuelalejandro.delfos.accounts

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.auth.SessionPrincipal
import com.manuelalejandro.delfos.domain.AccountStatus
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import com.manuelalejandro.delfos.persistence.SessionRepository
import com.manuelalejandro.delfos.survey.Cursors
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Limit
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * What an administrator does and nothing more: create accounts, list them and disable them. The
 * surveys of other managers stay out of reach, as the plan decides.
 */
@Service
class AccountService(
    private val accounts: AccountRepository,
    private val sessions: SessionRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) {

    class Page(val items: List<AccountEntity>, val nextCursor: String?)

    @Transactional(readOnly = true)
    fun list(principal: SessionPrincipal, cursor: String?, limit: Int): Page {
        requireAdmin(principal)
        val page = Limit.of(limit + 1)
        val items = if (cursor == null) {
            accounts.findFirstPage(page)
        } else {
            val (createdAt, id) = Cursors.decode(cursor, 2)
            val after = runCatching { Instant.parse(createdAt) to UUID.fromString(id) }.getOrNull() ?: Cursors.decode("", 2).let { error("unreachable") }
            accounts.findPageAfter(after.first, after.second, page)
        }
        return Page(items.take(limit), items.getOrNull(limit)?.let { Cursors.encode(items[limit - 1].createdAt.toString(), items[limit - 1].id.toString()) })
    }

    @Transactional
    fun create(principal: SessionPrincipal, username: String, password: String, isAdmin: Boolean): AccountEntity {
        requireAdmin(principal)
        if (accounts.findByUsernameIgnoreCase(username) != null) {
            throw ApiProblemException(HttpStatus.CONFLICT, ProblemCode.USERNAME_TAKEN, "The username is taken.")
        }
        val now = clock.instant()
        return accounts.save(AccountEntity(UUID.randomUUID(), username, requireNotNull(passwordEncoder.encode(password)), isAdmin, AccountStatus.ACTIVE, now, now))
    }

    /** Disables the account and revokes its sessions; the filter would reject them anyway, the revocation makes it explicit. */
    @Transactional
    fun disable(principal: SessionPrincipal, accountId: UUID): AccountEntity {
        requireAdmin(principal)
        if (accountId == principal.accountId) {
            throw ApiProblemException(HttpStatus.CONFLICT, ProblemCode.INVALID_STATE_TRANSITION, "An administrator cannot disable their own account.")
        }
        val account = accounts.findById(accountId).orElseThrow { ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "No such account.") }
        if (account.status == AccountStatus.ACTIVE) {
            val now = clock.instant()
            account.status = AccountStatus.DISABLED
            account.updatedAt = now
            sessions.findAllByAccountId(accountId).forEach { if (it.revokedAt == null) it.revokedAt = now }
        }
        return account
    }

    private fun requireAdmin(principal: SessionPrincipal) {
        if (!principal.isAdmin) throw ApiProblemException(HttpStatus.FORBIDDEN, ProblemCode.ADMIN_REQUIRED, "Only an administrator manages accounts.")
    }
}
