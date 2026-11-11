package com.manuelalejandro.delfos.survey

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.model.CreateSurveyRequest
import com.manuelalejandro.delfos.api.generated.model.DraftDefinition
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.api.generated.model.UpdateSurveyRequest
import com.manuelalejandro.delfos.definition.SurveyDefinition
import com.manuelalejandro.delfos.domain.RespondentMode
import com.manuelalejandro.delfos.domain.SurveyStatus
import com.manuelalejandro.delfos.domain.VersionStatus
import com.manuelalejandro.delfos.evaluation.DefinitionValidator
import com.manuelalejandro.delfos.evaluation.ValidationMode
import com.manuelalejandro.delfos.persistence.QuestionRepository
import com.manuelalejandro.delfos.persistence.SurveyEntity
import com.manuelalejandro.delfos.persistence.SurveyRepository
import com.manuelalejandro.delfos.persistence.SurveyResponseRepository
import com.manuelalejandro.delfos.persistence.SurveyVersionEntity
import com.manuelalejandro.delfos.persistence.SurveyVersionRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Limit
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The survey aggregate as the contract describes it: a survey with at most one draft and one
 * published version, published versions immutable, and every operation scoped to the owner.
 * A survey of another manager is reported as not found, never as forbidden.
 */
@Service
class SurveyService(
    private val surveys: SurveyRepository,
    private val versions: SurveyVersionRepository,
    private val questions: QuestionRepository,
    private val responses: SurveyResponseRepository,
    private val clock: Clock,
) {

    class SurveyPage(val items: List<SurveyEntity>, val nextCursor: String?)

    class VersionPage(val items: List<SurveyVersionEntity>, val nextCursor: String?)

    @Transactional
    fun create(ownerId: UUID, request: CreateSurveyRequest): SurveyEntity {
        val now = clock.instant()
        val survey = SurveyEntity(
            id = UUID.randomUUID(),
            publicId = newPublicId(),
            ownerId = ownerId,
            title = request.title,
            description = request.description,
            language = request.language.value,
            status = SurveyStatus.DRAFT,
            respondentMode = request.respondentMode?.let { RespondentMode.valueOf(it.value) } ?: RespondentMode.ANONYMOUS,
            closesAt = request.closesAt?.toInstant(),
            maxResponses = request.maxResponses,
            createdAt = now,
            updatedAt = now,
        )
        surveys.save(survey)
        versions.save(SurveyVersionEntity(UUID.randomUUID(), survey.id, number = 1, status = VersionStatus.DRAFT, createdAt = now))
        return survey
    }

    @Transactional(readOnly = true)
    fun list(ownerId: UUID, status: SurveyStatus?, cursor: String?, limit: Int): SurveyPage {
        val statuses = status?.let { listOf(it) } ?: SurveyStatus.entries
        val page = Limit.of(limit + 1)
        val items = if (cursor == null) {
            surveys.findFirstPage(ownerId, statuses, page)
        } else {
            val (updatedAt, id) = Cursors.decode(cursor, 2)
            val after = runCatching { Instant.parse(updatedAt) to UUID.fromString(id) }.getOrNull() ?: Cursors.decode("", 2).let { error("unreachable") }
            surveys.findPageAfter(ownerId, statuses, after.first, after.second, page)
        }
        return SurveyPage(items.take(limit), items.getOrNull(limit)?.let { Cursors.encode(items[limit - 1].updatedAt.toString(), items[limit - 1].id.toString()) })
    }

    @Transactional(readOnly = true)
    fun get(ownerId: UUID, surveyId: UUID): SurveyEntity = owned(ownerId, surveyId)

    @Transactional
    fun update(ownerId: UUID, surveyId: UUID, request: UpdateSurveyRequest): SurveyEntity {
        val survey = owned(ownerId, surveyId)
        request.title?.let { survey.title = it }
        if (request.description.isPresent) survey.description = request.description.get()
        if (request.closesAt.isPresent) survey.closesAt = request.closesAt.get()?.toInstant()
        if (request.maxResponses.isPresent) survey.maxResponses = request.maxResponses.get()
        survey.updatedAt = clock.instant()
        return survey
    }

    @Transactional
    fun delete(ownerId: UUID, surveyId: UUID) {
        surveys.delete(owned(ownerId, surveyId))
    }

    @Transactional
    fun publish(ownerId: UUID, surveyId: UUID): SurveyEntity {
        val survey = owned(ownerId, surveyId)
        if (survey.status == SurveyStatus.CLOSED) throw conflict(ProblemCode.INVALID_STATE_TRANSITION, "A closed survey cannot be published.")
        val draft = versions.findBySurveyIdAndStatus(surveyId, VersionStatus.DRAFT) ?: throw conflict(ProblemCode.NO_DRAFT, "The survey has no draft to publish.")
        val errors = DefinitionValidator.validate(DefinitionCodec.definitionOf(draft), ValidationMode.PUBLISH)
        if (errors.isNotEmpty()) throw ApiProblemException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, "${errors.size} fields are invalid.", errors)
        val now = clock.instant()
        versions.findBySurveyIdAndStatus(surveyId, VersionStatus.PUBLISHED)?.let {
            it.status = VersionStatus.SUPERSEDED
            // Flushed before the draft becomes published, so the one-published-version index holds at every step.
            versions.saveAndFlush(it)
        }
        draft.status = VersionStatus.PUBLISHED
        draft.publishedAt = now
        survey.status = SurveyStatus.PUBLISHED
        survey.updatedAt = now
        return survey
    }

    @Transactional
    fun close(ownerId: UUID, surveyId: UUID): SurveyEntity {
        val survey = owned(ownerId, surveyId)
        if (survey.status != SurveyStatus.PUBLISHED) throw conflict(ProblemCode.INVALID_STATE_TRANSITION, "Only a published survey can be closed.")
        survey.status = SurveyStatus.CLOSED
        survey.updatedAt = clock.instant()
        return survey
    }

    @Transactional(readOnly = true)
    fun listVersions(ownerId: UUID, surveyId: UUID, cursor: String?, limit: Int): VersionPage {
        owned(ownerId, surveyId)
        val page = Limit.of(limit + 1)
        val items = if (cursor == null) {
            versions.findBySurveyIdOrderByNumberDesc(surveyId, page)
        } else {
            val number = Cursors.decode(cursor, 1).first().toIntOrNull() ?: Cursors.decode("", 1).let { error("unreachable") }
            versions.findBySurveyIdAndNumberLessThanOrderByNumberDesc(surveyId, number, page)
        }
        return VersionPage(items.take(limit), items.getOrNull(limit)?.let { Cursors.encode(items[limit - 1].number.toString()) })
    }

    @Transactional(readOnly = true)
    fun getVersion(ownerId: UUID, surveyId: UUID, number: Int): SurveyVersionEntity {
        owned(ownerId, surveyId)
        return versions.findBySurveyIdAndNumber(surveyId, number)?.also { it.questions.forEach { q -> q.options.size } }
            ?: throw ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "No such version.")
    }

    @Transactional(readOnly = true)
    fun getDraft(ownerId: UUID, surveyId: UUID): SurveyVersionEntity {
        owned(ownerId, surveyId)
        return draftOf(surveyId)
    }

    @Transactional
    fun replaceDraft(ownerId: UUID, surveyId: UUID, request: DraftDefinition): SurveyVersionEntity {
        val survey = owned(ownerId, surveyId)
        val draft = draftOf(surveyId)
        val definition = DefinitionCodec.fromRequest(request)
        val errors = DefinitionValidator.validate(definition, ValidationMode.SAVE)
        if (errors.isNotEmpty()) throw ApiProblemException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, "${errors.size} fields are invalid.", errors)
        replaceQuestions(draft, definition)
        survey.updatedAt = clock.instant()
        return draft
    }

    @Transactional
    fun createDraft(ownerId: UUID, surveyId: UUID): SurveyVersionEntity {
        val survey = owned(ownerId, surveyId)
        if (survey.status == SurveyStatus.CLOSED) throw conflict(ProblemCode.INVALID_STATE_TRANSITION, "A closed survey cannot be edited.")
        if (versions.findBySurveyIdAndStatus(surveyId, VersionStatus.DRAFT) != null) throw conflict(ProblemCode.DRAFT_ALREADY_EXISTS, "The survey already has a draft.")
        val published = versions.findBySurveyIdAndStatus(surveyId, VersionStatus.PUBLISHED) ?: throw conflict(ProblemCode.NO_DRAFT, "The survey has nothing to copy a draft from.")
        val nextNumber = (versions.findAllBySurveyIdOrderByNumberDesc(surveyId).firstOrNull()?.number ?: 0) + 1
        val now = clock.instant()
        val draft = versions.save(SurveyVersionEntity(UUID.randomUUID(), surveyId, nextNumber, VersionStatus.DRAFT, published.rules, now))
        draft.questions += DefinitionCodec.toEntities(draft, DefinitionCodec.definitionOf(published))
        versions.save(draft)
        survey.updatedAt = now
        return draft
    }

    @Transactional(readOnly = true)
    fun responseCount(surveyId: UUID): Int = responses.countBySurveyId(surveyId).toInt()

    @Transactional(readOnly = true)
    fun publishedVersionNumber(surveyId: UUID): Int? = versions.findBySurveyIdAndStatus(surveyId, VersionStatus.PUBLISHED)?.number

    @Transactional(readOnly = true)
    fun hasDraft(surveyId: UUID): Boolean = versions.findBySurveyIdAndStatus(surveyId, VersionStatus.DRAFT) != null

    @Transactional(readOnly = true)
    fun questionCount(versionId: UUID): Int = questions.countByVersionId(versionId).toInt()

    @Transactional(readOnly = true)
    fun responseCountOfVersion(versionId: UUID): Int = responses.countByVersionId(versionId).toInt()

    private fun replaceQuestions(draft: SurveyVersionEntity, definition: SurveyDefinition) {
        // Old rows are deleted and flushed before the new ones are inserted, because Hibernate inserts
        // before it deletes within one flush and the (version, position) key would collide.
        draft.questions.clear()
        versions.saveAndFlush(draft)
        draft.questions += DefinitionCodec.toEntities(draft, definition)
        draft.rules = DefinitionCodec.encodeRules(definition.rules)
        versions.save(draft)
    }

    private fun draftOf(surveyId: UUID): SurveyVersionEntity =
        versions.findBySurveyIdAndStatus(surveyId, VersionStatus.DRAFT)?.also { it.questions.forEach { q -> q.options.size } }
            ?: throw ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NO_DRAFT, "The survey has no draft.")

    private fun owned(ownerId: UUID, surveyId: UUID): SurveyEntity =
        surveys.findByIdAndOwnerId(surveyId, ownerId) ?: throw ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "No such survey.")

    private fun conflict(code: ProblemCode, detail: String) = ApiProblemException(HttpStatus.CONFLICT, code, detail)

    private fun newPublicId(): String {
        repeat(PUBLIC_ID_ATTEMPTS) {
            val candidate = Identifiers.newPublicId()
            if (surveys.findByPublicId(candidate) == null) return candidate
        }
        throw DataIntegrityViolationException("Could not generate a unique public id")
    }

    private companion object {
        const val PUBLIC_ID_ATTEMPTS = 5
    }
}
