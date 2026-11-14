package com.manuelalejandro.delfos.mobile.api

import com.manuelalejandro.delfos.mobile.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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

/** The API of Delfos over HTTP, at one base URL fixed when the application is built. */
internal class DelfosApi(
    engine: HttpClientEngine,
    private val baseUrl: String = Config.API_BASE_URL,
) : PublicApi {
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

    private suspend inline fun <reified T> call(request: () -> HttpResponse): ApiResult<T> {
        val response = try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ApiResult.Unreachable(e)
        }
        if (!response.status.isSuccess()) {
            // Problems arrive as `application/problem+json`, which the negotiation plugin does not decode.
            val problem = try {
                json.decodeFromString<Problem>(response.bodyAsText())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            return ApiResult.Failure(response.status.value, problem)
        }
        return try {
            ApiResult.Success(response.body<T>())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Unreachable(e)
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 20_000L
        const val REQUEST_TIMEOUT_MS = 150_000L
    }
}
