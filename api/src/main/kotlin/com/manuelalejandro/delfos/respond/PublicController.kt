package com.manuelalejandro.delfos.respond

import com.manuelalejandro.delfos.api.generated.PublicApi
import com.manuelalejandro.delfos.api.generated.model.EvaluationRequest
import com.manuelalejandro.delfos.api.generated.model.Language
import com.manuelalejandro.delfos.api.generated.model.PublicSurvey
import com.manuelalejandro.delfos.api.generated.model.SubmitResponseRequest
import com.manuelalejandro.delfos.api.generated.model.SubmittedResponse
import com.manuelalejandro.delfos.api.generated.model.SurveyStatus
import com.manuelalejandro.delfos.evaluation.EvaluationResult
import com.manuelalejandro.delfos.survey.SurveyMapping
import com.manuelalejandro.delfos.survey.SurveyMapping.utc
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicController(private val service: PublicSurveyService) : PublicApi {

    override fun getPublicSurvey(publicId: String): ResponseEntity<PublicSurvey> {
        val published = service.get(publicId)
        val survey = published.survey
        return ResponseEntity.ok(
            PublicSurvey(
                publicId = survey.publicId,
                title = survey.title,
                language = Language.entries.first { it.value == survey.language },
                status = SurveyStatus.valueOf(survey.status.name),
                acceptingResponses = published.acceptingResponses,
                version = SurveyMapping.version(published.version),
                description = survey.description,
                closesAt = survey.closesAt?.utc(),
            ),
        )
    }

    override fun evaluateAnswers(publicId: String, evaluationRequest: EvaluationRequest): ResponseEntity<EvaluationResult> =
        ResponseEntity.ok(service.evaluate(publicId, evaluationRequest.versionNumber, evaluationRequest.answers))

    override fun submitResponse(publicId: String, submitResponseRequest: SubmitResponseRequest): ResponseEntity<SubmittedResponse> {
        val submitted = service.submit(publicId, submitResponseRequest.versionNumber, submitResponseRequest.answers)
        return ResponseEntity.status(HttpStatus.CREATED).body(SubmittedResponse(submitted.response.id, submitted.response.submittedAt.utc()))
    }
}
