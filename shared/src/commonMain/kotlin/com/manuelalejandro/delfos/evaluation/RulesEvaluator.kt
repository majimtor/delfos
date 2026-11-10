package com.manuelalejandro.delfos.evaluation

import com.manuelalejandro.delfos.definition.Condition
import com.manuelalejandro.delfos.definition.ConditionOperator
import com.manuelalejandro.delfos.definition.MatchMode
import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.definition.SurveyDefinition
import kotlinx.serialization.json.JsonPrimitive

/**
 * Evaluates answers against a definition that has passed [DefinitionValidator]: which questions are
 * visible, which answers are invalid, and which visible required questions still lack a valid answer.
 * The semantics are fixed by the conformance suite in `contract/conformance`.
 */
public class RulesEvaluator(private val definition: SurveyDefinition) {

    private val questionsByKey: Map<String, Question> = definition.questions.associateBy { it.key }
    private val rulesByTarget = definition.rules.associateBy { it.targetQuestionKey }

    public fun evaluate(answers: List<Answer>): EvaluationResult {
        val errors = mutableListOf<FieldError>()
        val answerIndexByKey = mutableMapOf<String, Int>()
        val validValues = mutableMapOf<String, Any>()
        val shapeErrorsByKey = mutableMapOf<String, List<FieldError>>()

        answers.forEachIndexed { index, answer ->
            val question = questionsByKey[answer.questionKey]
            when {
                question == null -> errors += FieldError("answers[$index].questionKey", FieldErrorCode.UNKNOWN_QUESTION)
                answer.questionKey in answerIndexByKey -> errors += FieldError("answers[$index].questionKey", FieldErrorCode.DUPLICATE)
                else -> {
                    answerIndexByKey[answer.questionKey] = index
                    val checked = checkAnswer(question, answer, "answers[$index]")
                    shapeErrorsByKey[answer.questionKey] = checked.errors
                    if (checked.value != null) validValues[answer.questionKey] = checked.value
                }
            }
        }

        val visible = mutableListOf<String>()
        val effective = mutableMapOf<String, Any>()
        val missing = mutableListOf<String>()
        for (question in definition.questions) {
            val key = question.key
            val rule = rulesByTarget[key]
            val isVisible = rule == null || run {
                val results = rule.conditions.map { holds(it, effective) }
                if (rule.match == MatchMode.ALL) results.all { it } else results.any { it }
            }
            if (isVisible) {
                visible += key
                validValues[key]?.let { effective[key] = it }
                if (key in answerIndexByKey) errors += shapeErrorsByKey.getValue(key)
                if (question.required && key !in validValues) missing += key
            } else if (key in answerIndexByKey) {
                errors += FieldError("answers[${answerIndexByKey.getValue(key)}]", FieldErrorCode.HIDDEN_QUESTION_ANSWERED)
            }
        }
        return EvaluationResult(visible, errors, missing)
    }

    private class CheckedAnswer(val value: Any?, val errors: List<FieldError>)

    @Suppress("CyclomaticComplexMethod")
    private fun checkAnswer(question: Question, answer: Answer, path: String): CheckedAnswer {
        val present = buildList {
            if (answer.optionKey != null) add("optionKey")
            if (answer.optionKeys != null) add("optionKeys")
            if (answer.number != null) add("number")
            if (answer.text != null) add("text")
        }
        val expected = when (question.type) {
            QuestionType.SINGLE_CHOICE -> "optionKey"
            QuestionType.MULTIPLE_CHOICE -> "optionKeys"
            QuestionType.TEXT -> "text"
            QuestionType.NUMBER, QuestionType.SCALE -> "number"
        }
        if (present != listOf(expected)) {
            return CheckedAnswer(null, listOf(FieldError(path, FieldErrorCode.ANSWER_TYPE_MISMATCH)))
        }
        val errors = mutableListOf<FieldError>()
        val value: Any = when (question.type) {
            QuestionType.SINGLE_CHOICE -> {
                val key = answer.optionKey!!
                if (question.options.orEmpty().none { it.key == key }) {
                    errors += FieldError("$path.optionKey", FieldErrorCode.UNKNOWN_OPTION)
                }
                key
            }
            QuestionType.MULTIPLE_CHOICE -> {
                val keys = answer.optionKeys!!
                val known = question.options.orEmpty().map { it.key }.toSet()
                val seen = mutableSetOf<String>()
                keys.forEachIndexed { index, key ->
                    when {
                        key !in known -> errors += FieldError("$path.optionKeys[$index]", FieldErrorCode.UNKNOWN_OPTION)
                        !seen.add(key) -> errors += FieldError("$path.optionKeys[$index]", FieldErrorCode.DUPLICATE_OPTION)
                    }
                }
                if (errors.isEmpty()) {
                    val distinct = seen.size
                    if (distinct < question.effectiveMinSelections) {
                        errors += FieldError("$path.optionKeys", FieldErrorCode.TOO_FEW_SELECTIONS, mapOf("min" to JsonPrimitive(question.effectiveMinSelections)))
                    } else if (distinct > question.effectiveMaxSelections) {
                        errors += FieldError("$path.optionKeys", FieldErrorCode.TOO_MANY_SELECTIONS, mapOf("max" to JsonPrimitive(question.effectiveMaxSelections)))
                    }
                }
                seen.toSet()
            }
            QuestionType.TEXT -> {
                val text = answer.text!!
                if (text.codePointLength() > question.effectiveMaxLength) {
                    errors += FieldError("$path.text", FieldErrorCode.TOO_LONG, mapOf("max" to JsonPrimitive(question.effectiveMaxLength)))
                }
                text
            }
            QuestionType.NUMBER -> {
                val number = answer.number!!
                val min = question.min
                val max = question.max
                if (question.integerOnly == true && !number.isWholeNumber()) {
                    errors += FieldError("$path.number", FieldErrorCode.NOT_INTEGER)
                } else if ((min != null && number < min) || (max != null && number > max)) {
                    errors += FieldError("$path.number", FieldErrorCode.OUT_OF_RANGE, buildMap {
                        if (min != null) put("min", FieldError.number(min))
                        if (max != null) put("max", FieldError.number(max))
                    })
                }
                number
            }
            QuestionType.SCALE -> {
                val number = answer.number!!
                val min = question.effectiveScaleMin
                val max = question.max ?: Double.POSITIVE_INFINITY
                if (!number.isWholeNumber()) {
                    errors += FieldError("$path.number", FieldErrorCode.NOT_INTEGER)
                } else if (number < min || number > max) {
                    errors += FieldError("$path.number", FieldErrorCode.OUT_OF_RANGE, mapOf("min" to FieldError.number(min), "max" to FieldError.number(max)))
                }
                number
            }
        }
        return CheckedAnswer(if (errors.isEmpty()) value else null, errors)
    }

    @Suppress("UNCHECKED_CAST", "CyclomaticComplexMethod")
    private fun holds(condition: Condition, effective: Map<String, Any>): Boolean {
        val source = effective[condition.sourceQuestionKey]
        val value = condition.value
        return when (condition.operator) {
            ConditionOperator.IS_ANSWERED -> source != null
            ConditionOperator.IS_NOT_ANSWERED -> source == null
            ConditionOperator.EQUALS -> source != null && source == value?.optionKey
            ConditionOperator.NOT_EQUALS -> source != null && source != value?.optionKey
            ConditionOperator.IN -> source != null && source in value?.optionKeys.orEmpty()
            ConditionOperator.INCLUDES_ANY -> source != null && value?.optionKeys.orEmpty().any { it in (source as Set<String>) }
            ConditionOperator.INCLUDES_ALL -> source != null && value?.optionKeys.orEmpty().all { it in (source as Set<String>) }
            ConditionOperator.EXCLUDES -> source != null && value?.optionKeys.orEmpty().none { it in (source as Set<String>) }
            ConditionOperator.EQ -> source != null && (source as Double) == value?.number
            ConditionOperator.NE -> source != null && (source as Double) != value?.number
            ConditionOperator.GT -> source != null && (source as Double) > value?.number!!
            ConditionOperator.GTE -> source != null && (source as Double) >= value?.number!!
            ConditionOperator.LT -> source != null && (source as Double) < value?.number!!
            ConditionOperator.LTE -> source != null && (source as Double) <= value?.number!!
            ConditionOperator.BETWEEN -> source != null && (source as Double) >= value?.min!! && source <= value.max!!
        }
    }
}
