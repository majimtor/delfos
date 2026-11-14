package com.manuelalejandro.delfos.mobile.answer

import com.manuelalejandro.delfos.evaluation.Answer
import com.manuelalejandro.delfos.evaluation.FieldError
import com.manuelalejandro.delfos.evaluation.FieldErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive

class AnswerFormTest {
    private val form = AnswerForm(DEFINITION)

    @Test
    fun aQuestionHiddenByARuleIsNeitherShownNorSentNorAnError() {
        val values = mapOf("q1" to AnswerValue(optionKey = "no"), "q2" to AnswerValue(text = "Fiat"))
        val evaluation = form.evaluate(values)
        assertEquals(listOf("q1", "q3", "q4", "q5"), evaluation.visible.map { it.key })
        assertEquals(listOf(Answer("q1", optionKey = "no")), evaluation.toSend)
        assertTrue(evaluation.errors.isEmpty())
    }

    @Test
    fun theAnswerThatSatisfiesTheRuleShowsTheDependentQuestion() {
        val evaluation = form.evaluate(mapOf("q1" to AnswerValue(optionKey = "yes")))
        assertEquals(listOf("q1", "q2", "q3", "q4", "q5"), evaluation.visible.map { it.key })
        assertTrue(evaluation.missingRequired.isEmpty())
    }

    @Test
    fun unansweredRequiredQuestionsAreMissing() {
        assertEquals(setOf("q1"), form.evaluate(emptyMap()).missingRequired)
    }

    @Test
    fun numbersAreCheckedWithTheCodesOfTheContract() {
        assertEquals(FieldErrorCode.OUT_OF_RANGE, codeOf("q3", AnswerValue(numberText = "12")))
        assertEquals(FieldErrorCode.NOT_INTEGER, codeOf("q3", AnswerValue(numberText = "2,5")))
        assertEquals(FieldErrorCode.INVALID_VALUE, codeOf("q3", AnswerValue(numberText = "abc")))
        assertNull(form.evaluate(mapOf("q3" to AnswerValue(numberText = " 7 "))).errors["q3"])
    }

    @Test
    fun aDecimalCommaIsReadAsANumber() {
        assertEquals(2.5, AnswerValue(numberText = "2,5").number)
        assertEquals(listOf(Answer("q5", number = 3.0)), form.evaluate(mapOf("q5" to AnswerValue(numberText = "3"))).toSend)
    }

    @Test
    fun tooManySelectionsAreReported() {
        assertEquals(FieldErrorCode.TOO_MANY_SELECTIONS, codeOf("q4", AnswerValue(optionKeys = listOf("a", "b", "c"))))
    }

    @Test
    fun serverErrorsAreAttributedThroughTheAnswersSent() {
        val sent = listOf(Answer("q1", optionKey = "yes"), Answer("q2", text = "A very long car name"))
        val errors = listOf(
            FieldError("answers[1].text", FieldErrorCode.TOO_LONG, mapOf("max" to JsonPrimitive(10))),
            FieldError("answers", FieldErrorCode.MISSING_REQUIRED_ANSWER, mapOf("questionKey" to JsonPrimitive("q5"))),
            FieldError("answers[7].text", FieldErrorCode.INVALID_VALUE, null),
        )
        val attributed = AnswerForm.attributeErrors(errors, sent)
        assertEquals(listOf("q2", "q5"), attributed.map { it.questionKey })
        assertEquals("10", attributed[0].params["max"]?.content)
    }

    private fun codeOf(key: String, value: AnswerValue): FieldErrorCode? =
        form.evaluate(mapOf(key to value)).errors[key]?.single()?.code
}
