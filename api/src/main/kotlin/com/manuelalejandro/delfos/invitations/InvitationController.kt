package com.manuelalejandro.delfos.invitations

import com.manuelalejandro.delfos.api.generated.InvitationsApi
import com.manuelalejandro.delfos.api.generated.model.CreateInvitationsRequest
import com.manuelalejandro.delfos.api.generated.model.CreatedInvitation
import com.manuelalejandro.delfos.api.generated.model.CreatedInvitations
import com.manuelalejandro.delfos.api.generated.model.Invitation
import com.manuelalejandro.delfos.api.generated.model.InvitationPage
import com.manuelalejandro.delfos.api.generated.model.InvitationStatus
import com.manuelalejandro.delfos.auth.CurrentSession
import com.manuelalejandro.delfos.persistence.InvitationEntity
import com.manuelalejandro.delfos.survey.SurveyMapping.utc
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class InvitationController(private val service: InvitationService) : InvitationsApi {

    override fun createInvitations(surveyId: UUID, createInvitationsRequest: CreateInvitationsRequest, xDelfosClient: String?): ResponseEntity<CreatedInvitations> {
        val created = service.create(CurrentSession.require().accountId, surveyId, createInvitationsRequest.count ?: 1)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            CreatedInvitations(created.map { CreatedInvitation(it.invitation.id, it.token, it.invitation.createdAt.utc()) }),
        )
    }

    override fun listInvitations(surveyId: UUID, cursor: String?, limit: Int): ResponseEntity<InvitationPage> {
        val page = service.list(CurrentSession.require().accountId, surveyId, cursor, limit)
        return ResponseEntity.ok(InvitationPage(page.items.map { toDto(it) }, page.nextCursor))
    }

    override fun revokeInvitation(surveyId: UUID, invitationId: UUID, xDelfosClient: String?): ResponseEntity<Unit> {
        service.revoke(CurrentSession.require().accountId, surveyId, invitationId)
        return ResponseEntity.noContent().build()
    }

    private fun toDto(entity: InvitationEntity) = Invitation(
        id = entity.id,
        status = InvitationStatus.valueOf(entity.status.name),
        createdAt = entity.createdAt.utc(),
        usedAt = entity.usedAt?.utc(),
        revokedAt = entity.revokedAt?.utc(),
    )
}
