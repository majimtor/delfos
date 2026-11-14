package com.manuelalejandro.delfos.mobile.api

import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.SurveyDefinition
import com.manuelalejandro.delfos.definition.VisibilityRule
import com.manuelalejandro.delfos.evaluation.Answer
import com.manuelalejandro.delfos.evaluation.FieldError
import kotlinx.serialization.Serializable

/**
 * A published survey as the public operations return it. Mirrors `PublicSurvey` in the contract;
 * the enumerations stay strings so that a value added to the contract does not break the client.
 */
@Serializable
internal data class PublicSurvey(
    val publicId: String,
    val title: String,
    val description: String? = null,
    val language: String,
    val status: String,
    val acceptingResponses: Boolean,
    val respondentMode: String? = null,
    val closesAt: String? = null,
    val version: SurveyVersion,
)

/** Mirrors `SurveyVersion` in the contract; [definition] is what the shared evaluator takes. */
@Serializable
internal data class SurveyVersion(
    val number: Int,
    val status: String,
    val createdAt: String,
    val publishedAt: String? = null,
    val questions: List<Question>,
    val rules: List<VisibilityRule> = emptyList(),
) {
    val definition: SurveyDefinition get() = SurveyDefinition(questions, rules)
}

/** Mirrors `SubmitResponseRequest` in the contract. */
@Serializable
internal data class SubmitResponseRequest(
    val versionNumber: Int,
    val answers: List<Answer>,
)

/** Mirrors `SubmittedResponse` in the contract. */
@Serializable
internal data class SubmittedResponse(
    val responseId: String,
    val submittedAt: String,
)

/** Problem Details with the stable code. Mirrors `Problem` in the contract. */
@Serializable
internal data class Problem(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
    val code: String,
    val errors: List<FieldError>? = null,
)
