package com.manuelalejandro.delfos.accounts

import com.manuelalejandro.delfos.domain.AccountStatus
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import java.time.Clock
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * The first administrator of an instance run by someone else (ADR 0018): named by
 * `DELFOS_ADMIN_USERNAME` and `DELFOS_ADMIN_PASSWORD`, created at startup only while the instance
 * has no account at all, so that the environment never overrides what an administrator did later.
 * The values follow the rules of the contract for an account; a value that breaks them stops the
 * startup with the reason, which is what an operator wants to see.
 */
@Component
class FirstAdministrator(
    private val accounts: AccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
    @Value("\${delfos.admin.username:}") private val username: String,
    @Value("\${delfos.admin.password:}") private val password: String,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(FirstAdministrator::class.java)

    override fun run(args: ApplicationArguments) = apply()

    fun apply() {
        if (username.isBlank() && password.isBlank()) return
        check(USERNAME.matches(username)) {
            "DELFOS_ADMIN_USERNAME must be 3 to 64 lowercase letters, digits, dots, hyphens or underscores, starting with a letter or a digit."
        }
        check(password.length in PASSWORD_LENGTH) { "DELFOS_ADMIN_PASSWORD must be 12 to 256 characters." }
        if (accounts.count() > 0) {
            log.info("The instance has accounts; the first administrator of the environment is not applied")
            return
        }
        val now = clock.instant()
        accounts.save(AccountEntity(UUID.randomUUID(), username, requireNotNull(passwordEncoder.encode(password)), true, AccountStatus.ACTIVE, now, now))
        log.info("First administrator '{}' created from the environment", username)
    }

    companion object {
        private val USERNAME = Regex("^[a-z0-9][a-z0-9._-]{2,63}$")
        private val PASSWORD_LENGTH = 12..256
    }
}
