package com.manuelalejandro.delfos.mobile.wake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelalejandro.delfos.mobile.api.HealthApi
import kotlin.time.TimeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the wake screen shows: the API is being probed, answers, or did not answer within the budget. */
internal sealed interface WakeUiState {
    /**
     * Probing. [attempts] counts the probes that failed so far, so the explanation appears only once the API
     * has not answered at once; [elapsedSeconds] is the time since the first probe.
     */
    data class Waking(val attempts: Int, val elapsedSeconds: Int) : WakeUiState

    data object Ready : WakeUiState

    data object Failed : WakeUiState
}

/**
 * Probes the API until it answers, like the wait screen of the web: the free instance of the demo sleeps when
 * idle and takes up to two minutes to answer again, which the screen explains instead of letting the first
 * request fail. One probe every [intervalMs] until [budgetMs] have passed; then the user decides to retry.
 */
internal class WakeViewModel(
    private val api: HealthApi,
    private val intervalMs: Long = INTERVAL_MS,
    private val budgetMs: Long = BUDGET_MS,
    /** Milliseconds of a monotonic clock; the tests pass the virtual time of their scheduler. */
    private val now: () -> Long = monotonicMillis(),
) : ViewModel() {
    private val state0 = MutableStateFlow<WakeUiState>(WakeUiState.Waking(attempts = 0, elapsedSeconds = 0))
    val state: StateFlow<WakeUiState> = state0
    private var probing: Job? = null

    init {
        probe()
    }

    fun retry() {
        probe()
    }

    private fun probe() {
        probing?.cancel()
        state0.value = WakeUiState.Waking(attempts = 0, elapsedSeconds = 0)
        probing = viewModelScope.launch {
            val startedAt = now()
            var attempts = 0
            while (true) {
                if (api.probeHealth()) {
                    state0.value = WakeUiState.Ready
                    return@launch
                }
                attempts++
                val waited = now() - startedAt
                if (waited >= budgetMs) {
                    state0.value = WakeUiState.Failed
                    return@launch
                }
                state0.value = WakeUiState.Waking(attempts, (waited / 1000).toInt())
                delay(intervalMs)
            }
        }
    }

    private companion object {
        const val INTERVAL_MS = 3_000L
        const val BUDGET_MS = 120_000L

        fun monotonicMillis(): () -> Long {
            val start = TimeSource.Monotonic.markNow()
            return { start.elapsedNow().inWholeMilliseconds }
        }
    }
}
