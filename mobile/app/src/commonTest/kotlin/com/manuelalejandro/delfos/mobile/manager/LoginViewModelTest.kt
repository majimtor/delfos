package com.manuelalejandro.delfos.mobile.manager

import com.manuelalejandro.delfos.mobile.api.ApiResult
import com.manuelalejandro.delfos.mobile.api.Problem
import com.manuelalejandro.delfos.mobile.session.SessionManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
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
    fun emptyFieldsAreNotSent() = runTest(dispatcher) {
        val api = FakeManagerApi()
        val viewModel = LoginViewModel(api, SessionManager(FakeTokenStore()))
        viewModel.setUsername("demo-manager")
        viewModel.submit()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.missing)
        assertTrue(api.sessionRequests.isEmpty())
    }

    @Test
    fun aSessionIsCreatedAsBearerAndKept() = runTest(dispatcher) {
        val api = FakeManagerApi()
        val store = FakeTokenStore()
        val session = SessionManager(store)
        val viewModel = LoginViewModel(api, session)
        viewModel.setUsername(" demo-manager ")
        viewModel.setPassword("delfos-demo")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals("BEARER", api.sessionRequests.single().transport)
        assertEquals("demo-manager", api.sessionRequests.single().username)
        assertTrue(viewModel.state.value.done)
        assertEquals("", viewModel.state.value.password)
        assertEquals("token-123", session.token)
    }

    @Test
    fun refusalsAreToldWithTheirCode() = runTest(dispatcher) {
        val wrong = LoginViewModel(FakeManagerApi(session = ApiResult.Failure(401, Problem(code = "INVALID_CREDENTIALS"))), SessionManager(FakeTokenStore()))
        val disabled = LoginViewModel(FakeManagerApi(session = ApiResult.Failure(401, Problem(code = "ACCOUNT_DISABLED"))), SessionManager(FakeTokenStore()))
        val limited = LoginViewModel(FakeManagerApi(session = ApiResult.Failure(429, Problem(code = "RATE_LIMITED"))), SessionManager(FakeTokenStore()))
        val away = LoginViewModel(FakeManagerApi(session = ApiResult.Unreachable(IllegalStateException("offline"))), SessionManager(FakeTokenStore()))
        for (viewModel in listOf(wrong, disabled, limited, away)) {
            viewModel.setUsername("demo-manager")
            viewModel.setPassword("x")
            viewModel.submit()
        }
        advanceUntilIdle()
        assertEquals(LoginError.INVALID_CREDENTIALS, wrong.state.value.error)
        assertEquals(LoginError.ACCOUNT_DISABLED, disabled.state.value.error)
        assertEquals(LoginError.RATE_LIMITED, limited.state.value.error)
        assertEquals(LoginError.GENERIC, away.state.value.error)
        assertFalse(wrong.state.value.done)
    }
}
