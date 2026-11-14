package com.manuelalejandro.delfos.mobile.api

import com.manuelalejandro.delfos.mobile.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json

/** Outcome of one call: the body, a Problem of the contract, or no answer from the server at all. */
internal sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    /** A non-2xx status; [problem] is null when the body was not a Problem (a proxy page, for example). */
    data class Failure(val status: Int, val problem: Problem?) : ApiResult<Nothing>

    data class Unreachable(val cause: Throwable) : ApiResult<Nothing>
}

/** The public operations the answer flow needs; a seam for the tests. */
internal interface PublicApi {
    suspend fun getPublicSurvey(publicId: String, invitation: String?): ApiResult<PublicSurvey>

    suspend fun submitResponse(publicId: String, invitation: String?, request: SubmitResponseRequest): ApiResult<SubmittedResponse>
}

/** The operations behind a session; the bearer token comes from the session manager. */
internal interface ManagerApi {
    suspend fun createSession(request: CreateSessionRequest): ApiResult<Session>

    suspend fun deleteCurrentSession(): ApiResult<Unit>

    suspend fun listSurveys(cursor: String?): ApiResult<SurveyPage>

    suspend fun getVersionResults(surveyId: String, versionNumber: Int): ApiResult<VersionResults>
}

/** The API of Delfos over HTTP, at one base URL fixed when the application is built. */
internal class DelfosApi(
    engine: HttpClientEngine,
    private val baseUrl: String = Config.API_BASE_URL,
    /** The bearer token of the current session, or null when nobody is signed in. */
    private val token: () -> String? = { null },
) : PublicApi, ManagerApi {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val client = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            // The free instance of the demo sleeps when idle and takes up to two minutes to answer again.
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
    }

    override suspend fun getPublicSurvey(publicId: String, invitation: String?): ApiResult<PublicSurvey> = call {
        client.get("$baseUrl/public/surveys/${publicId.encodeURLPathPart()}") {
            if (invitation != null) parameter("invitation", invitation)
        }
    }

    override suspend fun submitResponse(publicId: String, invitation: String?, request: SubmitResponseRequest): ApiResult<SubmittedResponse> = call {
        client.post("$baseUrl/public/surveys/${publicId.encodeURLPathPart()}/responses") {
            if (invitation != null) parameter("invitation", invitation)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun createSession(request: CreateSessionRequest): ApiResult<Session> = call {
        client.post("$baseUrl/auth/sessions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun deleteCurrentSession(): ApiResult<Unit> = callForStatus {
        client.delete("$baseUrl/auth/sessions/current") { bearer() }
    }

    override suspend fun listSurveys(cursor: String?): ApiResult<SurveyPage> = call {
        client.get("$baseUrl/surveys") {
            bearer()
            parameter("limit", PAGE_SIZE)
            if (cursor != null) parameter("cursor", cursor)
        }
    }

    override suspend fun getVersionResults(surveyId: String, versionNumber: Int): ApiResult<VersionResults> = call {
        client.get("$baseUrl/surveys/${surveyId.encodeURLPathPart()}/versions/$versionNumber/results") { bearer() }
    }

    private fun HttpRequestBuilder.bearer() {
        token()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

    private suspend inline fun <reified T> call(request: () -> HttpResponse): ApiResult<T> {
        val response = exchange(request) ?: return ApiResult.Unreachable(IllegalStateException("no response"))
        return when (response) {
            is Exchange.Done -> try {
                ApiResult.Success(response.response.body<T>())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ApiResult.Unreachable(e)
            }
            is Exchange.Refused -> response.failure
            is Exchange.Lost -> response.unreachable
        }
    }

    private suspend inline fun callForStatus(request: () -> HttpResponse): ApiResult<Unit> =
        when (val response = exchange(request)) {
            is Exchange.Done -> ApiResult.Success(Unit)
            is Exchange.Refused -> response.failure
            is Exchange.Lost -> response.unreachable
            null -> ApiResult.Unreachable(IllegalStateException("no response"))
        }

    private sealed interface Exchange {
        class Done(val response: HttpResponse) : Exchange

        class Refused(val failure: ApiResult.Failure) : Exchange

        class Lost(val unreachable: ApiResult.Unreachable) : Exchange
    }

    /** Sends the request and sorts the outcome; a Problem body arrives as `application/problem+json`, decoded here. */
    private suspend inline fun exchange(request: () -> HttpResponse): Exchange? {
        val response = try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Exchange.Lost(ApiResult.Unreachable(e))
        }
        if (response.status.isSuccess()) return Exchange.Done(response)
        val problem = try {
            json.decodeFromString<Problem>(response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        return Exchange.Refused(ApiResult.Failure(response.status.value, problem))
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 20_000L
        const val REQUEST_TIMEOUT_MS = 150_000L
        const val PAGE_SIZE = 50
    }
}
