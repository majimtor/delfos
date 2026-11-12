package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.definition.Option
import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.evaluation.Answer
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CsvExportTest {

    private val export = CsvExport(
        listOf(
            Question("colour", QuestionType.SINGLE_CHOICE, "Colour", options = listOf(Option("red", "Red"), Option("blue", "Blue"))),
            Question("toppings", QuestionType.MULTIPLE_CHOICE, "Toppings", options = listOf(Option("a", "A"), Option("b", "B"))),
            Question("age", QuestionType.NUMBER, "Age"),
            Question("mood", QuestionType.SCALE, "Mood", max = 5.0),
            Question("note", QuestionType.TEXT, "Note, please"),
        ),
    )

    @Test
    fun `the header names every column with its key and gives a multiple choice one column per option`() {
        assertThat(export.header()).isEqualTo(
            "Response id,Submitted at,Colour (colour),Toppings (toppings) / A (a),Toppings (toppings) / B (b),Age (age),Mood (mood),\"Note, please (note)\"\r\n",
        )
    }

    @Test
    fun `a row exports labels, flags and plain numbers, and leaves what was not answered empty`() {
        val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val row = export.row(
            id,
            Instant.parse("2026-11-12T10:00:00.123456Z"),
            listOf(Answer("colour", optionKey = "blue"), Answer("toppings", optionKeys = listOf("b")), Answer("age", number = 30.0), Answer("mood", number = 4.5)),
        )
        assertThat(row).isEqualTo("00000000-0000-0000-0000-000000000001,2026-11-12T10:00:00Z,Blue,0,1,30,4.5,\r\n")
        assertThat(export.row(id, Instant.parse("2026-11-12T10:00:00Z"), emptyList())).isEqualTo("00000000-0000-0000-0000-000000000001,2026-11-12T10:00:00Z,,,,,,\r\n")
    }

    @Test
    fun `text is quoted when it has to be and never starts like a formula`() {
        assertThat(CsvExport.quote("plain text")).isEqualTo("plain text")
        assertThat(CsvExport.quote("a, \"b\"\nc")).isEqualTo("\"a, \"\"b\"\"\nc\"")
        for (text in listOf("=SUM(A1)", "+1", "-1", "@x", "\tx", "\rx")) {
            assertThat(CsvExport.safe(text)).describedAs(text).isEqualTo("'$text")
        }
        assertThat(CsvExport.safe("x=1")).isEqualTo("x=1")
        assertThat(CsvExport.safe("")).isEqualTo("")
        assertThat(export.row(UUID.randomUUID(), Instant.EPOCH, listOf(Answer("note", text = "=1+1")))).endsWith(",'=1+1\r\n")
    }

    @Test
    fun `numbers come out without exponent or trailing zeros`() {
        assertThat(CsvExport.plain(30.0)).isEqualTo("30")
        assertThat(CsvExport.plain(2.50)).isEqualTo("2.5")
        assertThat(CsvExport.plain(0.0000001)).isEqualTo("0.0000001")
        assertThat(CsvExport.plain(12345678.0)).isEqualTo("12345678")
    }
}
