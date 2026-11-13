package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.evaluation.Answer
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * The tabular layout shared by the exports, as the contract describes: one row per response after
 * the response id and the submission time, one column per question in the order of the version
 * and, for a multiple choice, one column per option. Each format renders the typed cells its way.
 */
class ExportLayout(questions: List<Question>) {

    /** A cell: text from a person, a number, a yes-or-no flag, or nothing. */
    sealed interface Cell {
        data class Text(val value: String) : Cell
        data class Number(val value: Double) : Cell
        data class Flag(val value: Boolean) : Cell
        data object Empty : Cell
    }

    /** A column: its header and the cell it takes from a response's answers, by question key. */
    class Column(val header: String, val cell: (Map<String, Answer>) -> Cell)

    val columns: List<Column> = listOf(
        Column("Response id") { answers -> Cell.Text(answers.getValue(ID).text.orEmpty()) },
        Column("Submitted at") { answers -> Cell.Text(answers.getValue(SUBMITTED_AT).text.orEmpty()) },
    ) + questions.flatMap { question ->
        val header = "${question.text} (${question.key})"
        when (question.type) {
            QuestionType.SINGLE_CHOICE -> listOf(
                Column(header) { answers ->
                    answers[question.key]?.optionKey?.let { key -> Cell.Text(question.options?.firstOrNull { it.key == key }?.label ?: key) } ?: Cell.Empty
                },
            )
            QuestionType.MULTIPLE_CHOICE -> question.options.orEmpty().map { option ->
                Column("${question.text} (${question.key}) / ${option.label} (${option.key})") { answers ->
                    answers[question.key]?.let { Cell.Flag(option.key in it.optionKeys.orEmpty()) } ?: Cell.Empty
                }
            }
            QuestionType.NUMBER, QuestionType.SCALE -> listOf(Column(header) { answers -> answers[question.key]?.number?.let { Cell.Number(it) } ?: Cell.Empty })
            QuestionType.TEXT -> listOf(Column(header) { answers -> answers[question.key]?.text?.let { Cell.Text(it) } ?: Cell.Empty })
        }
    }

    /** The cells of one response, in column order. */
    fun cells(id: UUID, submittedAt: Instant, answers: List<Answer>): List<Cell> {
        val byKey = answers.associateBy { it.questionKey } +
            mapOf(ID to Answer(ID, text = id.toString()), SUBMITTED_AT to Answer(SUBMITTED_AT, text = submittedAt.truncatedTo(ChronoUnit.SECONDS).toString()))
        return columns.map { it.cell(byKey) }
    }

    private companion object {
        // Pseudo keys for the two leading columns; a question key never starts with a space.
        const val ID = " id"
        const val SUBMITTED_AT = " submittedAt"
    }
}
