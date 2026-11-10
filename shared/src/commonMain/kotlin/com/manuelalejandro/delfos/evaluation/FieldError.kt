package com.manuelalejandro.delfos.evaluation

import kotlin.math.abs
import kotlin.math.floor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** A field-level error with the JSON path of the offending element. Mirrors `FieldError` in the contract. */
@Serializable
public data class FieldError(
    val path: String,
    val code: FieldErrorCode,
    val params: Map<String, JsonPrimitive>? = null,
) {
    public companion object {
        /** Builds the `params` entry of a numeric bound, as an integer when it has no fractional part. */
        public fun number(value: Double): JsonPrimitive =
            if (value.isFinite() && value == floor(value) && abs(value) < MAX_EXACT_LONG) {
                JsonPrimitive(value.toLong())
            } else {
                JsonPrimitive(value)
            }

        private const val MAX_EXACT_LONG = 9_007_199_254_740_992.0
    }
}

/** Stable codes of field-level errors, as enumerated by the contract. */
@Serializable
public enum class FieldErrorCode {
    REQUIRED,
    UNKNOWN_FIELD,
    INVALID_FORMAT,
    INVALID_VALUE,
    TOO_SHORT,
    TOO_LONG,
    OUT_OF_RANGE,
    NOT_INTEGER,
    TOO_FEW_ITEMS,
    TOO_MANY_ITEMS,
    DUPLICATE,
    NO_QUESTIONS,
    UNKNOWN_QUESTION,
    UNKNOWN_OPTION,
    SELF_REFERENCE,
    FORWARD_REFERENCE,
    OPERATOR_TYPE_MISMATCH,
    VALUE_TYPE_MISMATCH,
    DUPLICATE_RULE,
    EMPTY_CONDITIONS,
    MISSING_REQUIRED_ANSWER,
    HIDDEN_QUESTION_ANSWERED,
    ANSWER_TYPE_MISMATCH,
    DUPLICATE_OPTION,
    TOO_FEW_SELECTIONS,
    TOO_MANY_SELECTIONS,
}
