package com.manuelalejandro.delfos.mobile.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelalejandro.delfos.mobile.api.ApiResult
import com.manuelalejandro.delfos.mobile.api.ManagerApi
import com.manuelalejandro.delfos.mobile.api.VersionResults
import com.manuelalejandro.delfos.mobile.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface ResultsUiState {
    data object Loading : ResultsUiState

    data object Failed : ResultsUiState

    data class Loaded(val results: VersionResults, val refreshing: Boolean = false) : ResultsUiState
}

/**
 * The aggregates of one published version, refreshed every [pollIntervalMs] while the screen
 * lives; null disables the polling (the tests). The web client listens to server events; a phone
 * on the move is better served by a slow poll that costs nothing while the screen is away.
 */
internal class ResultsViewModel(
    private val api: ManagerApi,
    private val session: SessionManager,
    private val surveyId: String,
    private val versionNumber: Int,
    pollIntervalMs: Long? = DEFAULT_POLL_MS,
) : ViewModel() {
    private val state0 = MutableStateFlow<ResultsUiState>(ResultsUiState.Loading)
    val state: StateFlow<ResultsUiState> = state0.asStateFlow()

    init {
        load()
        if (pollIntervalMs != null) {
            viewModelScope.launch {
                while (true) {
                    delay(pollIntervalMs)
                    refresh()
                }
            }
        }
    }

    fun load() {
        state0.value = ResultsUiState.Loading
        viewModelScope.launch { fetch() }
    }

    fun refresh() {
        val current = state0.value
        if (current is ResultsUiState.Loaded) state0.value = current.copy(refreshing = true)
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        val previous = state0.value as? ResultsUiState.Loaded
        state0.value = when (val result = api.getVersionResults(surveyId, versionNumber)) {
            is ApiResult.Success -> ResultsUiState.Loaded(result.value)
            is ApiResult.Failure -> {
                if (result.status == UNAUTHORIZED) session.signOut()
                previous?.copy(refreshing = false) ?: ResultsUiState.Failed
            }
            is ApiResult.Unreachable -> previous?.copy(refreshing = false) ?: ResultsUiState.Failed
        }
    }

    private companion object {
        const val UNAUTHORIZED = 401
        const val DEFAULT_POLL_MS = 60_000L
    }
}
