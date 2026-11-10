package com.manuelalejandro.delfos.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** A submitted response. Its answers are written and aggregated with SQL, not mapped as entities. */
@Entity
@Table(name = "response")
class SurveyResponseEntity(
    @Id
    val id: UUID,
    @Column(name = "version_id", nullable = false)
    val versionId: UUID,
    @Column(name = "invitation_id")
    val invitationId: UUID? = null,
    @Column(name = "submitted_at", nullable = false)
    val submittedAt: Instant,
)
