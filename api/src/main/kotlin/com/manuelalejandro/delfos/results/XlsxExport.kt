package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.evaluation.Answer
import com.manuelalejandro.delfos.results.ExportLayout.Cell
import java.io.OutputStream
import java.io.Writer
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Renders the shared export layout as an Office Open XML workbook with one sheet, written by hand:
 * the package is a zip of six small XML parts. Texts go as inline strings, which a spreadsheet never
 * evaluates, so no formula protection is needed; numbers and flags go as numbers.
 */
class XlsxExport(questions: List<Question>) {

    private val layout = ExportLayout(questions)

    class Row(val id: UUID, val submittedAt: Instant, val answers: List<Answer>)

    /** Writes the whole workbook; the rows are pulled from the sequence as the sheet is written. */
    fun write(out: OutputStream, rows: Sequence<Row>) {
        ZipOutputStream(out, Charsets.UTF_8).use { zip ->
            zip.part("[Content_Types].xml", CONTENT_TYPES)
            zip.part("_rels/.rels", ROOT_RELS)
            zip.part("xl/workbook.xml", WORKBOOK)
            zip.part("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            zip.part("xl/styles.xml", STYLES)
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val writer = zip.writer(Charsets.UTF_8)
            writer.write(XML_HEAD)
            writer.write("<worksheet xmlns=\"$MAIN_NS\"><sheetData>")
            writer.row(1, layout.columns.map { Cell.Text(it.header) })
            rows.forEachIndexed { index, row -> writer.row(index + 2, layout.cells(row.id, row.submittedAt, row.answers)) }
            writer.write("</sheetData></worksheet>")
            writer.flush()
            zip.closeEntry()
        }
    }

    private fun Writer.row(number: Int, cells: List<Cell>) {
        write("<row r=\"$number\">")
        cells.forEachIndexed { index, cell ->
            val ref = "${columnName(index)}$number"
            when (cell) {
                is Cell.Text -> write("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escape(cell.value)}</t></is></c>")
                is Cell.Number -> write("<c r=\"$ref\"><v>${CsvExport.plain(cell.value)}</v></c>")
                is Cell.Flag -> write("<c r=\"$ref\"><v>${if (cell.value) 1 else 0}</v></c>")
                Cell.Empty -> Unit
            }
        }
        write("</row>")
    }

    private fun ZipOutputStream.part(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    companion object {
        const val CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

        private const val MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        private const val XML_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        private const val LETTERS = 26

        /** Column names as spreadsheets count them: A to Z, then AA, AB and so on. */
        fun columnName(index: Int): String {
            var n = index
            val name = StringBuilder()
            do {
                name.insert(0, 'A' + n % LETTERS)
                n = n / LETTERS - 1
            } while (n >= 0)
            return name.toString()
        }

        /** XML-escaped, without the control characters XML 1.0 forbids. */
        fun escape(text: String): String = buildString(text.length) {
            for (c in text) {
                when {
                    c == '&' -> append("&amp;")
                    c == '<' -> append("&lt;")
                    c == '>' -> append("&gt;")
                    c == '"' -> append("&quot;")
                    c == '\t' || c == '\n' || c == '\r' || c >= ' ' -> append(c)
                }
            }
        }

        private val CONTENT_TYPES = XML_HEAD +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
            "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
            "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
            "</Types>"
        private val ROOT_RELS = XML_HEAD +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"
        private val WORKBOOK = XML_HEAD +
            "<workbook xmlns=\"$MAIN_NS\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
            "<sheets><sheet name=\"Responses\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"
        private val WORKBOOK_RELS = XML_HEAD +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
            "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
            "</Relationships>"
        private val STYLES = XML_HEAD +
            "<styleSheet xmlns=\"$MAIN_NS\">" +
            "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
            "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>" +
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs>" +
            "</styleSheet>"
    }
}
