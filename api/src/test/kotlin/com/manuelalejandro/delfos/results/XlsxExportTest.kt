package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.definition.Option
import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.evaluation.Answer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XlsxExportTest {

    private val export = XlsxExport(
        listOf(
            Question("colour", QuestionType.SINGLE_CHOICE, "Colour", options = listOf(Option("red", "Red"), Option("blue", "Blue"))),
            Question("toppings", QuestionType.MULTIPLE_CHOICE, "Toppings", options = listOf(Option("a", "A"), Option("b", "B"))),
            Question("age", QuestionType.NUMBER, "Age"),
            Question("note", QuestionType.TEXT, "Note <& \"quoted\">"),
        ),
    )

    @Test
    fun `the workbook is a package of six well-formed parts with one sheet of typed cells`() {
        val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val rows = sequenceOf(
            XlsxExport.Row(id, Instant.parse("2026-11-12T10:00:00.5Z"), listOf(Answer("colour", optionKey = "blue"), Answer("toppings", optionKeys = listOf("b")), Answer("age", number = 30.0), Answer("note", text = "=SUM(A1) & <b>"))),
            XlsxExport.Row(UUID.randomUUID(), Instant.parse("2026-11-12T11:00:00Z"), emptyList()),
        )
        val out = ByteArrayOutputStream()
        export.write(out, rows)
        val parts = parts(out.toByteArray())
        assertThat(parts.keys).containsExactlyInAnyOrder("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml")
        parts.values.forEach { xml -> DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray())) }

        val sheet = parts.getValue("xl/worksheets/sheet1.xml")
        assertThat(sheet).contains("<row r=\"1\">")
        assertThat(sheet).contains("<c r=\"A1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Response id</t></is></c>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">Toppings (toppings) / B (b)</t>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">Note &lt;&amp; &quot;quoted&quot;&gt; (note)</t>")
        assertThat(sheet).contains("<c r=\"A2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">00000000-0000-0000-0000-000000000001</t></is></c>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">2026-11-12T10:00:00Z</t>")
        assertThat(sheet).contains("<c r=\"C2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Blue</t></is></c>")
        assertThat(sheet).contains("<c r=\"D2\"><v>0</v></c><c r=\"E2\"><v>1</v></c>")
        assertThat(sheet).contains("<c r=\"F2\"><v>30</v></c>")
        assertThat(sheet).contains("<c r=\"G2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">=SUM(A1) &amp; &lt;b&gt;</t></is></c>")
        // The empty response keeps its two leading cells and no other.
        assertThat(Regex("<row r=\"3\">(.*?)</row>").find(sheet)!!.groupValues[1]).doesNotContain("r=\"C3\"")
        assertThat(parts.getValue("xl/workbook.xml")).contains("<sheet name=\"Responses\" sheetId=\"1\" r:id=\"rId1\"/>")
    }

    @Test
    fun `column names and escaping follow the spreadsheet conventions`() {
        assertThat(listOf(0, 1, 25, 26, 27, 51, 52, 701, 702).map { XlsxExport.columnName(it) }).containsExactly("A", "B", "Z", "AA", "AB", "AZ", "BA", "ZZ", "AAA")
        assertThat(XlsxExport.escape("a<b>&\"c\"\u0001\t")).isEqualTo("a&lt;b&gt;&amp;&quot;c&quot;\t")
    }

    private fun parts(bytes: ByteArray): Map<String, String> = buildMap {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry -> put(entry.name, String(zip.readBytes(), Charsets.UTF_8)) }
        }
    }
}
