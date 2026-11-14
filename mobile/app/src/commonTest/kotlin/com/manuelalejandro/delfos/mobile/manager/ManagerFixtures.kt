package com.manuelalejandro.delfos.mobile.manager

import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.mobile.api.ApiResult
import com.manuelalejandro.delfos.mobile.api.CreateSessionRequest
import com.manuelalejandro.delfos.mobile.api.ManagerApi
import com.manuelalejandro.delfos.mobile.api.NumberStats
import com.manuelalejandro.delfos.mobile.api.OptionCount
import com.manuelalejandro.delfos.mobile.api.QuestionResult
import com.manuelalejandro.delfos.mobile.api.Session
import com.manuelalejandro.delfos.mobile.api.Survey
import com.manuelalejandro.delfos.mobile.api.SurveyPage
import com.manuelalejandro.delfos.mobile.api.VersionResults
import com.manuelalejandro.delfos.mobile.session.TokenStore

internal val SESSION = Session(
    accountId = "0f0d4a0e-1111-4c1c-9a9a-000000000001",
    username = "demo-manager",
    isAdmin = false,
    expiresAt = "2026-11-14T12:00:00Z",
    token = "token-123",
)

internal fun survey(id: String, title: String, version: Int? = 1, status: String = "PUBLISHED", responses: Int = 20) = Survey(
    id = id,
    publicId = "demo-$id",
    title = title,
    language = "en",
    status = status,
    respondentMode = "ANONYMOUS",
    responseCount = responses,
    publishedVersionNumber = version,
    hasDraft = version == null,
    createdAt = "2026-11-13T10:00:00Z",
    updatedAt = "2026-11-13T11:00:00Z",
)

internal val RESULTS = VersionResults(
    surveyId = "s1",
    versionNumber = 1,
    responseCount = 20,
    computedAt = "2026-11-13T12:00:00Z",
    questions = listOf(
        QuestionResult("transport", "How do you travel?", QuestionType.SINGLE_CHOICE, 20, options = listOf(OptionCount("car", "By car", 12), OptionCount("bike", "By bicycle", 8))),
        QuestionResult("km", "Kilometres", QuestionType.NUMBER, 18, number = NumberStats(18, 12.5, 0.0, 60.0)),
    ),
)

internal class FakeTokenStore(initial: String? = null) : TokenStore {
    var value: String? = initial
    var cleared = 0

    override fun read(): String? = value

    override fun write(value: String) {
        this.value = value
    }

    override fun clear() {
        value = null
        cleared++
    }
}

internal class FakeManagerApi(
    var session: ApiResult<Session> = ApiResult.Success(SESSION),
    var pages: List<ApiResult<SurveyPage>> = listOf(ApiResult.Success(SurveyPage(listOf(survey("s1", "Commuting"))))),
    var results: ApiResult<VersionResults> = ApiResult.Success(RESULTS),
) : ManagerApi {
    val sessionRequests = mutableListOf<CreateSessionRequest>()
    val cursors = mutableListOf<String?>()
    var deleted = 0
    var resultsCalls = 0

    override suspend fun createSession(request: CreateSessionRequest): ApiResult<Session> {
        sessionRequests += request
        return session
    }

    override suspend fun deleteCurrentSession(): ApiResult<Unit> {
        deleted++
        return ApiResult.Success(Unit)
    }

    override suspend fun listSurveys(cursor: String?): ApiResult<SurveyPage> {
        cursors += cursor
        return pages[minOf(cursors.size - 1, pages.lastIndex)]
    }

    override suspend fun getVersionResults(surveyId: String, versionNumber: Int): ApiResult<VersionResults> {
        resultsCalls++
        return results
    }
}
