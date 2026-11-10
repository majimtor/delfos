package com.manuelalejandro.delfos.api

import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.evaluation.FieldError
import org.springframework.http.HttpStatus

/** A failure the domain decided; the handler renders it as Problem Details with its code. */
class ApiProblemException(
    val status: HttpStatus,
    val code: ProblemCode,
    val detail: String,
    val errors: List<FieldError>? = null,
) : RuntimeException(detail)
