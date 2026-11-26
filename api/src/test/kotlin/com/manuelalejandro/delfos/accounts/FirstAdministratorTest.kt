package com.manuelalejandro.delfos.accounts

import com.manuelalejandro.delfos.domain.AccountStatus
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder

class FirstAdministratorTest {

    private val accounts: AccountRepository = mock(AccountRepository::class.java)
    private val encoder: PasswordEncoder = mock(PasswordEncoder::class.java)
    private val clock: Clock = Clock.fixed(Instant.parse("2026-11-26T10:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `creates the administrator while the instance has no account`() {
        `when`(accounts.count()).thenReturn(0L)
        `when`(encoder.encode("a-long-enough-password")).thenReturn("hash")
        `when`(accounts.save(any(AccountEntity::class.java))).thenAnswer { it.arguments[0] }

        FirstAdministrator(accounts, encoder, clock, "operator", "a-long-enough-password").apply()

        val saved = ArgumentCaptor.forClass(AccountEntity::class.java)
        verify(accounts).save(saved.capture())
        assertThat(saved.value.username).isEqualTo("operator")
        assertThat(saved.value.passwordHash).isEqualTo("hash")
        assertThat(saved.value.isAdmin).isTrue()
        assertThat(saved.value.status).isEqualTo(AccountStatus.ACTIVE)
        assertThat(saved.value.createdAt).isEqualTo(clock.instant())
    }

    @Test
    fun `leaves an instance with accounts as it is`() {
        `when`(accounts.count()).thenReturn(1L)
        FirstAdministrator(accounts, encoder, clock, "operator", "a-long-enough-password").apply()
        verify(accounts, never()).save(any(AccountEntity::class.java))
    }

    @Test
    fun `does nothing without the two variables`() {
        FirstAdministrator(accounts, encoder, clock, "", "").apply()
        verify(accounts, never()).count()
    }

    @Test
    fun `stops the startup on a username or a password outside the rules of an account`() {
        assertThatThrownBy { FirstAdministrator(accounts, encoder, clock, "Operator", "a-long-enough-password").apply() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("DELFOS_ADMIN_USERNAME")
        assertThatThrownBy { FirstAdministrator(accounts, encoder, clock, "operator", "short").apply() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("DELFOS_ADMIN_PASSWORD")
        assertThatThrownBy { FirstAdministrator(accounts, encoder, clock, "operator", "").apply() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("DELFOS_ADMIN_PASSWORD")
        verify(accounts, never()).save(any(AccountEntity::class.java))
    }
}
