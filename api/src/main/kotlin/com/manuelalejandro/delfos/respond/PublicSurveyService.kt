package com.manuelalejandro.delfos.respond

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.domain.SurveyStatus
import com.manuelalejandro.delfos.domain.VersionStatus
import com.manuelalejandro.delfos.evaluation.Answer
import com.manuelalejandro.delfos.evaluation.EvaluationResult
import com.manuelalejandro.delfos.evaluation.FieldError
import com.manuelalejandro.delfos.evaluation.FieldErrorCode
import com.manuelalejandro.delfos.evaluation.RulesEvaluator
import com.manuelalejandro.delfos.persistence.SurveyEntity
import com.manuelalejandro.delfos.persistence.SurveyRepository
import com.manuelalejandro.delfos.persistence.SurveyResponseEntity
import com.manuelalejandro.delfos.persistence.SurveyResponseRepository
import com.manuelalejandro.delfos.persistence.SurveyVersionEntity
import com.manuelalejandro.delfos.persistence.SurveyVersionRepository
import com.manuelalejandro.delfos.survey.DefinitionCodec
import java.time.Clock
import kotlinx.serialization.json.JsonPrimitive
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The respondent side: no account, no token, nothing stored about the person. A survey closes by
 * itself when its closing time passes or its quota is reached, the first time anyone looks.
 */
@Service
class PublicSurveyService(
    private val surveys: SurveyRepository,
    private val versions: SurveyVersionRepository,
    private val responses: SurveyResponseRepository,
    private val writer: ResponseWriter,
    private val clock: Clock,
) {

    class PublishedSurvey(val survey: SurveyEntity, val version: SurveyVersionEntity, val acceptingResponses: Boolean)

    class Submitted(val response: SurveyResponseEntity)

    @Transactional
    fun get(publicId: String): PublishedSurvey {
        val survey = published(surveys.findByPublicId(publicId))
        val version = versions.findBySurveyIdAndStatus(survey.id, VersionStatus.PUBLISHED) ?: throw notFound()
        version.questions.forEach { it.options.size }
        return PublishedSurvey(survey, version, closeIfDue(survey))
    }

    @Transactional
    fun evaluate(publicId: String, versionNumber: Int, givenAnswers: List<Answer>): EvaluationResult {
        val survey = published(surveys.findByPublicId(publicId))
        return RulesEvaluator(DefinitionCodec.definitionOf(answerableVersion(survey, versionNumber))).evaluate(givenAnswers)
    }

    @Transactional
    fun submit(publicId: String, versionNumber: Int, givenAnswers: List<Answer>): Submitted {
        val survey = published(surveys.lockByPublicId(publicId))
        if (!closeIfDue(survey)) {
            val code = if (survey.maxResponses != null && responses.countBySurveyId(survey.id) >= survey.maxResponses!!) ProblemCode.RESPONSE_QUOTA_REACHED else ProblemCode.SURVEY_NOT_ACCEPTING_RESPONSES
            throw ApiProblemException(HttpStatus.CONFLICT, code, "The survey is not accepting responses.")
        }
        val version = answerableVersion(survey, versionNumber)
        val definition = DefinitionCodec.definitionOf(version)
        val evaluation = RulesEvaluator(definition).evaluate(givenAnswers)
        val missing = evaluation.missingRequiredQuestionKeys.map {
            FieldError("answers", FieldErrorCode.MISSING_REQUIRED_ANSWER, mapOf("questionKey" to JsonPrimitive(it)))
        }
        val errors = evaluation.errors + missing
        if (errors.isNotEmpty()) throw ApiProblemException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, "${errors.size} fields are invalid.", errors)

        val response = writer.store(version.id, definition, givenAnswers, clock.instant())
        survey.maxResponses?.let { quota -> if (responses.countBySurveyId(survey.id) >= quota) close(survey) }
        return Submitted(response)
    }

    /** Closes the survey when its closing time has passed or its quota is full; true when it still accepts responses. */
    private fun closeIfDue(survey: SurveyEntity): Boolean {
        if (survey.status != SurveyStatus.PUBLISHED) return false
        val now = clock.instant()
        val dueByTime = survey.closesAt?.let { !now.isBefore(it) } ?: false
        val dueByQuota = survey.maxResponses?.let { responses.countBySurveyId(survey.id) >= it } ?: false
        if (dueByTime || dueByQuota) {
            close(survey)
            return false
        }
        return true
    }

    private fun close(survey: SurveyEntity) {
        survey.status = SurveyStatus.CLOSED
        survey.updatedAt = clock.instant()
    }

    private fun answerableVersion(survey: SurveyEntity, number: Int): SurveyVersionEntity {
        val version = versions.findBySurveyIdAndNumber(survey.id, number) ?: throw notFound("No such version.")
        if (version.status == VersionStatus.DRAFT) {
            throw ApiProblemException(HttpStatus.CONFLICT, ProblemCode.VERSION_NOT_ANSWERABLE, "The version is a draft and cannot be answered.")
        }
        version.questions.forEach { it.options.size }
        return version
    }

    /** A survey exists for respondents once it has been published; a draft or an unknown id are the same not found. */
    private fun published(survey: SurveyEntity?): SurveyEntity =
        survey?.takeIf { it.status != SurveyStatus.DRAFT } ?: throw notFound()

    private fun notFound(detail: String = "No such survey.") = ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, detail)
}
