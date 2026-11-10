package com.manuelalejandro.delfos.persistence

import com.manuelalejandro.delfos.domain.AccountStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "account")
class AccountEntity(
    @Id
    val id: UUID,
    @Column(nullable = false)
    var username: String,
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,
    @Column(name = "is_admin", nullable = false)
    var isAdmin: Boolean = false,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AccountStatus = AccountStatus.ACTIVE,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
