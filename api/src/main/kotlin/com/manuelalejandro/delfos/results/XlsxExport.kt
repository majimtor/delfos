package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.evaluation.Answer
import com.manuelalejandro.delfos.results.ExportLayout.Cell
import java.io.OutputStream
import java.io.Writer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Renders the shared export layout as an Office Open XML workbook with one sheet, written by hand:
 * the package is a zip of six small XML parts. Texts go as inline strings, which a spreadsheet never
 * evaluates, so no formula protection is needed; numbers and flags go as numbers; the submission
 * time goes as a date cell. The header row is bold and stays in view, and each column is as wide as
 * its longest content, within limits, which asks for the rows before the sheet starts.
 */
class XlsxExport(questions: List<Question>) {

    private val layout = ExportLayout(questions)

    class Row(val id: UUID, val submittedAt: Instant, val answers: List<Answer>)

    /** Writes the whole workbook; the rows are read whole first, for the widths of the columns. */
    fun write(out: OutputStream, rows: Sequence<Row>) {
        val all = rows.map { layout.cells(it.id, it.submittedAt, it.answers) }.toList()
        val widths = layout.columns.indices.map { index ->
            val longest = all.maxOfOrNull { cells -> length(cells[index]) } ?: 0
            (maxOf(layout.columns[index].header.length, longest) + WIDTH_PADDING).coerceIn(MIN_WIDTH, MAX_WIDTH)
        }
        ZipOutputStream(out, Charsets.UTF_8).use { zip ->
            zip.part("[Content_Types].xml", CONTENT_TYPES)
            zip.part("_rels/.rels", ROOT_RELS)
            zip.part("xl/workbook.xml", WORKBOOK)
            zip.part("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            zip.part("xl/styles.xml", STYLES)
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val writer = zip.writer(Charsets.UTF_8)
            writer.write(XML_HEAD)
            writer.write("<worksheet xmlns=\"$MAIN_NS\">$SHEET_VIEWS<cols>")
            widths.forEachIndexed { index, width -> writer.write("<col min=\"${index + 1}\" max=\"${index + 1}\" width=\"$width\" customWidth=\"1\"/>") }
            writer.write("</cols><sheetData>")
            writer.row(1, layout.columns.map { Cell.Text(it.header) }, HEADER_STYLE)
            all.forEachIndexed { index, cells -> writer.row(index + 2, cells, null) }
            writer.write("</sheetData></worksheet>")
            writer.flush()
            zip.closeEntry()
        }
    }

    /** The characters a cell shows on its widest line; a date is shown to the second. */
    private fun length(cell: Cell): Int = when (cell) {
        is Cell.Text -> cell.value.lineSequence().maxOf { it.length }
        is Cell.Number -> CsvExport.plain(cell.value).length
        is Cell.Flag -> 1
        is Cell.Timestamp -> DATE_LENGTH
        Cell.Empty -> 0
    }

    private fun Writer.row(number: Int, cells: List<Cell>, style: Int?) {
        write("<row r=\"$number\">")
        val styled = if (style == null) "" else " s=\"$style\""
        cells.forEachIndexed { index, cell ->
            val ref = "${columnName(index)}$number"
            when (cell) {
                is Cell.Text -> write("<c r=\"$ref\"$styled t=\"inlineStr\"><is><t xml:space=\"preserve\">${escape(cell.value)}</t></is></c>")
                is Cell.Number -> write("<c r=\"$ref\"$styled><v>${CsvExport.plain(cell.value)}</v></c>")
                is Cell.Flag -> write("<c r=\"$ref\"$styled><v>${if (cell.value) 1 else 0}</v></c>")
                is Cell.Timestamp -> write("<c r=\"$ref\" s=\"$DATE_STYLE\"><v>${serial(cell.value)}</v></c>")
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

        // Indexes into the cell formats of styles.xml: the bold header, the date to the second.
        private const val HEADER_STYLE = 1
        private const val DATE_STYLE = 2

        // Column widths in characters of the default font: the content plus a margin, within limits.
        private const val WIDTH_PADDING = 2
        private const val MIN_WIDTH = 10
        private const val MAX_WIDTH = 60
        private const val DATE_LENGTH = "yyyy-mm-dd hh:mm:ss".length

        // A spreadsheet counts days from 1899-12-30; 1970-01-01 is day 25569.
        private val EPOCH_DAY = BigDecimal(25569)
        private val SECONDS_PER_DAY = BigDecimal(86400)
        private const val SERIAL_SCALE = 10

        /** The serial date a spreadsheet stores: days since 1899-12-30, with the time of day as a fraction, in UTC. */
        fun serial(instant: Instant): String =
            BigDecimal(instant.epochSecond).divide(SECONDS_PER_DAY, SERIAL_SCALE, RoundingMode.HALF_UP).add(EPOCH_DAY).stripTrailingZeros().toPlainString()

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
            "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"yyyy-mm-dd hh:mm:ss\"/></numFmts>" +
            "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
            "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>" +
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"3\">" +
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
            "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
            "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
            "</cellXfs>" +
            "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
            "</styleSheet>"
        // The first row stays in view while the sheet scrolls.
        private const val SHEET_VIEWS = "<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>"
    }
}
