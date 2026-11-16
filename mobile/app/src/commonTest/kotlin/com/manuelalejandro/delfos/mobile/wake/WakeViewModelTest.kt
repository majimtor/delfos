package com.manuelalejandro.delfos.mobile.wake

import com.manuelalejandro.delfos.mobile.api.HealthApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class WakeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Answers false for the first [failures] probes and true afterwards. */
    private class FakeHealthApi(private val failures: Int) : HealthApi {
        var probes = 0

        override suspend fun probeHealth(): Boolean = ++probes > failures
    }

    private fun TestScope.viewModel(api: HealthApi) =
        WakeViewModel(api, intervalMs = 3_000, budgetMs = 10_000, now = { testScheduler.currentTime })

    @Test
    fun anApiThatAnswersAtOnceIsReadyWithoutAnyExplanation() = runTest(dispatcher) {
        val api = FakeHealthApi(failures = 0)
        val viewModel = viewModel(api)
        assertEquals(WakeUiState.Waking(attempts = 0, elapsedSeconds = 0), viewModel.state.value)
        advanceUntilIdle()
        assertEquals(WakeUiState.Ready, viewModel.state.value)
        assertEquals(1, api.probes)
    }

    @Test
    fun anApiThatWakesUpIsProbedEveryThreeSecondsAndTheWaitIsCounted() = runTest(dispatcher) {
        val api = FakeHealthApi(failures = 2)
        val viewModel = viewModel(api)
        advanceTimeBy(1)
        assertEquals(WakeUiState.Waking(attempts = 1, elapsedSeconds = 0), viewModel.state.value)
        advanceTimeBy(3_000)
        assertEquals(WakeUiState.Waking(attempts = 2, elapsedSeconds = 3), viewModel.state.value)
        advanceUntilIdle()
        assertEquals(WakeUiState.Ready, viewModel.state.value)
        assertEquals(3, api.probes)
    }

    @Test
    fun anApiThatNeverAnswersWithinTheBudgetFails() = runTest(dispatcher) {
        val api = FakeHealthApi(failures = Int.MAX_VALUE)
        val viewModel = viewModel(api)
        advanceUntilIdle()
        assertEquals(WakeUiState.Failed, viewModel.state.value)
        // Probes at 0, 3, 6 and 9 seconds keep waiting; the one at 12 seconds exceeds the ten-second budget.
        assertEquals(5, api.probes)
    }

    @Test
    fun aRetryProbesAgain() = runTest(dispatcher) {
        val api = FakeHealthApi(failures = 5)
        val viewModel = viewModel(api)
        advanceUntilIdle()
        assertEquals(WakeUiState.Failed, viewModel.state.value)
        viewModel.retry()
        assertEquals(WakeUiState.Waking(attempts = 0, elapsedSeconds = 0), viewModel.state.value)
        advanceUntilIdle()
        assertEquals(WakeUiState.Ready, viewModel.state.value)
        assertEquals(6, api.probes)
    }
}
