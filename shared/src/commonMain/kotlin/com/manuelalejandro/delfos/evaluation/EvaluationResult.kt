package com.manuelalejandro.delfos.evaluation

import kotlinx.serialization.Serializable

/** Outcome of evaluating partial answers against a definition. Mirrors `EvaluationResult` in the contract. */
@Serializable
public data class EvaluationResult(
    /** Keys of the visible questions, in survey order. */
    val visibleQuestionKeys: List<String>,
    /** Problems with the answers given; an answer with an error is ignored by the rules. */
    val errors: List<FieldError>,
    /** Visible required questions without a valid answer, in survey order. */
    val missingRequiredQuestionKeys: List<String>,
)
