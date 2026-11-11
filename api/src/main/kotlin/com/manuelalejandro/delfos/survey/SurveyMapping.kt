package com.manuelalejandro.delfos.survey

import com.manuelalejandro.delfos.api.generated.model.Language
import com.manuelalejandro.delfos.api.generated.model.RespondentMode
import com.manuelalejandro.delfos.api.generated.model.Survey
import com.manuelalejandro.delfos.api.generated.model.SurveyStatus
import com.manuelalejandro.delfos.api.generated.model.SurveyVersion
import com.manuelalejandro.delfos.api.generated.model.VersionStatus
import com.manuelalejandro.delfos.api.generated.model.VersionSummary
import com.manuelalejandro.delfos.persistence.SurveyEntity
import com.manuelalejandro.delfos.persistence.SurveyVersionEntity
import com.manuelalejandro.delfos.survey.DefinitionCodec.toModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Entities to the DTOs of the contract. */
object SurveyMapping {

    fun Instant.utc(): OffsetDateTime = atOffset(ZoneOffset.UTC)

    fun survey(entity: SurveyEntity, responseCount: Int, publishedVersionNumber: Int?, hasDraft: Boolean): Survey = Survey(
        id = entity.id,
        publicId = entity.publicId,
        title = entity.title,
        language = Language.entries.first { it.value == entity.language },
        status = SurveyStatus.valueOf(entity.status.name),
        respondentMode = RespondentMode.valueOf(entity.respondentMode.name),
        responseCount = responseCount,
        hasDraft = hasDraft,
        createdAt = entity.createdAt.utc(),
        updatedAt = entity.updatedAt.utc(),
        description = entity.description,
        closesAt = entity.closesAt?.utc(),
        maxResponses = entity.maxResponses,
        publishedVersionNumber = publishedVersionNumber,
    )

    fun version(entity: SurveyVersionEntity): SurveyVersion = SurveyVersion(
        number = entity.number,
        status = VersionStatus.valueOf(entity.status.name),
        createdAt = entity.createdAt.utc(),
        questions = entity.questions.map { it.toModel() },
        rules = DefinitionCodec.rulesOf(entity),
        publishedAt = entity.publishedAt?.utc(),
    )

    fun summary(entity: SurveyVersionEntity, questionCount: Int, responseCount: Int): VersionSummary = VersionSummary(
        number = entity.number,
        status = VersionStatus.valueOf(entity.status.name),
        questionCount = questionCount,
        responseCount = responseCount,
        createdAt = entity.createdAt.utc(),
        publishedAt = entity.publishedAt?.utc(),
    )
}
