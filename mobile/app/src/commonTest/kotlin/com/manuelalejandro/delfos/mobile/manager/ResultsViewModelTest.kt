package com.manuelalejandro.delfos.mobile.manager

import com.manuelalejandro.delfos.mobile.api.ApiResult
import com.manuelalejandro.delfos.mobile.api.Problem
import com.manuelalejandro.delfos.mobile.session.SessionManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(api: FakeManagerApi, session: SessionManager = SessionManager(FakeTokenStore())) =
        ResultsViewModel(api, session, "s1", 1, pollIntervalMs = null)

    @Test
    fun loadsTheAggregates() = runTest(dispatcher) {
        val viewModel = viewModel(FakeManagerApi())
        assertEquals(ResultsUiState.Loading, viewModel.state.value)
        advanceUntilIdle()
        val loaded = assertIs<ResultsUiState.Loaded>(viewModel.state.value)
        assertEquals(20, loaded.results.responseCount)
        assertEquals(2, loaded.results.questions.size)
    }

    @Test
    fun aRefreshThatFailsKeepsWhatWasShown() = runTest(dispatcher) {
        val api = FakeManagerApi()
        val viewModel = viewModel(api)
        advanceUntilIdle()
        api.results = ApiResult.Unreachable(IllegalStateException("offline"))
        viewModel.refresh()
        advanceUntilIdle()
        val loaded = assertIs<ResultsUiState.Loaded>(viewModel.state.value)
        assertEquals(RESULTS, loaded.results)
        assertFalse(loaded.refreshing)
        assertEquals(2, api.resultsCalls)
    }

    @Test
    fun aFailureWithNothingShownIsTold() = runTest(dispatcher) {
        val viewModel = viewModel(FakeManagerApi(results = ApiResult.Failure(404, Problem(code = "NOT_FOUND"))))
        advanceUntilIdle()
        assertEquals(ResultsUiState.Failed, viewModel.state.value)
    }

    @Test
    fun aSessionTheServerNoLongerKnowsIsDropped() = runTest(dispatcher) {
        val session = SessionManager(FakeTokenStore("""{"token":"old","username":"demo-manager","isAdmin":false}"""))
        viewModel(FakeManagerApi(results = ApiResult.Failure(401, Problem(code = "UNAUTHENTICATED"))), session)
        advanceUntilIdle()
        assertFalse(session.signedIn)
    }
}
