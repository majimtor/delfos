package com.manuelalejandro.delfos.api

import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.evaluation.FieldError
import com.manuelalejandro.delfos.evaluation.FieldErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError as SpringFieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Turns every failure into the single error shape of the contract. The built-in exceptions of
 * Spring MVC all pass through [handleExceptionInternal], where their default body is replaced by
 * a problem with the stable `code`; nothing internal leaks into a response.
 */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = fieldErrorsOf(ex)
        val (code, detail) = when (ex) {
            is NoResourceFoundException -> ProblemCode.NOT_FOUND to "No resource at this path."
            is HttpMessageNotReadableException -> ProblemCode.MALFORMED_REQUEST to "The request body could not be read."
            is MethodArgumentNotValidException, is HandlerMethodValidationException, is MissingServletRequestParameterException ->
                ProblemCode.VALIDATION_ERROR to "${errors.size} fields are invalid."
            else -> Problems.codeFor(statusCode) to (HttpStatus.resolve(statusCode.value())?.reasonPhrase ?: "Request rejected.")
        }
        val problem = Problems.of(statusCode, code, detail, instanceOf(request), errors.takeIf { it.isNotEmpty() })
        return ResponseEntity.status(statusCode).headers(headers).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: WebRequest): ResponseEntity<ProblemDetail> {
        log.error("Unhandled exception", ex)
        val problem = Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, ProblemCode.INTERNAL_ERROR, "An unexpected error occurred.", instanceOf(request))
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem)
    }

    private fun instanceOf(request: WebRequest): String? = (request as? ServletWebRequest)?.request?.requestURI

    private fun fieldErrorsOf(ex: Exception): List<FieldError> = when (ex) {
        is MethodArgumentNotValidException -> ex.bindingResult.fieldErrors.map { it.toFieldError() }
        is HandlerMethodValidationException -> ex.parameterValidationResults.flatMap { result ->
            result.resolvableErrors.map { error ->
                FieldError(
                    path = result.methodParameter.parameterName ?: "parameter",
                    code = codeOf((error as? SpringFieldError)?.code ?: error.codes?.firstOrNull()),
                )
            }
        }
        is MissingServletRequestParameterException -> listOf(FieldError(ex.parameterName, FieldErrorCode.REQUIRED))
        else -> emptyList()
    }

    private fun SpringFieldError.toFieldError(): FieldError = FieldError(path = field, code = codeOf(code))

    /** Maps a Bean Validation constraint name to the stable field error code of the contract. */
    private fun codeOf(constraint: String?): FieldErrorCode = when (constraint?.substringBefore('.')) {
        "NotNull", "NotBlank", "NotEmpty" -> FieldErrorCode.REQUIRED
        "Size", "Max", "DecimalMax" -> FieldErrorCode.TOO_LONG
        "Min", "DecimalMin", "Positive", "PositiveOrZero" -> FieldErrorCode.OUT_OF_RANGE
        "Pattern", "Email" -> FieldErrorCode.INVALID_FORMAT
        else -> FieldErrorCode.INVALID_VALUE
    }
}
