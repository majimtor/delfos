package com.manuelalejandro.delfos.survey

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.evaluation.FieldError
import com.manuelalejandro.delfos.evaluation.FieldErrorCode
import java.util.Base64
import org.springframework.http.HttpStatus

/** Opaque pagination cursors: the keyset of the last item, encoded so clients do not depend on it. */
object Cursors {

    fun encode(vararg parts: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(parts.joinToString("|").toByteArray(Charsets.UTF_8))

    /** Decodes a cursor into its parts, or fails as a validation error on the `cursor` parameter. */
    fun decode(cursor: String, expectedParts: Int): List<String> {
        val parts = runCatching { String(Base64.getUrlDecoder().decode(cursor), Charsets.UTF_8).split('|') }.getOrNull()
        if (parts == null || parts.size != expectedParts) {
            throw ApiProblemException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, "1 fields are invalid.", listOf(FieldError("cursor", FieldErrorCode.INVALID_VALUE)))
        }
        return parts
    }
}
