package com.manuelalejandro.delfos.persistence

import com.manuelalejandro.delfos.domain.InvitationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** A single-use invitation to answer a survey in invited mode. The token itself is never stored, only its hash. */
@Entity
@Table(name = "invitation")
class InvitationEntity(
    @Id
    val id: UUID,
    @Column(name = "survey_id", nullable = false)
    val surveyId: UUID,
    @Column(name = "token_hash", nullable = false)
    val tokenHash: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: InvitationStatus = InvitationStatus.PENDING,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "used_at")
    var usedAt: Instant? = null,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
)
