package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.evaluation.Answer
import com.manuelalejandro.delfos.results.ExportLayout.Cell
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Renders the shared export layout as CSV for spreadsheets. Every text that came from a person,
 * headers included, gets a leading apostrophe when a spreadsheet would run it as a formula. Comma
 * separated, RFC 4180 quoting, CRLF line ends; the byte-order mark is for the caller to prepend once.
 */
class CsvExport(questions: List<Question>) {

    private val layout = ExportLayout(questions)

    fun header(): String = line(layout.columns.map { safe(it.header) })

    fun row(id: UUID, submittedAt: Instant, answers: List<Answer>): String = line(layout.cells(id, submittedAt, answers).map { render(it) })

    private fun render(cell: Cell): String = when (cell) {
        is Cell.Text -> safe(cell.value)
        is Cell.Number -> plain(cell.value)
        is Cell.Flag -> if (cell.value) "1" else "0"
        Cell.Empty -> ""
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
