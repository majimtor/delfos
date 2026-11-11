package com.manuelalejandro.delfos.seed

import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.definition.SurveyDefinition
import com.manuelalejandro.delfos.evaluation.Answer
import com.manuelalejandro.delfos.evaluation.RulesEvaluator
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Deterministic synthetic responses: questions are answered in order, each one only when the
 * rules make it visible given the answers so far, so every generated response is valid.
 */
class ResponseGenerator(private val definition: SurveyDefinition, seed: Int) {

    private val random = Random(seed)
    private val evaluator = RulesEvaluator(definition)
    private val texts = listOf(
        "Sin comentarios.", "Todo correcto.", "Mejoraría la frecuencia.", "Muy satisfecho en general.",
        "Depende del día.", "Prefiero no detallar.", "Buena experiencia.", "Podría ser mejor.",
    )

    fun next(): List<Answer> {
        val answers = mutableListOf<Answer>()
        for (question in definition.questions) {
            val visible = question.key in evaluator.evaluate(answers).visibleQuestionKeys
            if (!visible) continue
            if (!question.required && random.nextDouble() < SKIP_PROBABILITY) continue
            answers += answerFor(question)
        }
        val check = evaluator.evaluate(answers)
        require(check.errors.isEmpty() && check.missingRequiredQuestionKeys.isEmpty()) { "generated response is invalid: $check" }
        return answers
    }

    private fun answerFor(question: Question): Answer = when (question.type) {
        QuestionType.SINGLE_CHOICE -> Answer(question.key, optionKey = question.options!!.random(random).key)
        QuestionType.MULTIPLE_CHOICE -> {
            val count = random.nextInt(question.effectiveMinSelections, question.effectiveMaxSelections + 1)
            Answer(question.key, optionKeys = question.options!!.shuffled(random).take(count).map { it.key })
        }
        QuestionType.TEXT -> Answer(question.key, text = texts.random(random))
        QuestionType.NUMBER -> {
            val min = question.min ?: 0.0
            val max = question.max ?: 100.0
            val value = min + random.nextDouble() * (max - min)
            Answer(question.key, number = if (question.integerOnly == true) value.roundToInt().toDouble() else (value * 10).roundToInt() / 10.0)
        }
        QuestionType.SCALE -> {
            val min = question.effectiveScaleMin.toInt()
            val max = question.max!!.toInt()
            // Skewed towards the upper half, as real satisfaction answers tend to be.
            val value = maxOf(random.nextInt(min, max + 1), random.nextInt(min, max + 1))
            Answer(question.key, number = value.toDouble())
        }
    }

    private companion object {
        const val SKIP_PROBABILITY = 0.25
    }
}
