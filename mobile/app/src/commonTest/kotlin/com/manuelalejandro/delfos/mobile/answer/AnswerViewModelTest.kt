package com.manuelalejandro.delfos.mobile.answer

import com.manuelalejandro.delfos.evaluation.Answer
import com.manuelalejandro.delfos.evaluation.FieldError
import com.manuelalejandro.delfos.evaluation.FieldErrorCode
import com.manuelalejandro.delfos.mobile.api.ApiResult
import com.manuelalejandro.delfos.mobile.api.Problem
import com.manuelalejandro.delfos.mobile.api.PublicApi
import com.manuelalejandro.delfos.mobile.api.PublicSurvey
import com.manuelalejandro.delfos.mobile.api.SubmitResponseRequest
import com.manuelalejandro.delfos.mobile.api.SubmittedResponse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
class AnswerViewModelTest {
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
    fun loadsEvaluatesAndSubmitsTheVisibleAnswers() = runTest(dispatcher) {
        val api = FakeApi()
        val viewModel = AnswerViewModel(api, "demo-survey1", null)
        assertEquals(AnswerUiState.Loading, viewModel.state.value)
        advanceUntilIdle()
        assertIs<AnswerUiState.Form>(viewModel.state.value)

        viewModel.change("q1", AnswerValue(optionKey = "no"))
        viewModel.change("q2", AnswerValue(text = "hidden"))
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AnswerUiState.Done, viewModel.state.value)
        assertEquals(SubmitResponseRequest(1, listOf(Answer("q1", optionKey = "no"))), api.submitted)
        assertEquals(null, api.invitationSeen)
    }

    @Test
    fun aMissingRequiredAnswerStopsTheSubmissionLocally() = runTest(dispatcher) {
        val api = FakeApi()
        val viewModel = AnswerViewModel(api, "demo-survey1", "demo-survey1-3")
        advanceUntilIdle()
        viewModel.submit()
        advanceUntilIdle()
        val form = assertIs<AnswerUiState.Form>(viewModel.state.value)
        assertEquals(setOf("q1"), form.missing)
        assertNull(api.submitted)
    }

    @Test
    fun errorsReportedByTheServerLandOnTheirQuestions() = runTest(dispatcher) {
        val problem = Problem(
            code = "VALIDATION_ERROR",
            errors = listOf(FieldError("answers[0].optionKey", FieldErrorCode.INVALID_VALUE)),
        )
        val api = FakeApi(submit = ApiResult.Failure(400, problem))
        val viewModel = AnswerViewModel(api, "demo-survey1", null)
        advanceUntilIdle()
        viewModel.change("q1", AnswerValue(optionKey = "yes"))
        viewModel.submit()
        advanceUntilIdle()
        val form = assertIs<AnswerUiState.Form>(viewModel.state.value)
        assertEquals(FieldErrorCode.INVALID_VALUE, form.errorsOf("q1").single().code)
        assertTrue(!form.submitting)
    }

    @Test
    fun aUsedInvitationAtSubmissionEndsTheFlow() = runTest(dispatcher) {
        val api = FakeApi(submit = ApiResult.Failure(409, Problem(code = "INVITATION_USED")))
        val viewModel = AnswerViewModel(api, "demo-survey1", "demo-survey1-2")
        advanceUntilIdle()
        viewModel.change("q1", AnswerValue(optionKey = "yes"))
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(AnswerUiState.Refused(Refusal.INVITATION_USED), viewModel.state.value)
        assertEquals("demo-survey1-2", api.invitationSeen)
    }

    @Test
    fun aClosedSurveyAtSubmissionDisablesTheForm() = runTest(dispatcher) {
        val api = FakeApi(submit = ApiResult.Failure(409, Problem(code = "SURVEY_NOT_ACCEPTING_RESPONSES")))
        val viewModel = AnswerViewModel(api, "demo-survey1", null)
        advanceUntilIdle()
        viewModel.change("q1", AnswerValue(optionKey = "yes"))
        viewModel.submit()
        advanceUntilIdle()
        assertTrue(assertIs<AnswerUiState.Form>(viewModel.state.value).closed)
    }

    @Test
    fun notFoundAndRefusalsAtLoadAreTold() = runTest(dispatcher) {
        val missing = AnswerViewModel(FakeApi(survey = ApiResult.Failure(404, null)), "nosuchsurvey", null)
        val refused = AnswerViewModel(FakeApi(survey = ApiResult.Failure(403, Problem(code = "INVITATION_REQUIRED"))), "demo-survey1", null)
        val down = AnswerViewModel(FakeApi(survey = ApiResult.Unreachable(IllegalStateException("offline"))), "demo-survey1", null)
        advanceUntilIdle()
        assertEquals(AnswerUiState.NotFound, missing.state.value)
        assertEquals(AnswerUiState.Refused(Refusal.INVITATION_REQUIRED), refused.state.value)
        assertEquals(AnswerUiState.LoadFailed, down.state.value)
    }

    private class FakeApi(
        private val survey: ApiResult<PublicSurvey> = ApiResult.Success(SURVEY),
        private val submit: ApiResult<SubmittedResponse> = ApiResult.Success(SubmittedResponse("r1", "2026-11-13T10:05:00Z")),
    ) : PublicApi {
        var submitted: SubmitResponseRequest? = null
        var invitationSeen: String? = null

        override suspend fun getPublicSurvey(publicId: String, invitation: String?): ApiResult<PublicSurvey> {
            invitationSeen = invitation
            return survey
        }

        override suspend fun submitResponse(publicId: String, invitation: String?, request: SubmitResponseRequest): ApiResult<SubmittedResponse> {
            invitationSeen = invitation
            submitted = request
            return submit
        }
    }
}
