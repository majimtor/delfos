package com.manuelalejandro.delfos.api

import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.evaluation.FieldError
import com.manuelalejandro.delfos.evaluation.FieldErrorCode
import com.manuelalejandro.delfos.limits.PayloadTooLargeException
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
import tools.jackson.core.JacksonException
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.databind.exc.InvalidNullException
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.exc.PropertyBindingException
import tools.jackson.databind.exc.UnrecognizedPropertyException
import tools.jackson.databind.exc.ValueInstantiationException

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
        if (ex is HttpMessageNotReadableException && generateSequence<Throwable>(ex) { it.cause }.any { it is PayloadTooLargeException }) {
            val problem = Problems.of(HttpStatus.CONTENT_TOO_LARGE, ProblemCode.PAYLOAD_TOO_LARGE, "The request body is too large.", instanceOf(request))
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem)
        }
        val errors = fieldErrorsOf(ex)
        val (code, detail) = when {
            ex is NoResourceFoundException -> ProblemCode.NOT_FOUND to "No resource at this path."
            ex is HttpMessageNotReadableException && errors.isEmpty() -> ProblemCode.MALFORMED_REQUEST to "The request body could not be read."
            ex is HttpMessageNotReadableException || ex is MethodArgumentNotValidException ||
                ex is HandlerMethodValidationException || ex is MissingServletRequestParameterException ->
                ProblemCode.VALIDATION_ERROR to "${errors.size} fields are invalid."
            else -> Problems.codeFor(statusCode) to (HttpStatus.resolve(statusCode.value())?.reasonPhrase ?: "Request rejected.")
        }
        val problem = Problems.of(statusCode, code, detail, instanceOf(request), errors.takeIf { it.isNotEmpty() })
        return ResponseEntity.status(statusCode).headers(headers).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem)
    }

    @ExceptionHandler(ApiProblemException::class)
    fun handleProblem(ex: ApiProblemException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val problem = Problems.of(ex.status, ex.code, ex.detail, instanceOf(request), ex.errors)
        return ResponseEntity.status(ex.status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: WebRequest): ResponseEntity<ProblemDetail> {
        log.error("Unhandled exception", ex)
        val problem = Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, ProblemCode.INTERNAL_ERROR, "An unexpected error occurred.", instanceOf(request))
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem)
    }

    private fun instanceOf(request: WebRequest): String? = (request as? ServletWebRequest)?.request?.requestURI

    private fun fieldErrorsOf(ex: Exception): List<FieldError> = when (ex) {
        is HttpMessageNotReadableException -> listOfNotNull(bindingErrorOf(ex))
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

    /**
     * A body that parses as JSON but does not bind to the DTO is a field error with the JSON path,
     * not a malformed request: a missing non-null field, an unknown field or a value of the wrong
     * kind. A body that is not JSON yields no field error and stays malformed.
     */
    private fun bindingErrorOf(ex: HttpMessageNotReadableException): FieldError? {
        val cause = generateSequence<Throwable>(ex) { it.cause }.filterIsInstance<JacksonException>().firstOrNull() ?: return null
        val code = when (cause) {
            is InvalidNullException -> FieldErrorCode.REQUIRED
            is UnrecognizedPropertyException -> FieldErrorCode.UNKNOWN_FIELD
            is InvalidFormatException -> if (cause.targetType?.isEnum == true) FieldErrorCode.INVALID_VALUE else FieldErrorCode.INVALID_FORMAT
            // A generated enum rejects an unknown value from its creator.
            is ValueInstantiationException, is MismatchedInputException -> FieldErrorCode.INVALID_VALUE
            else -> return null
        }
        return FieldError(path = jsonPathOf(cause), code = code)
    }

    private fun jsonPathOf(ex: JacksonException): String {
        val segments = ex.path.map { ref -> ref.propertyName ?: ref.index.takeIf { it >= 0 }?.let { "[$it]" } ?: "" }
        val property = (ex as? PropertyBindingException)?.propertyName ?: (ex as? InvalidNullException)?.propertyName?.simpleName
        val all = if (property != null && segments.lastOrNull() != property) segments + property else segments
        return all.filter { it.isNotEmpty() }.joinToString("") { if (it.startsWith("[")) it else ".$it" }.trimStart('.')
    }

    /** Maps a Bean Validation constraint name to the stable field error code of the contract. */
    private fun codeOf(constraint: String?): FieldErrorCode = when (constraint?.substringBefore('.')) {
        "NotNull", "NotBlank", "NotEmpty" -> FieldErrorCode.REQUIRED
        "Size", "Max", "DecimalMax" -> FieldErrorCode.TOO_LONG
        "Min", "DecimalMin", "Positive", "PositiveOrZero" -> FieldErrorCode.OUT_OF_RANGE
        "Pattern", "Email" -> FieldErrorCode.INVALID_FORMAT
        else -> FieldErrorCode.INVALID_VALUE
    }
}
