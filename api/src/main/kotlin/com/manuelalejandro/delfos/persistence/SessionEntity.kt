package com.manuelalejandro.delfos.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** A manager session. Only the SHA-256 of the opaque token is stored, as lowercase hex. */
@Entity
@Table(name = "session")
class SessionEntity(
    @Id
    val id: UUID,
    @Column(name = "account_id", nullable = false)
    val accountId: UUID,
    @Column(name = "token_hash", nullable = false)
    val tokenHash: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "last_seen_at", nullable = false)
    var lastSeenAt: Instant,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
)
