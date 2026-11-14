package com.manuelalejandro.delfos.mobile.api

import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
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

/** Mirrors `CreateSessionRequest`; the token travels as a bearer afterwards. */
@Serializable
internal data class CreateSessionRequest(
    val username: String,
    val password: String,
    val transport: String = "BEARER",
)

/** Mirrors `Session`; `token` is present once, at creation, with the bearer transport. */
@Serializable
internal data class Session(
    val accountId: String,
    val username: String,
    val isAdmin: Boolean,
    val expiresAt: String,
    val token: String? = null,
)

/** Mirrors `Survey` in the contract. */
@Serializable
internal data class Survey(
    val id: String,
    val publicId: String,
    val title: String,
    val description: String? = null,
    val language: String,
    val status: String,
    val respondentMode: String,
    val closesAt: String? = null,
    val maxResponses: Int? = null,
    val responseCount: Int,
    val publishedVersionNumber: Int? = null,
    val hasDraft: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

/** Mirrors `SurveyPage` in the contract. */
@Serializable
internal data class SurveyPage(
    val items: List<Survey>,
    val nextCursor: String? = null,
)

/** Mirrors `VersionResults` in the contract. */
@Serializable
internal data class VersionResults(
    val surveyId: String,
    val versionNumber: Int,
    val responseCount: Int,
    val computedAt: String,
    val questions: List<QuestionResult>,
)

/** Mirrors `QuestionResult`: `options` for choice types, `number` for numbers and scales, `distribution` for scales. */
@Serializable
internal data class QuestionResult(
    val key: String,
    val text: String,
    val type: QuestionType,
    val answerCount: Int,
    val options: List<OptionCount>? = null,
    val number: NumberStats? = null,
    val distribution: List<ValueCount>? = null,
)

@Serializable
internal data class OptionCount(val key: String, val label: String, val count: Int)

@Serializable
internal data class NumberStats(val count: Int, val mean: Double, val min: Double, val max: Double)

@Serializable
internal data class ValueCount(val value: Int, val count: Int)
