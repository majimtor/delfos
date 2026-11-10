package com.manuelalejandro.delfos.definition

import kotlinx.serialization.Serializable

/**
 * Decides whether [targetQuestionKey] is shown. Conditions may only reference questions that
 * precede the target; a question has at most one rule and is visible without one.
 */
@Serializable
public data class VisibilityRule(
    val targetQuestionKey: String,
    val match: MatchMode,
    val conditions: List<Condition>,
)

@Serializable
public enum class MatchMode { ALL, ANY }

@Serializable
public data class Condition(
    val sourceQuestionKey: String,
    val operator: ConditionOperator,
    val value: ConditionValue? = null,
)

/** The operand of a condition; which field applies depends on the operator (see [ConditionOperator.valueShape]). */
@Serializable
public data class ConditionValue(
    val optionKey: String? = null,
    val optionKeys: List<String>? = null,
    val number: Double? = null,
    val min: Double? = null,
    val max: Double? = null,
)

@Serializable
public enum class ConditionOperator(
    /** Source question types this operator applies to. */
    public val sourceTypes: Set<QuestionType>,
    /** Fields of [ConditionValue] that must be present, and no other. */
    public val valueShape: Set<String>,
) {
    EQUALS(setOf(QuestionType.SINGLE_CHOICE), setOf("optionKey")),
    NOT_EQUALS(setOf(QuestionType.SINGLE_CHOICE), setOf("optionKey")),
    IN(setOf(QuestionType.SINGLE_CHOICE), setOf("optionKeys")),
    INCLUDES_ANY(setOf(QuestionType.MULTIPLE_CHOICE), setOf("optionKeys")),
    INCLUDES_ALL(setOf(QuestionType.MULTIPLE_CHOICE), setOf("optionKeys")),
    EXCLUDES(setOf(QuestionType.MULTIPLE_CHOICE), setOf("optionKeys")),
    EQ(NUMERIC_TYPES, setOf("number")),
    NE(NUMERIC_TYPES, setOf("number")),
    GT(NUMERIC_TYPES, setOf("number")),
    GTE(NUMERIC_TYPES, setOf("number")),
    LT(NUMERIC_TYPES, setOf("number")),
    LTE(NUMERIC_TYPES, setOf("number")),
    BETWEEN(NUMERIC_TYPES, setOf("min", "max")),
    IS_ANSWERED(ALL_TYPES, emptySet()),
    IS_NOT_ANSWERED(ALL_TYPES, emptySet()),
}

// Enum entries cannot read their companion object during initialisation; file-level values can be read.
private val NUMERIC_TYPES: Set<QuestionType> = setOf(QuestionType.NUMBER, QuestionType.SCALE)
private val ALL_TYPES: Set<QuestionType> = QuestionType.entries.toSet()
