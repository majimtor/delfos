package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.evaluation.Answer
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Lays out the responses of a version as CSV for spreadsheets, as the contract describes: one row
 * per response, one column per question in the order of the version and, for a multiple choice,
 * one column per option with 1 or 0. Every text that came from a person, headers included, gets a
 * leading apostrophe when a spreadsheet would run it as a formula. Comma separated, RFC 4180
 * quoting, CRLF line ends; the byte-order mark is for the caller to prepend once.
 */
class CsvExport(questions: List<Question>) {

    /** A column: its header and the cell it takes from a response's answers, by question key. */
    private class Column(val header: String, val cell: (Map<String, Answer>) -> String)

    private val columns: List<Column> = questions.flatMap { question ->
        val header = safe("${question.text} (${question.key})")
        when (question.type) {
            QuestionType.SINGLE_CHOICE -> listOf(
                Column(header) { answers ->
                    answers[question.key]?.optionKey?.let { key -> safe(question.options?.firstOrNull { it.key == key }?.label ?: key) } ?: ""
                },
            )
            QuestionType.MULTIPLE_CHOICE -> question.options.orEmpty().map { option ->
                Column(safe("${question.text} (${question.key}) / ${option.label} (${option.key})")) { answers ->
                    answers[question.key]?.let { if (option.key in it.optionKeys.orEmpty()) "1" else "0" } ?: ""
                }
            }
            QuestionType.NUMBER, QuestionType.SCALE -> listOf(Column(header) { answers -> answers[question.key]?.number?.let(::plain) ?: "" })
            QuestionType.TEXT -> listOf(Column(header) { answers -> answers[question.key]?.text?.let(::safe) ?: "" })
        }
    }

    fun header(): String = line(listOf("Response id", "Submitted at") + columns.map { it.header })

    fun row(id: UUID, submittedAt: Instant, answers: List<Answer>): String {
        val byKey = answers.associateBy { it.questionKey }
        return line(listOf(id.toString(), submittedAt.truncatedTo(ChronoUnit.SECONDS).toString()) + columns.map { it.cell(byKey) })
    }

    companion object {
        const val BOM = "﻿"

        private val FORMULA_STARTERS = setOf('=', '+', '-', '@', '\t', '\r')

        /** Quoted when the field holds a comma, a quote or a line break; a quote inside is doubled. */
        fun quote(field: String): String =
            if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${field.replace("\"", "\"\"")}\"" else field

        /** A leading apostrophe makes a spreadsheet read the text as text instead of running it. */
        fun safe(text: String): String = if (text.isNotEmpty() && text[0] in FORMULA_STARTERS) "'$text" else text

        /** A number without exponent and without trailing zeros: 30.0 as `30`, 2.50 as `2.5`. */
        fun plain(number: Double): String = BigDecimal(number.toString()).stripTrailingZeros().toPlainString()

        private fun line(fields: List<String>): String = fields.joinToString(",", postfix = "\r\n") { quote(it) }
    }
}
