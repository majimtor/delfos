package com.manuelalejandro.delfos.survey

import com.manuelalejandro.delfos.api.generated.DraftsApi
import com.manuelalejandro.delfos.api.generated.SurveysApi
import com.manuelalejandro.delfos.api.generated.VersionsApi
import com.manuelalejandro.delfos.api.generated.model.CreateSurveyRequest
import com.manuelalejandro.delfos.api.generated.model.DraftDefinition
import com.manuelalejandro.delfos.api.generated.model.Survey
import com.manuelalejandro.delfos.api.generated.model.SurveyPage
import com.manuelalejandro.delfos.api.generated.model.SurveyStatus
import com.manuelalejandro.delfos.api.generated.model.SurveyVersion
import com.manuelalejandro.delfos.api.generated.model.UpdateSurveyRequest
import com.manuelalejandro.delfos.api.generated.model.VersionPage
import com.manuelalejandro.delfos.auth.CurrentSession
import com.manuelalejandro.delfos.persistence.SurveyEntity
import java.net.URI
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class SurveyController(private val service: SurveyService) : SurveysApi {

    override fun createSurvey(createSurveyRequest: CreateSurveyRequest, xDelfosClient: String?): ResponseEntity<Survey> {
        val survey = service.create(CurrentSession.require().accountId, createSurveyRequest)
        return ResponseEntity.created(URI.create("/api/v1/surveys/${survey.id}")).body(toDto(survey))
    }

    override fun listSurveys(status: SurveyStatus?, cursor: String?, limit: Int): ResponseEntity<SurveyPage> {
        val page = service.list(CurrentSession.require().accountId, status?.let { com.manuelalejandro.delfos.domain.SurveyStatus.valueOf(it.value) }, cursor, limit)
        return ResponseEntity.ok(SurveyPage(items = page.items.map { toDto(it) }, nextCursor = page.nextCursor))
    }

    override fun getSurvey(surveyId: UUID): ResponseEntity<Survey> =
        ResponseEntity.ok(toDto(service.get(CurrentSession.require().accountId, surveyId)))

    override fun updateSurvey(surveyId: UUID, updateSurveyRequest: UpdateSurveyRequest, xDelfosClient: String?): ResponseEntity<Survey> =
        ResponseEntity.ok(toDto(service.update(CurrentSession.require().accountId, surveyId, updateSurveyRequest)))

    override fun deleteSurvey(surveyId: UUID, xDelfosClient: String?): ResponseEntity<Unit> {
        service.delete(CurrentSession.require().accountId, surveyId)
        return ResponseEntity.noContent().build()
    }

    override fun publishSurvey(surveyId: UUID, xDelfosClient: String?): ResponseEntity<Survey> =
        ResponseEntity.ok(toDto(service.publish(CurrentSession.require().accountId, surveyId)))

    override fun closeSurvey(surveyId: UUID, xDelfosClient: String?): ResponseEntity<Survey> =
        ResponseEntity.ok(toDto(service.close(CurrentSession.require().accountId, surveyId)))

    private fun toDto(survey: SurveyEntity): Survey =
        SurveyMapping.survey(survey, service.responseCount(survey.id), service.publishedVersionNumber(survey.id), service.hasDraft(survey.id))
}

@RestController
class DraftController(private val service: SurveyService) : DraftsApi {

    override fun getDraft(surveyId: UUID): ResponseEntity<SurveyVersion> =
        ResponseEntity.ok(SurveyMapping.version(service.getDraft(CurrentSession.require().accountId, surveyId)))

    override fun replaceDraft(surveyId: UUID, draftDefinition: DraftDefinition, xDelfosClient: String?): ResponseEntity<SurveyVersion> =
        ResponseEntity.ok(SurveyMapping.version(service.replaceDraft(CurrentSession.require().accountId, surveyId, draftDefinition)))

    override fun createDraft(surveyId: UUID, xDelfosClient: String?): ResponseEntity<SurveyVersion> =
        ResponseEntity.status(HttpStatus.CREATED).body(SurveyMapping.version(service.createDraft(CurrentSession.require().accountId, surveyId)))
}

@RestController
class VersionController(private val service: SurveyService) : VersionsApi {

    override fun listVersions(surveyId: UUID, cursor: String?, limit: Int): ResponseEntity<VersionPage> {
        val page = service.listVersions(CurrentSession.require().accountId, surveyId, cursor, limit)
        val items = page.items.map { SurveyMapping.summary(it, service.questionCount(it.id), service.responseCountOfVersion(it.id)) }
        return ResponseEntity.ok(VersionPage(items = items, nextCursor = page.nextCursor))
    }

    override fun getVersion(surveyId: UUID, versionNumber: Int): ResponseEntity<SurveyVersion> =
        ResponseEntity.ok(SurveyMapping.version(service.getVersion(CurrentSession.require().accountId, surveyId, versionNumber)))
}
