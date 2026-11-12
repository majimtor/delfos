package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.evaluation.Answer
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * Reads of the typed answer rows: aggregates for results and keyset pages for listings. All in SQL
 * on purpose; the object model is not involved in counting.
 */
@Repository
class AnswerQueries(private val jdbc: JdbcTemplate) {

    class Scope(val versionIds: Collection<UUID>)

    class Totals(val responseCount: Int, val lastSubmittedAt: Instant?)

    class NumberStats(val count: Int, val mean: Double, val min: Double, val max: Double)

    class ResponseRow(val id: UUID, val versionNumber: Int, val submittedAt: Instant)

    class AnswerRow(val responseId: UUID, val submittedAt: Instant, val answer: Answer)

    fun totals(scope: Scope): Totals = jdbc.query(
        "select count(*) as n, max(submitted_at) as last from response where version_id = any (?)",
        { rs, _ -> Totals(rs.getInt("n"), rs.getTimestamp("last")?.toInstant()) },
        uuidArray(scope),
    ).first()

    fun answerCounts(scope: Scope): Map<String, Int> = jdbc.query(
        "select a.question_key, count(*) as n from answer a join response r on r.id = a.response_id where r.version_id = any (?) group by a.question_key",
        { rs, _ -> rs.getString("question_key") to rs.getInt("n") },
        uuidArray(scope),
    ).toMap()

    /** Selected option counts per question, for both choice types. */
    fun optionCounts(scope: Scope): Map<String, Map<String, Int>> = jdbc.query(
        "select a.question_key, k, count(*) as n from answer a join response r on r.id = a.response_id " +
            "cross join lateral unnest(a.option_keys) as k where r.version_id = any (?) and a.option_keys is not null group by a.question_key, k",
        { rs, _ -> Triple(rs.getString("question_key"), rs.getString("k"), rs.getInt("n")) },
        uuidArray(scope),
    ).groupBy({ it.first }, { it.second to it.third }).mapValues { (_, pairs) -> pairs.toMap() }

    fun numberStats(scope: Scope): Map<String, NumberStats> = jdbc.query(
        "select a.question_key, count(*) as n, avg(a.number_value) as mean, min(a.number_value) as mn, max(a.number_value) as mx " +
            "from answer a join response r on r.id = a.response_id where r.version_id = any (?) and a.number_value is not null group by a.question_key",
        { rs, _ -> rs.getString("question_key") to NumberStats(rs.getInt("n"), rs.getDouble("mean"), rs.getDouble("mn"), rs.getDouble("mx")) },
        uuidArray(scope),
    ).toMap()

    /** Count per value of the scale questions, ascending by value. */
    fun scaleDistributions(scope: Scope): Map<String, List<Pair<Int, Int>>> = jdbc.query(
        "select a.question_key, a.number_value as v, count(*) as n from answer a join response r on r.id = a.response_id " +
            "where r.version_id = any (?) and a.type = 'SCALE' group by a.question_key, a.number_value order by a.question_key, a.number_value",
        { rs, _ -> Triple(rs.getString("question_key"), rs.getBigDecimal("v").toInt(), rs.getInt("n")) },
        uuidArray(scope),
    ).groupBy({ it.first }, { it.second to it.third })

    fun responsePage(versionId: UUID, versionNumber: Int, after: Pair<Instant, UUID>?, limit: Int): List<ResponseRow> {
        val mapper = { rs: ResultSet, _: Int -> ResponseRow(rs.getObject("id", UUID::class.java), versionNumber, rs.getTimestamp("submitted_at").toInstant()) }
        return if (after == null) {
            jdbc.query("select id, submitted_at from response where version_id = ? order by submitted_at desc, id desc limit ?", mapper, versionId, limit)
        } else {
            jdbc.query(
                "select id, submitted_at from response where version_id = ? and (submitted_at, id) < (?, ?) order by submitted_at desc, id desc limit ?",
                mapper, versionId, java.sql.Timestamp.from(after.first), after.second, limit,
            )
        }
    }

    /** The responses after a keyset position, oldest first; the export reads them batch by batch. */
    fun responsesAscending(versionId: UUID, versionNumber: Int, after: Pair<Instant, UUID>?, limit: Int): List<ResponseRow> {
        val mapper = { rs: ResultSet, _: Int -> ResponseRow(rs.getObject("id", UUID::class.java), versionNumber, rs.getTimestamp("submitted_at").toInstant()) }
        return if (after == null) {
            jdbc.query("select id, submitted_at from response where version_id = ? order by submitted_at, id limit ?", mapper, versionId, limit)
        } else {
            jdbc.query(
                "select id, submitted_at from response where version_id = ? and (submitted_at, id) > (?, ?) order by submitted_at, id limit ?",
                mapper, versionId, java.sql.Timestamp.from(after.first), after.second, limit,
            )
        }
    }

    fun answersOf(responseIds: Collection<UUID>): Map<UUID, List<Answer>> {
        if (responseIds.isEmpty()) return emptyMap()
        return jdbc.query(
            "select response_id, question_key, type, option_keys, number_value, text_value from answer where response_id = any (?)",
            { rs, _ -> rs.getObject("response_id", UUID::class.java) to answerOf(rs) },
            jdbc.dataSource!!.connection.use { it.createArrayOf("uuid", responseIds.toTypedArray()) },
        ).groupBy({ it.first }, { it.second })
    }

    fun answerPage(versionId: UUID, questionKey: String, after: Pair<Instant, UUID>?, limit: Int): List<AnswerRow> {
        val mapper = { rs: ResultSet, _: Int -> AnswerRow(rs.getObject("response_id", UUID::class.java), rs.getTimestamp("submitted_at").toInstant(), answerOf(rs)) }
        val select = "select r.id as response_id, r.submitted_at, a.question_key, a.type, a.option_keys, a.number_value, a.text_value " +
            "from answer a join response r on r.id = a.response_id where r.version_id = ? and a.question_key = ?"
        return if (after == null) {
            jdbc.query("$select order by r.submitted_at desc, r.id desc limit ?", mapper, versionId, questionKey, limit)
        } else {
            jdbc.query(
                "$select and (r.submitted_at, r.id) < (?, ?) order by r.submitted_at desc, r.id desc limit ?",
                mapper, versionId, questionKey, java.sql.Timestamp.from(after.first), after.second, limit,
            )
        }
    }

    private fun answerOf(rs: ResultSet): Answer {
        val key = rs.getString("question_key")
        val optionKeys = (rs.getArray("option_keys")?.array as? Array<*>)?.map { it as String }
        return when (QuestionType.valueOf(rs.getString("type"))) {
            QuestionType.SINGLE_CHOICE -> Answer(key, optionKey = optionKeys?.firstOrNull())
            QuestionType.MULTIPLE_CHOICE -> Answer(key, optionKeys = optionKeys)
            QuestionType.NUMBER, QuestionType.SCALE -> Answer(key, number = rs.getBigDecimal("number_value")?.toDouble())
            QuestionType.TEXT -> Answer(key, text = rs.getString("text_value"))
        }
    }

    private fun uuidArray(scope: Scope): java.sql.Array =
        jdbc.dataSource!!.connection.use { it.createArrayOf("uuid", scope.versionIds.toTypedArray()) }
}
