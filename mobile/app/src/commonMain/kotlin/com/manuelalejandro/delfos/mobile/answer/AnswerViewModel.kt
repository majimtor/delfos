package com.manuelalejandro.delfos.mobile.answer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelalejandro.delfos.evaluation.FieldErrorCode
import com.manuelalejandro.delfos.mobile.api.ApiResult
import com.manuelalejandro.delfos.mobile.api.PublicApi
import com.manuelalejandro.delfos.mobile.api.PublicSurvey
import com.manuelalejandro.delfos.mobile.api.SubmitResponseRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Why the server refuses to show or take a survey in invited mode. */
internal enum class Refusal {
    INVITATION_REQUIRED,
    INVITATION_INVALID,
    INVITATION_USED,
    ;

    companion object {
        fun of(code: String?): Refusal? = entries.firstOrNull { it.name == code }
    }
}

internal sealed interface AnswerUiState {
    data object Loading : AnswerUiState

    data object NotFound : AnswerUiState

    data object LoadFailed : AnswerUiState

    data class Refused(val reason: Refusal) : AnswerUiState

    data class Form(
        val survey: PublicSurvey,
        val evaluation: FormEvaluation,
        val values: Map<String, AnswerValue> = emptyMap(),
        /** Errors the server reported at submission, until the next change. */
        val submitErrors: Map<String, List<QuestionError>> = emptyMap(),
        /** Required questions found unanswered at submission, until the next change. */
        val missing: Set<String> = emptySet(),
        val submitting: Boolean = false,
        val closed: Boolean = !survey.acceptingResponses,
        val sendFailed: Boolean = false,
    ) : AnswerUiState {
        fun errorsOf(questionKey: String): List<QuestionError> =
            (evaluation.errors[questionKey] ?: emptyList()) + (submitErrors[questionKey] ?: emptyList())
    }

    data object Done : AnswerUiState
}

/** Loads one survey, keeps the respondent's values evaluated locally, and submits the response. */
internal class AnswerViewModel(
    private val api: PublicApi,
    private val publicId: String,
    private val invitation: String?,
) : ViewModel() {
    private val state0 = MutableStateFlow<AnswerUiState>(AnswerUiState.Loading)
    val state: StateFlow<AnswerUiState> = state0.asStateFlow()
    private var form: AnswerForm? = null

    init {
        load()
    }

    fun load() {
        state0.value = AnswerUiState.Loading
        viewModelScope.launch {
            state0.value = when (val result = api.getPublicSurvey(publicId, invitation)) {
                is ApiResult.Success -> {
                    val loaded = AnswerForm(result.value.version.definition)
                    form = loaded
                    AnswerUiState.Form(survey = result.value, evaluation = loaded.evaluate(emptyMap()))
                }
                is ApiResult.Failure -> {
                    val refusal = Refusal.of(result.problem?.code)
                    when {
                        result.status == NOT_FOUND -> AnswerUiState.NotFound
                        refusal != null -> AnswerUiState.Refused(refusal)
                        else -> AnswerUiState.LoadFailed
                    }
                }
                is ApiResult.Unreachable -> AnswerUiState.LoadFailed
            }
        }
    }

    fun change(questionKey: String, value: AnswerValue) {
        val current = state0.value as? AnswerUiState.Form ?: return
        val loaded = form ?: return
        if (current.submitting) return
        val values = current.values + (questionKey to value)
        state0.value = current.copy(
            values = values,
            evaluation = loaded.evaluate(values),
            submitErrors = emptyMap(),
            missing = emptySet(),
            sendFailed = false,
        )
    }

    fun submit() {
        val current = state0.value as? AnswerUiState.Form ?: return
        if (current.submitting || current.closed) return
        val evaluation = current.evaluation
        // The same checks the server applies, so a response that would be refused is not sent.
        if (evaluation.errors.isNotEmpty() || evaluation.missingRequired.isNotEmpty()) {
            state0.value = current.copy(missing = evaluation.missingRequired)
            return
        }
        state0.value = current.copy(submitting = true, sendFailed = false)
        viewModelScope.launch {
            val request = SubmitResponseRequest(current.survey.version.number, evaluation.toSend)
            state0.value = when (val result = api.submitResponse(publicId, invitation, request)) {
                is ApiResult.Success -> AnswerUiState.Done
                is ApiResult.Failure -> refused(current, result)
                is ApiResult.Unreachable -> current.copy(submitting = false, sendFailed = true)
            }
        }
    }

    private fun refused(current: AnswerUiState.Form, result: ApiResult.Failure): AnswerUiState {
        val problem = result.problem
        val fieldErrors = problem?.errors
        if (result.status == BAD_REQUEST && fieldErrors != null) {
            val attributed = AnswerForm.attributeErrors(fieldErrors, current.evaluation.toSend)
            val (missing, others) = attributed.partition { it.code == FieldErrorCode.MISSING_REQUIRED_ANSWER }
            return current.copy(
                submitting = false,
                submitErrors = others.groupBy { it.questionKey },
                missing = missing.mapTo(HashSet()) { it.questionKey },
            )
        }
        if (problem?.code == Refusal.INVITATION_USED.name) return AnswerUiState.Refused(Refusal.INVITATION_USED)
        return if (result.status == CONFLICT) current.copy(submitting = false, closed = true) else current.copy(submitting = false, sendFailed = true)
    }

    private companion object {
        const val BAD_REQUEST = 400
        const val NOT_FOUND = 404
        const val CONFLICT = 409
    }
}
