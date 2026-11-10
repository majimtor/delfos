package com.manuelalejandro.delfos.evaluation

import com.manuelalejandro.delfos.definition.ConditionOperator
import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.definition.SurveyDefinition
import kotlinx.serialization.json.JsonPrimitive

/** When a definition is validated: saving a draft, or publishing it, which adds the checks of publication. */
public enum class ValidationMode { SAVE, PUBLISH }

/**
 * Validates a definition and reports every error at once, with the path of each one, so an editor can
 * show them in place. The semantics are fixed by the conformance suite in `contract/conformance`.
 */
public object DefinitionValidator {

    private const val MIN_OPTIONS = 2
    private const val MAX_OPTIONS = 50
    private const val SCALE_MIN_LOWER = 0.0
    private const val SCALE_MIN_UPPER = 1.0
    private const val SCALE_MAX_LOWER = 3.0
    private const val SCALE_MAX_UPPER = 10.0

    private val commonFields = setOf("key", "type", "text", "required")
    private val fieldsByType: Map<QuestionType, Set<String>> = mapOf(
        QuestionType.SINGLE_CHOICE to setOf("options"),
        QuestionType.MULTIPLE_CHOICE to setOf("options", "minSelections", "maxSelections"),
        QuestionType.TEXT to setOf("maxLength", "multiline"),
        QuestionType.NUMBER to setOf("min", "max", "integerOnly"),
        QuestionType.SCALE to setOf("min", "max", "minLabel", "maxLabel"),
    )

    public fun validate(definition: SurveyDefinition, mode: ValidationMode = ValidationMode.SAVE): List<FieldError> {
        val errors = mutableListOf<FieldError>()
        if (mode == ValidationMode.PUBLISH && definition.questions.isEmpty()) {
            errors += FieldError("questions", FieldErrorCode.NO_QUESTIONS)
        }
        val seenKeys = mutableSetOf<String>()
        definition.questions.forEachIndexed { index, question ->
            val path = "questions[$index]"
            if (!seenKeys.add(question.key)) errors += FieldError("$path.key", FieldErrorCode.DUPLICATE)
            validateQuestion(question, path, errors)
        }
        val byKey = definition.questions.withIndex().associateBy({ it.value.key }, { it })
        val targets = mutableSetOf<String>()
        definition.rules.forEachIndexed { ruleIndex, rule ->
            val rulePath = "rules[$ruleIndex]"
            val target = byKey[rule.targetQuestionKey]
            when {
                target == null -> errors += FieldError("$rulePath.targetQuestionKey", FieldErrorCode.UNKNOWN_QUESTION)
                !targets.add(rule.targetQuestionKey) -> errors += FieldError("$rulePath.targetQuestionKey", FieldErrorCode.DUPLICATE_RULE)
            }
            if (rule.conditions.isEmpty()) errors += FieldError("$rulePath.conditions", FieldErrorCode.EMPTY_CONDITIONS)
            rule.conditions.forEachIndexed { conditionIndex, condition ->
                val conditionPath = "$rulePath.conditions[$conditionIndex]"
                val source = byKey[condition.sourceQuestionKey]
                val operator = condition.operator
                if (source == null) {
                    errors += FieldError("$conditionPath.sourceQuestionKey", FieldErrorCode.UNKNOWN_QUESTION)
                } else {
                    if (target != null) {
                        if (source.index == target.index) {
                            errors += FieldError("$conditionPath.sourceQuestionKey", FieldErrorCode.SELF_REFERENCE)
                        } else if (source.index > target.index) {
                            errors += FieldError(
                                "$conditionPath.sourceQuestionKey",
                                FieldErrorCode.FORWARD_REFERENCE,
                                mapOf(
                                    "sourcePosition" to JsonPrimitive(source.index + 1),
                                    "targetPosition" to JsonPrimitive(target.index + 1),
                                ),
                            )
                        }
                    }
                    if (source.value.type !in operator.sourceTypes) {
                        errors += FieldError(
                            "$conditionPath.operator",
                            FieldErrorCode.OPERATOR_TYPE_MISMATCH,
                            mapOf("sourceType" to JsonPrimitive(source.value.type.name)),
                        )
                    }
                }
                val value = condition.value
                val present = buildSet {
                    if (value?.optionKey != null) add("optionKey")
                    if (value?.optionKeys != null) add("optionKeys")
                    if (value?.number != null) add("number")
                    if (value?.min != null) add("min")
                    if (value?.max != null) add("max")
                }
                val shape = operator.valueShape
                when {
                    shape.isEmpty() -> if (value != null) errors += FieldError("$conditionPath.value", FieldErrorCode.VALUE_TYPE_MISMATCH)
                    present != shape -> errors += FieldError("$conditionPath.value", FieldErrorCode.VALUE_TYPE_MISMATCH)
                    operator == ConditionOperator.BETWEEN && value!!.min!! > value.max!! ->
                        errors += FieldError("$conditionPath.value", FieldErrorCode.VALUE_TYPE_MISMATCH)
                    source != null && source.value.type in operator.sourceTypes && source.value.options != null -> {
                        val optionKeys = source.value.options.orEmpty().map { it.key }.toSet()
                        if ("optionKey" in shape && value!!.optionKey !in optionKeys) {
                            errors += FieldError("$conditionPath.value.optionKey", FieldErrorCode.UNKNOWN_OPTION)
                        }
                        if ("optionKeys" in shape) {
                            value!!.optionKeys!!.forEachIndexed { keyIndex, key ->
                                if (key !in optionKeys) {
                                    errors += FieldError("$conditionPath.value.optionKeys[$keyIndex]", FieldErrorCode.UNKNOWN_OPTION)
                                }
                            }
                        }
                    }
                }
            }
        }
        return errors
    }

    private fun validateQuestion(question: Question, path: String, errors: MutableList<FieldError>) {
        val allowed = commonFields + fieldsByType.getValue(question.type)
        val present = buildList {
            if (question.options != null) add("options")
            if (question.minSelections != null) add("minSelections")
            if (question.maxSelections != null) add("maxSelections")
            if (question.maxLength != null) add("maxLength")
            if (question.multiline != null) add("multiline")
            if (question.min != null) add("min")
            if (question.max != null) add("max")
            if (question.integerOnly != null) add("integerOnly")
            if (question.minLabel != null) add("minLabel")
            if (question.maxLabel != null) add("maxLabel")
        }
        present.filter { it !in allowed }.forEach { errors += FieldError("$path.$it", FieldErrorCode.INVALID_VALUE) }

        val options = question.options
        when (question.type) {
            QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE -> {
                if (options == null) {
                    errors += FieldError("$path.options", FieldErrorCode.REQUIRED)
                } else {
                    if (options.size < MIN_OPTIONS) {
                        errors += FieldError("$path.options", FieldErrorCode.TOO_FEW_ITEMS, mapOf("min" to JsonPrimitive(MIN_OPTIONS)))
                    }
                    if (options.size > MAX_OPTIONS) {
                        errors += FieldError("$path.options", FieldErrorCode.TOO_MANY_ITEMS, mapOf("max" to JsonPrimitive(MAX_OPTIONS)))
                    }
                    val seen = mutableSetOf<String>()
                    options.forEachIndexed { index, option ->
                        if (!seen.add(option.key)) errors += FieldError("$path.options[$index].key", FieldErrorCode.DUPLICATE)
                    }
                    if (question.type == QuestionType.MULTIPLE_CHOICE) {
                        val min = question.effectiveMinSelections
                        val max = question.effectiveMaxSelections
                        if (question.minSelections != null && min > max) {
                            errors += FieldError("$path.minSelections", FieldErrorCode.INVALID_VALUE)
                        } else if (question.maxSelections != null && max < min) {
                            errors += FieldError("$path.maxSelections", FieldErrorCode.INVALID_VALUE)
                        }
                        if (question.maxSelections != null && max > options.size) {
                            errors += FieldError("$path.maxSelections", FieldErrorCode.INVALID_VALUE)
                        }
                    }
                }
            }
            QuestionType.NUMBER -> {
                if (question.min != null && question.max != null && question.min > question.max) {
                    errors += FieldError("$path.max", FieldErrorCode.INVALID_VALUE)
                }
            }
            QuestionType.SCALE -> {
                val max = question.max
                if (max == null) {
                    errors += FieldError("$path.max", FieldErrorCode.REQUIRED)
                } else if (!max.isWholeNumber() || max < SCALE_MAX_LOWER || max > SCALE_MAX_UPPER) {
                    errors += FieldError(
                        "$path.max",
                        FieldErrorCode.OUT_OF_RANGE,
                        mapOf("min" to FieldError.number(SCALE_MAX_LOWER), "max" to FieldError.number(SCALE_MAX_UPPER)),
                    )
                }
                val min = question.effectiveScaleMin
                if (!min.isWholeNumber() || min < SCALE_MIN_LOWER || min > SCALE_MIN_UPPER) {
                    errors += FieldError(
                        "$path.min",
                        FieldErrorCode.OUT_OF_RANGE,
                        mapOf("min" to FieldError.number(SCALE_MIN_LOWER), "max" to FieldError.number(SCALE_MIN_UPPER)),
                    )
                }
            }
            QuestionType.TEXT -> Unit
        }
    }
}
