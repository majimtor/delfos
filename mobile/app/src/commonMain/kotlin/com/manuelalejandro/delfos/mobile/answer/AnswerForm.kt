package com.manuelalejandro.delfos.mobile.answer

import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.definition.SurveyDefinition
import com.manuelalejandro.delfos.evaluation.Answer
import com.manuelalejandro.delfos.evaluation.FieldError
import com.manuelalejandro.delfos.evaluation.FieldErrorCode
import com.manuelalejandro.delfos.evaluation.RulesEvaluator
import kotlinx.serialization.json.JsonPrimitive

/** What the respondent typed or chose for one question, before it becomes an `Answer` of the contract. */
internal data class AnswerValue(
    val optionKey: String? = null,
    val optionKeys: List<String> = emptyList(),
    /** The number as typed, so that the field can hold an unfinished value; a decimal comma is accepted. */
    val numberText: String = "",
    val text: String = "",
) {
    val number: Double?
        get() = numberText.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    val hasUnreadableNumber: Boolean
        get() = numberText.isNotBlank() && number == null
}

/** A field error of the contract attributed to the question it concerns. */
internal data class QuestionError(
    val questionKey: String,
    val code: FieldErrorCode,
    val params: Map<String, JsonPrimitive>,
)

/** The form as the shared evaluator sees it: visible questions, their errors, and the answers to send. */
internal data class FormEvaluation(
    val visible: List<Question>,
    val errors: Map<String, List<QuestionError>>,
    val missingRequired: Set<String>,
    val toSend: List<Answer>,
)

/**
 * Evaluates the respondent's values with the shared evaluator, which applies the same rules as the
 * server at submission. Only visible questions carry an answer, so a question hidden by a rule
 * never influences another rule; rules only look at earlier questions, so the visible set settles
 * in a few passes.
 */
internal class AnswerForm(private val definition: SurveyDefinition) {
    private val evaluator = RulesEvaluator(definition)

    fun evaluate(values: Map<String, AnswerValue>): FormEvaluation {
        var visible = definition.questions.mapTo(LinkedHashSet()) { it.key }
        var sent = answersToSend(values, visible)
        var result = evaluator.evaluate(sent)
        var passes = 0
        while (result.visibleQuestionKeys.toSet() != visible && passes < MAX_PASSES) {
            visible = result.visibleQuestionKeys.toCollection(LinkedHashSet())
            sent = answersToSend(values, visible)
            result = evaluator.evaluate(sent)
            passes++
        }
        val visibleQuestions = definition.questions.filter { it.key in visible }
        val errors = attributeErrors(result.errors, sent).toMutableList()
        for (question in visibleQuestions) {
            val value = values[question.key] ?: continue
            if (question.type.isNumeric && value.hasUnreadableNumber) {
                errors += QuestionError(question.key, FieldErrorCode.INVALID_VALUE, emptyMap())
            }
        }
        return FormEvaluation(
            visible = visibleQuestions,
            errors = errors.groupBy { it.questionKey },
            missingRequired = result.missingRequiredQuestionKeys.toSet(),
            toSend = sent,
        )
    }

    /** The answers to send: only questions in [visible], in survey order, only with a value. */
    fun answersToSend(values: Map<String, AnswerValue>, visible: Set<String>): List<Answer> =
        definition.questions
            .filter { it.key in visible }
            .mapNotNull { question -> values[question.key]?.let { toAnswer(question, it) } }

    companion object {
        private const val MAX_PASSES = 8
        private val ANSWER_INDEX = Regex("""^answers\[(\d+)]""")

        fun toAnswer(question: Question, value: AnswerValue): Answer? = when (question.type) {
            QuestionType.SINGLE_CHOICE -> value.optionKey?.let { Answer(question.key, optionKey = it) }
            QuestionType.MULTIPLE_CHOICE -> value.optionKeys.takeIf { it.isNotEmpty() }?.let { Answer(question.key, optionKeys = it) }
            QuestionType.TEXT -> value.text.takeIf { it.isNotEmpty() }?.let { Answer(question.key, text = it) }
            QuestionType.NUMBER, QuestionType.SCALE -> value.number?.let { Answer(question.key, number = it) }
        }

        /** Maps the paths of the contract (`answers[2].number`) back to question keys through the answers that were sent. */
        fun attributeErrors(errors: List<FieldError>, sent: List<Answer>): List<QuestionError> =
            errors.mapNotNull { error ->
                val params = error.params ?: emptyMap()
                if (error.code == FieldErrorCode.MISSING_REQUIRED_ANSWER) {
                    params["questionKey"]?.content?.let { QuestionError(it, error.code, params) }
                } else {
                    val index = ANSWER_INDEX.find(error.path)?.groupValues?.get(1)?.toIntOrNull()
                    index?.let(sent::getOrNull)?.let { QuestionError(it.questionKey, error.code, params) }
                }
            }
    }
}

internal val QuestionType.isNumeric: Boolean
    get() = this == QuestionType.NUMBER || this == QuestionType.SCALE
