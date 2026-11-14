package com.manuelalejandro.delfos.mobile.answer

import androidx.compose.runtime.Composable
import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.evaluation.FieldErrorCode
import com.manuelalejandro.delfos.mobile.resources.Res
import com.manuelalejandro.delfos.mobile.resources.error_hidden_answered
import com.manuelalejandro.delfos.mobile.resources.error_invalid_value
import com.manuelalejandro.delfos.mobile.resources.error_missing_required
import com.manuelalejandro.delfos.mobile.resources.error_not_integer
import com.manuelalejandro.delfos.mobile.resources.error_out_of_range
import com.manuelalejandro.delfos.mobile.resources.error_out_of_range_max
import com.manuelalejandro.delfos.mobile.resources.error_out_of_range_min
import com.manuelalejandro.delfos.mobile.resources.error_too_few_selections
import com.manuelalejandro.delfos.mobile.resources.error_too_long
import com.manuelalejandro.delfos.mobile.resources.error_too_many_selections
import com.manuelalejandro.delfos.mobile.resources.hint_integer
import com.manuelalejandro.delfos.mobile.resources.hint_max
import com.manuelalejandro.delfos.mobile.resources.hint_max_length
import com.manuelalejandro.delfos.mobile.resources.hint_min
import com.manuelalejandro.delfos.mobile.resources.hint_range
import com.manuelalejandro.delfos.mobile.resources.hint_selections
import kotlin.math.abs
import kotlin.math.floor
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.compose.resources.stringResource

/** The stable code of the contract, translated with its parameters; same wording as the web client. */
@Composable
internal fun errorMessage(error: QuestionError): String {
    val min = error.params["min"]?.display()
    val max = error.params["max"]?.display()
    return when (error.code) {
        FieldErrorCode.MISSING_REQUIRED_ANSWER, FieldErrorCode.REQUIRED -> stringResource(Res.string.error_missing_required)
        FieldErrorCode.TOO_LONG -> stringResource(Res.string.error_too_long, max ?: "0")
        FieldErrorCode.OUT_OF_RANGE -> when {
            min != null && max != null -> stringResource(Res.string.error_out_of_range, min, max)
            min != null -> stringResource(Res.string.error_out_of_range_min, min)
            else -> stringResource(Res.string.error_out_of_range_max, max ?: "0")
        }
        FieldErrorCode.NOT_INTEGER -> stringResource(Res.string.error_not_integer)
        FieldErrorCode.TOO_FEW_SELECTIONS -> stringResource(Res.string.error_too_few_selections, min ?: "1")
        FieldErrorCode.TOO_MANY_SELECTIONS -> stringResource(Res.string.error_too_many_selections, max ?: "1")
        FieldErrorCode.HIDDEN_QUESTION_ANSWERED -> stringResource(Res.string.error_hidden_answered)
        else -> stringResource(Res.string.error_invalid_value)
    }
}

/** What the respondent may enter, stated under the question as the web client does. */
@Composable
internal fun hintFor(question: Question): String? = when (question.type) {
    QuestionType.MULTIPLE_CHOICE -> stringResource(
        Res.string.hint_selections,
        question.effectiveMinSelections.toString(),
        question.effectiveMaxSelections.toString(),
    )
    QuestionType.TEXT -> stringResource(Res.string.hint_max_length, question.effectiveMaxLength.toString())
    QuestionType.NUMBER -> {
        val min = question.min
        val max = question.max
        val parts = buildList {
            when {
                min != null && max != null -> add(stringResource(Res.string.hint_range, min.display(), max.display()))
                min != null -> add(stringResource(Res.string.hint_min, min.display()))
                max != null -> add(stringResource(Res.string.hint_max, max.display()))
            }
            if (question.integerOnly == true) add(stringResource(Res.string.hint_integer))
        }
        parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }
    QuestionType.SINGLE_CHOICE, QuestionType.SCALE -> null
}

/** A number as people read it: no decimal part when it has none. */
internal fun Double.display(): String =
    if (isFinite() && this == floor(this) && abs(this) < 1e15) toLong().toString() else toString()

private fun JsonPrimitive.display(): String = content.toDoubleOrNull()?.display() ?: content
