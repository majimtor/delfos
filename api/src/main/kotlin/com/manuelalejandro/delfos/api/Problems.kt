package com.manuelalejandro.delfos.api

import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.evaluation.FieldError
import java.net.URI
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail

/**
 * Builds Problem Details (RFC 9457) with the stable `code` the contract promises. `title` and
 * `detail` are developer text in English and are never shown to end users.
 */
object Problems {

    fun of(
        status: HttpStatusCode,
        code: ProblemCode,
        detail: String,
        instance: String? = null,
        errors: List<FieldError>? = null,
    ): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.type = URI.create("urn:delfos:problem:" + code.value.lowercase().replace('_', '-'))
        problem.title = HttpStatus.resolve(status.value())?.reasonPhrase ?: "Error"
        instance?.let { problem.instance = URI.create(it) }
        problem.setProperty("code", code.value)
        errors?.let { problem.setProperty("errors", it) }
        return problem
    }

    /** The code that corresponds to a status when the exception carries no more specific meaning. */
    fun codeFor(status: HttpStatusCode): ProblemCode = when (status.value()) {
        HttpStatus.NOT_FOUND.value() -> ProblemCode.NOT_FOUND
        HttpStatus.CONTENT_TOO_LARGE.value() -> ProblemCode.PAYLOAD_TOO_LARGE
        HttpStatus.UNAUTHORIZED.value() -> ProblemCode.UNAUTHENTICATED
        HttpStatus.TOO_MANY_REQUESTS.value() -> ProblemCode.RATE_LIMITED
        // The contract has no dedicated codes for 405, 406 or 415: the request is malformed for this API.
        in 400..499 -> ProblemCode.MALFORMED_REQUEST
        else -> ProblemCode.INTERNAL_ERROR
    }
}
