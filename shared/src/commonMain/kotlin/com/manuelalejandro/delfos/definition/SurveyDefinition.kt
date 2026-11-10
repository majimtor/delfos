package com.manuelalejandro.delfos.definition

import kotlinx.serialization.Serializable

/** Questions and rules of one survey version, in survey order. Mirrors `DraftDefinition` in the contract. */
@Serializable
public data class SurveyDefinition(
    val questions: List<Question>,
    val rules: List<VisibilityRule> = emptyList(),
)

@Serializable
public enum class QuestionType { SINGLE_CHOICE, MULTIPLE_CHOICE, TEXT, NUMBER, SCALE }

@Serializable
public data class Option(
    val key: String,
    val label: String,
)

/**
 * A question with its per-type configuration. A configuration field is `null` when absent;
 * which fields apply to each type is defined by the contract and checked by [com.manuelalejandro.delfos.evaluation.DefinitionValidator].
 */
@Serializable
public data class Question(
    val key: String,
    val type: QuestionType,
    val text: String,
    val required: Boolean = false,
    val options: List<Option>? = null,
    val minSelections: Int? = null,
    val maxSelections: Int? = null,
    val maxLength: Int? = null,
    val multiline: Boolean? = null,
    val min: Double? = null,
    val max: Double? = null,
    val integerOnly: Boolean? = null,
    val minLabel: String? = null,
    val maxLabel: String? = null,
) {
    /** Default of `maxLength` for `TEXT` questions, as stated in the contract. */
    public val effectiveMaxLength: Int get() = maxLength ?: DEFAULT_MAX_LENGTH

    /** Default of `minSelections` for `MULTIPLE_CHOICE` questions. */
    public val effectiveMinSelections: Int get() = minSelections ?: 1

    /** Default of `maxSelections` for `MULTIPLE_CHOICE` questions: every option. */
    public val effectiveMaxSelections: Int get() = maxSelections ?: (options?.size ?: 0)

    /** Default of `min` for `SCALE` questions. */
    public val effectiveScaleMin: Double get() = min ?: 1.0

    private companion object {
        const val DEFAULT_MAX_LENGTH = 500
    }
}
