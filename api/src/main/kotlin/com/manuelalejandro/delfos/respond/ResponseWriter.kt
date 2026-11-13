package com.manuelalejandro.delfos.respond

import com.manuelalejandro.delfos.definition.SurveyDefinition
import com.manuelalejandro.delfos.evaluation.Answer
import com.manuelalejandro.delfos.persistence.SurveyResponseEntity
import com.manuelalejandro.delfos.persistence.SurveyResponseRepository
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Component

/** Stores one validated response with its typed answers; shared by the public submission and the seed. */
@Component
class ResponseWriter(private val responses: SurveyResponseRepository, private val answers: AnswerStore) {

    fun store(versionId: UUID, definition: SurveyDefinition, givenAnswers: List<Answer>, submittedAt: Instant, invitationId: UUID? = null): SurveyResponseEntity {
        // Flushed so the answers inserted with SQL can reference the row in the same transaction.
        val response = responses.saveAndFlush(SurveyResponseEntity(UUID.randomUUID(), versionId, invitationId, submittedAt))
        val typeByKey = definition.questions.associate { it.key to it.type }
        answers.insertAll(response.id, givenAnswers.map { AnswerStore.TypedAnswer(it, typeByKey.getValue(it.questionKey)) })
        return response
    }
}
