package com.manuelalejandro.delfos.respond

import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.evaluation.Answer
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.Types
import java.util.UUID
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/** Writes the typed answer rows of a response. Answers are never updated; results aggregate them in SQL. */
@Repository
class AnswerStore(private val jdbc: JdbcTemplate) {

    class TypedAnswer(val answer: Answer, val type: QuestionType)

    fun insertAll(responseId: UUID, answers: List<TypedAnswer>) {
        if (answers.isEmpty()) return
        jdbc.batchUpdate(
            "insert into answer (response_id, question_key, type, option_keys, number_value, text_value) values (?, ?, ?, ?, ?, ?)",
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    val (answer, type) = answers[i].answer to answers[i].type
                    ps.setObject(1, responseId)
                    ps.setString(2, answer.questionKey)
                    ps.setString(3, type.name)
                    val optionKeys = when (type) {
                        QuestionType.SINGLE_CHOICE -> listOf(answer.optionKey!!)
                        QuestionType.MULTIPLE_CHOICE -> answer.optionKeys!!
                        else -> null
                    }
                    if (optionKeys != null) ps.setArray(4, ps.connection.createArrayOf("text", optionKeys.toTypedArray())) else ps.setNull(4, Types.ARRAY)
                    val number = answer.number
                    if (number != null) ps.setBigDecimal(5, BigDecimal.valueOf(number)) else ps.setNull(5, Types.NUMERIC)
                    val text = answer.text
                    if (text != null) ps.setString(6, text) else ps.setNull(6, Types.VARCHAR)
                }

                override fun getBatchSize(): Int = answers.size
            },
        )
    }
}
