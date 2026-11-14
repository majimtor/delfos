package com.manuelalejandro.delfos.mobile.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelalejandro.delfos.mobile.api.ApiResult
import com.manuelalejandro.delfos.mobile.api.CreateSessionRequest
import com.manuelalejandro.delfos.mobile.api.ManagerApi
import com.manuelalejandro.delfos.mobile.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class LoginError { INVALID_CREDENTIALS, ACCOUNT_DISABLED, RATE_LIMITED, GENERIC }

internal data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    /** True after a submission with an empty field. */
    val missing: Boolean = false,
    val error: LoginError? = null,
    /** True once the session exists; the screen then leaves. */
    val done: Boolean = false,
)

/** Takes the credentials, creates a bearer session and hands it to the session manager. */
internal class LoginViewModel(
    private val api: ManagerApi,
    private val session: SessionManager,
) : ViewModel() {
    private val state0 = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = state0.asStateFlow()

    fun setUsername(value: String) {
        state0.value = state0.value.copy(username = value, missing = false, error = null)
    }

    fun setPassword(value: String) {
        state0.value = state0.value.copy(password = value, missing = false, error = null)
    }

    fun submit() {
        val current = state0.value
        if (current.submitting) return
        if (current.username.isBlank() || current.password.isBlank()) {
            state0.value = current.copy(missing = true)
            return
        }
        state0.value = current.copy(submitting = true, error = null)
        viewModelScope.launch {
            val result = api.createSession(CreateSessionRequest(current.username.trim(), current.password, transport = BEARER))
            state0.value = when (result) {
                is ApiResult.Success -> {
                    session.signIn(result.value)
                    state0.value.copy(submitting = false, password = "", done = true)
                }
                is ApiResult.Failure -> state0.value.copy(submitting = false, error = errorOf(result))
                is ApiResult.Unreachable -> state0.value.copy(submitting = false, error = LoginError.GENERIC)
            }
        }
    }

    private fun errorOf(failure: ApiResult.Failure): LoginError = when {
        failure.status == TOO_MANY_REQUESTS -> LoginError.RATE_LIMITED
        failure.problem?.code == "ACCOUNT_DISABLED" -> LoginError.ACCOUNT_DISABLED
        failure.problem?.code == "INVALID_CREDENTIALS" -> LoginError.INVALID_CREDENTIALS
        else -> LoginError.GENERIC
    }

    private companion object {
        const val BEARER = "BEARER"
        const val TOO_MANY_REQUESTS = 429
    }
}
