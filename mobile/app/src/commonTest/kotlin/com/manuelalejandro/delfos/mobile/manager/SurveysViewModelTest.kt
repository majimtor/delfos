package com.manuelalejandro.delfos.mobile.manager

import com.manuelalejandro.delfos.mobile.api.ApiResult
import com.manuelalejandro.delfos.mobile.api.Problem
import com.manuelalejandro.delfos.mobile.api.SurveyPage
import com.manuelalejandro.delfos.mobile.session.SessionManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SurveysViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsThePagesInOrder() = runTest(dispatcher) {
        val api = FakeManagerApi(
            pages = listOf(
                ApiResult.Success(SurveyPage(listOf(survey("s1", "Commuting")), nextCursor = "c2")),
                ApiResult.Success(SurveyPage(listOf(survey("s2", "Workshop", version = null, status = "DRAFT", responses = 0)))),
            ),
        )
        val viewModel = SurveysViewModel(api, SessionManager(FakeTokenStore()))
        advanceUntilIdle()
        assertEquals(listOf("s1"), viewModel.state.value.surveys.map { it.id })
        assertEquals("c2", viewModel.state.value.nextCursor)
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("s1", "s2"), viewModel.state.value.surveys.map { it.id })
        assertNull(viewModel.state.value.nextCursor)
        assertEquals(listOf(null, "c2"), api.cursors)
    }

    @Test
    fun aSessionTheServerNoLongerKnowsIsDropped() = runTest(dispatcher) {
        val api = FakeManagerApi(pages = listOf(ApiResult.Failure(401, Problem(code = "UNAUTHENTICATED"))))
        val session = SessionManager(FakeTokenStore("""{"token":"old","username":"demo-manager","isAdmin":false}"""))
        val viewModel = SurveysViewModel(api, session)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.failed)
        assertFalse(session.signedIn)
    }

    @Test
    fun loggingOutRevokesTheSessionAndForgetsIt() = runTest(dispatcher) {
        val api = FakeManagerApi()
        val session = SessionManager(FakeTokenStore("""{"token":"old","username":"demo-manager","isAdmin":false}"""))
        val viewModel = SurveysViewModel(api, session)
        advanceUntilIdle()
        viewModel.logout()
        advanceUntilIdle()
        assertEquals(1, api.deleted)
        assertFalse(session.signedIn)
    }
}
