package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.model.ComparableResults
import com.manuelalejandro.delfos.api.generated.model.NumberStats
import com.manuelalejandro.delfos.api.generated.model.OptionCount
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.api.generated.model.QuestionResult
import com.manuelalejandro.delfos.api.generated.model.ValueCount
import com.manuelalejandro.delfos.api.generated.model.VersionResults
import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.persistence.SurveyRepository
import com.manuelalejandro.delfos.persistence.SurveyVersionEntity
import com.manuelalejandro.delfos.persistence.SurveyVersionRepository
import com.manuelalejandro.delfos.survey.Cursors
import com.manuelalejandro.delfos.survey.DefinitionCodec.toModel
import com.manuelalejandro.delfos.survey.SurveyMapping.utc
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Aggregated results and raw listings, computed in SQL over the typed answers. */
@Service
class ResultsService(
    private val surveys: SurveyRepository,
    private val versions: SurveyVersionRepository,
    private val queries: AnswerQueries,
    private val clock: Clock,
) {

    class Tagged<T>(val body: T, val etag: String)

    class ResponsePage(val items: List<AnswerQueries.ResponseRow>, val answers: Map<UUID, List<com.manuelalejandro.delfos.evaluation.Answer>>, val nextCursor: String?)

    class AnswerPage(val items: List<AnswerQueries.AnswerRow>, val nextCursor: String?)

    @Transactional(readOnly = true)
    fun versionResults(ownerId: UUID, surveyId: UUID, number: Int): Tagged<VersionResults> {
        val version = version(ownerId, surveyId, number)
        val scope = AnswerQueries.Scope(listOf(version.id))
        val totals = queries.totals(scope)
        val results = aggregate(version.questions.map { it.toModel() }, scope)
        return Tagged(VersionResults(surveyId, number, totals.responseCount, clock.instant().utc(), results), etag("v$number", totals))
    }

    @Transactional(readOnly = true)
    fun comparableResults(ownerId: UUID, surveyId: UUID): Tagged<ComparableResults> {
        owned(ownerId, surveyId)
        val all = versions.findAllBySurveyIdOrderByNumberDesc(surveyId).sortedBy { it.number }
        all.forEach { v -> v.questions.forEach { it.options.size } }
        val totals = queries.totals(AnswerQueries.Scope(all.map { it.id }))

        // Questions by first appearance; comparable when every appearance has the same shape.
        val appearances = linkedMapOf<String, MutableList<Pair<Int, Question>>>()
        for (version in all) for (entity in version.questions) {
            appearances.getOrPut(entity.key) { mutableListOf() } += version.number to entity.toModel()
        }
        val comparable = appearances.filterValues { list -> list.all { sameShape(it.second, list.first().second) } }
        val versionIdsByKey = appearances.mapValues { (_, list) -> list.map { (n, _) -> all.first { it.number == n }.id } }
        val scope = AnswerQueries.Scope(all.map { it.id })
        val counts = queries.answerCounts(scope)
        val optionCounts = queries.optionCounts(scope)
        val numberStats = queries.numberStats(scope)
        val distributions = queries.scaleDistributions(scope)
        val results = appearances.map { (key, list) ->
            val latest = list.last().second
            val isComparable = key in comparable
            questionResult(latest, counts, optionCounts, numberStats, distributions, withAggregates = isComparable)
                .copy(comparable = isComparable, versionNumbers = list.map { it.first })
        }
        return Tagged(ComparableResults(surveyId, all.map { it.number }, totals.responseCount, clock.instant().utc(), results), etag("all", totals))
    }

    @Transactional(readOnly = true)
    fun listResponses(ownerId: UUID, surveyId: UUID, number: Int, cursor: String?, limit: Int): ResponsePage {
        val version = version(ownerId, surveyId, number)
        val rows = queries.responsePage(version.id, number, cursor?.let { keyset(it) }, limit + 1)
        val page = rows.take(limit)
        return ResponsePage(page, queries.answersOf(page.map { it.id }), rows.getOrNull(limit)?.let { Cursors.encode(page.last().submittedAt.toString(), page.last().id.toString()) })
    }

    @Transactional(readOnly = true)
    fun listAnswers(ownerId: UUID, surveyId: UUID, number: Int, questionKey: String, cursor: String?, limit: Int): AnswerPage {
        val version = version(ownerId, surveyId, number)
        if (version.questions.none { it.key == questionKey }) throw ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "No such question in this version.")
        val rows = queries.answerPage(version.id, questionKey, cursor?.let { keyset(it) }, limit + 1)
        val page = rows.take(limit)
        return AnswerPage(page, rows.getOrNull(limit)?.let { Cursors.encode(page.last().submittedAt.toString(), page.last().responseId.toString()) })
    }

    private fun aggregate(questions: List<Question>, scope: AnswerQueries.Scope): List<QuestionResult> {
        val counts = queries.answerCounts(scope)
        val optionCounts = queries.optionCounts(scope)
        val numberStats = queries.numberStats(scope)
        val distributions = queries.scaleDistributions(scope)
        return questions.map { questionResult(it, counts, optionCounts, numberStats, distributions, withAggregates = true) }
    }

    private fun questionResult(
        question: Question,
        counts: Map<String, Int>,
        optionCounts: Map<String, Map<String, Int>>,
        numberStats: Map<String, AnswerQueries.NumberStats>,
        distributions: Map<String, List<Pair<Int, Int>>>,
        withAggregates: Boolean,
    ): QuestionResult {
        val key = question.key
        val base = QuestionResult(key = key, text = question.text, type = question.type, answerCount = counts[key] ?: 0)
        if (!withAggregates) return base
        return when (question.type) {
            QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE -> base.copy(
                options = question.options.orEmpty().map { OptionCount(it.key, it.label, optionCounts[key]?.get(it.key) ?: 0) },
            )
            QuestionType.NUMBER -> base.copy(number = numberStats[key]?.toDto())
            QuestionType.SCALE -> base.copy(
                number = numberStats[key]?.toDto(),
                distribution = scaleValues(question).map { value -> ValueCount(value, distributions[key]?.firstOrNull { it.first == value }?.second ?: 0) },
            )
            QuestionType.TEXT -> base
        }
    }

    private fun scaleValues(question: Question): List<Int> {
        val min = question.effectiveScaleMin.toInt()
        val max = question.max?.toInt() ?: min
        return (min..max).toList()
    }

    private fun AnswerQueries.NumberStats.toDto() = NumberStats(count, mean, min, max)

    private fun sameShape(a: Question, b: Question): Boolean =
        a.type == b.type && (a.type !in setOf(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE) || a.options?.map { it.key }?.toSet() == b.options?.map { it.key }?.toSet())

    private fun etag(scope: String, totals: AnswerQueries.Totals): String =
        "\"$scope-${totals.responseCount}-${totals.lastSubmittedAt?.toEpochMilli() ?: 0}\""

    private fun keyset(cursor: String): Pair<Instant, UUID> {
        val (ts, id) = Cursors.decode(cursor, 2)
        return runCatching { Instant.parse(ts) to UUID.fromString(id) }.getOrElse { Cursors.decode("", 2); error("unreachable") }
    }

    private fun version(ownerId: UUID, surveyId: UUID, number: Int): SurveyVersionEntity {
        owned(ownerId, surveyId)
        return versions.findBySurveyIdAndNumber(surveyId, number)?.also { it.questions.forEach { q -> q.options.size } }
            ?: throw ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "No such version.")
    }

    private fun owned(ownerId: UUID, surveyId: UUID) {
        surveys.findByIdAndOwnerId(surveyId, ownerId) ?: throw ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "No such survey.")
    }
}
