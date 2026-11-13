package com.manuelalejandro.delfos.invitations

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.domain.InvitationStatus
import com.manuelalejandro.delfos.domain.RespondentMode
import com.manuelalejandro.delfos.domain.SurveyStatus
import com.manuelalejandro.delfos.persistence.InvitationEntity
import com.manuelalejandro.delfos.persistence.InvitationRepository
import com.manuelalejandro.delfos.persistence.SurveyEntity
import com.manuelalejandro.delfos.persistence.SurveyRepository
import com.manuelalejandro.delfos.survey.Cursors
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Limit
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** The manager side of invited mode: invitations are created in batches, listed with their state and revoked one by one. */
@Service
class InvitationService(
    private val surveys: SurveyRepository,
    private val invitations: InvitationRepository,
    private val clock: Clock,
) {

    class Created(val invitation: InvitationEntity, val token: String)

    class Page(val items: List<InvitationEntity>, val nextCursor: String?)

    @Transactional
    fun create(ownerId: UUID, surveyId: UUID, count: Int): List<Created> {
        val survey = ownedForUpdate(ownerId, surveyId)
        if (survey.respondentMode != RespondentMode.INVITED) {
            throw conflict("The survey is answered anonymously; it takes no invitations.")
        }
        if (survey.status == SurveyStatus.CLOSED) throw conflict("A closed survey takes no invitations.")
        val now = clock.instant()
        return List(count) {
            val token = InvitationTokens.newToken()
            Created(invitations.save(InvitationEntity(UUID.randomUUID(), surveyId, InvitationTokens.hash(token), createdAt = now)), token)
        }
    }

    @Transactional(readOnly = true)
    fun list(ownerId: UUID, surveyId: UUID, cursor: String?, limit: Int): Page {
        owned(ownerId, surveyId)
        val page = Limit.of(limit + 1)
        val items = if (cursor == null) {
            invitations.findFirstPage(surveyId, page)
        } else {
            val (createdAt, id) = Cursors.decode(cursor, 2)
            val after = runCatching { Instant.parse(createdAt) to UUID.fromString(id) }.getOrNull() ?: Cursors.decode("", 2).let { error("unreachable") }
            invitations.findPageAfter(surveyId, after.first, after.second, page)
        }
        return Page(items.take(limit), items.getOrNull(limit)?.let { Cursors.encode(items[limit - 1].createdAt.toString(), items[limit - 1].id.toString()) })
    }

    @Transactional
    fun revoke(ownerId: UUID, surveyId: UUID, invitationId: UUID) {
        ownedForUpdate(ownerId, surveyId)
        val invitation = invitations.findByIdAndSurveyId(invitationId, surveyId)
            ?: throw ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "No such invitation.")
        when (invitation.status) {
            InvitationStatus.USED -> throw conflict("A used invitation cannot be revoked.")
            InvitationStatus.REVOKED -> Unit
            InvitationStatus.PENDING -> {
                invitation.status = InvitationStatus.REVOKED
                invitation.revokedAt = clock.instant()
            }
        }
    }

    private fun owned(ownerId: UUID, surveyId: UUID): SurveyEntity =
        surveys.findByIdAndOwnerId(surveyId, ownerId) ?: throw noSuchSurvey()

    private fun ownedForUpdate(ownerId: UUID, surveyId: UUID): SurveyEntity =
        surveys.lockByIdAndOwnerId(surveyId, ownerId) ?: throw noSuchSurvey()

    private fun noSuchSurvey() = ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "No such survey.")

    private fun conflict(detail: String) = ApiProblemException(HttpStatus.CONFLICT, ProblemCode.INVALID_STATE_TRANSITION, detail)
}
