package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.ResultsApi
import com.manuelalejandro.delfos.api.generated.model.AnswerPage
import com.manuelalejandro.delfos.api.generated.model.AnswerRecord
import com.manuelalejandro.delfos.api.generated.model.ComparableResults
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.api.generated.model.ResponsePage
import com.manuelalejandro.delfos.api.generated.model.ResponseRecord
import com.manuelalejandro.delfos.api.generated.model.VersionResults
import com.manuelalejandro.delfos.auth.CurrentSession
import com.manuelalejandro.delfos.evaluation.FieldError
import com.manuelalejandro.delfos.evaluation.FieldErrorCode
import com.manuelalejandro.delfos.survey.SurveyMapping.utc
import java.util.UUID
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ResultsController(private val service: ResultsService) : ResultsApi {

    override fun getVersionResults(surveyId: UUID, versionNumber: Int, ifNoneMatch: String?): ResponseEntity<VersionResults> =
        tagged(service.versionResults(CurrentSession.require().accountId, surveyId, versionNumber), ifNoneMatch)

    override fun getComparableResults(surveyId: UUID, scope: String, ifNoneMatch: String?): ResponseEntity<ComparableResults> {
        if (scope != "comparable") {
            throw ApiProblemException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, "1 fields are invalid.", listOf(FieldError("scope", FieldErrorCode.INVALID_VALUE)))
        }
        return tagged(service.comparableResults(CurrentSession.require().accountId, surveyId), ifNoneMatch)
    }

    override fun listResponses(surveyId: UUID, versionNumber: Int, cursor: String?, limit: Int): ResponseEntity<ResponsePage> {
        val page = service.listResponses(CurrentSession.require().accountId, surveyId, versionNumber, cursor, limit)
        val items = page.items.map { ResponseRecord(it.id, it.versionNumber, it.submittedAt.utc(), page.answers[it.id].orEmpty()) }
        return ResponseEntity.ok(ResponsePage(items, page.nextCursor))
    }

    override fun listAnswers(surveyId: UUID, versionNumber: Int, questionKey: String, cursor: String?, limit: Int): ResponseEntity<AnswerPage> {
        val page = service.listAnswers(CurrentSession.require().accountId, surveyId, versionNumber, questionKey, cursor, limit)
        return ResponseEntity.ok(AnswerPage(page.items.map { AnswerRecord(it.responseId, it.submittedAt.utc(), it.answer) }, page.nextCursor))
    }

    override fun exportResponsesCsv(surveyId: UUID, versionNumber: Int): ResponseEntity<Resource> {
        val file = service.exportCsv(CurrentSession.require().accountId, surveyId, versionNumber)
        return ResponseEntity.ok()
            .contentType(CSV)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename).build().toString())
            // An InputStreamResource is written as it is read; no content length is computed up front.
            .body(InputStreamResource(file.body))
    }

    /** `304` without a body when the client already holds the current representation. */
    private fun <T : Any> tagged(result: ResultsService.Tagged<T>, ifNoneMatch: String?): ResponseEntity<T> =
        if (ifNoneMatch != null && ifNoneMatch.split(',').map { it.trim() }.contains(result.etag)) {
            ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(result.etag).build()
        } else {
            ResponseEntity.ok().eTag(result.etag).body(result.body)
        }

    private companion object {
        val CSV: MediaType = MediaType.parseMediaType("text/csv;charset=UTF-8")
    }
}
