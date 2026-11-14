package com.manuelalejandro.delfos.mobile.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelalejandro.delfos.mobile.api.ApiResult
import com.manuelalejandro.delfos.mobile.api.ManagerApi
import com.manuelalejandro.delfos.mobile.api.Survey
import com.manuelalejandro.delfos.mobile.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class SurveysUiState(
    val surveys: List<Survey> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
    val nextCursor: String? = null,
    val loadingMore: Boolean = false,
)

/** The manager's surveys, most recently updated first, a page at a time. */
internal class SurveysViewModel(
    private val api: ManagerApi,
    private val session: SessionManager,
) : ViewModel() {
    private val state0 = MutableStateFlow(SurveysUiState())
    val state: StateFlow<SurveysUiState> = state0.asStateFlow()

    init {
        load()
    }

    fun load() {
        state0.value = SurveysUiState()
        viewModelScope.launch {
            state0.value = when (val result = api.listSurveys(cursor = null)) {
                is ApiResult.Success -> SurveysUiState(surveys = result.value.items, loading = false, nextCursor = result.value.nextCursor)
                is ApiResult.Failure -> {
                    if (result.status == UNAUTHORIZED) session.signOut()
                    SurveysUiState(loading = false, failed = true)
                }
                is ApiResult.Unreachable -> SurveysUiState(loading = false, failed = true)
            }
        }
    }

    fun loadMore() {
        val current = state0.value
        val cursor = current.nextCursor ?: return
        if (current.loadingMore) return
        state0.value = current.copy(loadingMore = true)
        viewModelScope.launch {
            state0.value = when (val result = api.listSurveys(cursor)) {
                is ApiResult.Success -> state0.value.copy(
                    surveys = state0.value.surveys + result.value.items,
                    nextCursor = result.value.nextCursor,
                    loadingMore = false,
                )
                is ApiResult.Failure -> {
                    if (result.status == UNAUTHORIZED) session.signOut()
                    state0.value.copy(loadingMore = false, failed = true)
                }
                is ApiResult.Unreachable -> state0.value.copy(loadingMore = false, failed = true)
            }
        }
    }

    /** Revokes the session on the server, then forgets it here whatever the server said. */
    fun logout() {
        viewModelScope.launch {
            api.deleteCurrentSession()
            session.signOut()
        }
    }

    private companion object {
        const val UNAUTHORIZED = 401
    }
}
