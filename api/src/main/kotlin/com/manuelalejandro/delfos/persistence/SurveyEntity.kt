package com.manuelalejandro.delfos.persistence

import com.manuelalejandro.delfos.domain.RespondentMode
import com.manuelalejandro.delfos.domain.SurveyStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "survey")
class SurveyEntity(
    @Id
    val id: UUID,
    @Column(name = "public_id", nullable = false)
    val publicId: String,
    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,
    @Column(nullable = false)
    var title: String,
    @Column
    var description: String? = null,
    /** Content language code as the contract spells it: `es` or `en`. */
    @Column(nullable = false)
    val language: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SurveyStatus = SurveyStatus.DRAFT,
    @Enumerated(EnumType.STRING)
    @Column(name = "respondent_mode", nullable = false)
    val respondentMode: RespondentMode = RespondentMode.ANONYMOUS,
    @Column(name = "multiple_responses_per_invitation", nullable = false)
    var multipleResponsesPerInvitation: Boolean = false,
    @Column(name = "closes_at")
    var closesAt: Instant? = null,
    @Column(name = "max_responses")
    var maxResponses: Int? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
