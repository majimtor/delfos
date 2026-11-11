package com.manuelalejandro.delfos.survey

import com.manuelalejandro.delfos.api.generated.model.DraftDefinition
import com.manuelalejandro.delfos.definition.Option
import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.definition.SurveyDefinition
import com.manuelalejandro.delfos.definition.VisibilityRule
import com.manuelalejandro.delfos.persistence.QuestionEntity
import com.manuelalejandro.delfos.persistence.QuestionOptionEntity
import com.manuelalejandro.delfos.persistence.SurveyVersionEntity
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Per-type configuration stored in the `config` column of a question, as the contract names it. */
@Serializable
data class QuestionConfig(
    val minSelections: Int? = null,
    val maxSelections: Int? = null,
    val maxLength: Int? = null,
    val multiline: Boolean? = null,
    val min: Double? = null,
    val max: Double? = null,
    val integerOnly: Boolean? = null,
    val minLabel: String? = null,
    val maxLabel: String? = null,
)

/** Converts between the stored shape of a version and the shared definition model. */
object DefinitionCodec {

    private val json = Json { explicitNulls = false; ignoreUnknownKeys = true; encodeDefaults = false }
    private val choiceTypes = setOf(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE)

    fun rulesOf(version: SurveyVersionEntity): List<VisibilityRule> = json.decodeFromString(version.rules)

    fun encodeRules(rules: List<VisibilityRule>): String = json.encodeToString(rules)

    fun definitionOf(version: SurveyVersionEntity): SurveyDefinition =
        SurveyDefinition(version.questions.map { it.toModel() }, rulesOf(version))

    fun QuestionEntity.toModel(): Question {
        val config = json.decodeFromString<QuestionConfig>(this.config)
        val type = QuestionType.valueOf(this.type)
        return Question(
            key = key,
            type = type,
            text = text,
            required = required,
            options = if (type in choiceTypes) options.map { Option(it.key, it.label) } else null,
            minSelections = config.minSelections,
            maxSelections = config.maxSelections,
            maxLength = config.maxLength,
            multiline = config.multiline,
            min = config.min,
            max = config.max,
            integerOnly = config.integerOnly,
            minLabel = config.minLabel,
            maxLabel = config.maxLabel,
        )
    }

    /** The request body as a definition, with keys assigned to the questions and options that have none. */
    fun fromRequest(draft: DraftDefinition): SurveyDefinition = SurveyDefinition(
        questions = draft.questions.map { input ->
            Question(
                key = input.key ?: Identifiers.newQuestionKey(),
                type = input.type,
                text = input.text,
                required = input.required ?: false,
                options = input.options?.map { Option(it.key ?: Identifiers.newOptionKey(), it.label) },
                minSelections = input.minSelections,
                maxSelections = input.maxSelections,
                maxLength = input.maxLength,
                // The generated DTO defaults these flags to false; only true is a value the client chose.
                multiline = input.multiline?.takeIf { it },
                min = input.min,
                max = input.max,
                integerOnly = input.integerOnly?.takeIf { it },
                minLabel = input.minLabel,
                maxLabel = input.maxLabel,
            )
        },
        rules = draft.rules,
    )

    /** New question entities for a version, in order, from a validated definition. */
    fun toEntities(version: SurveyVersionEntity, definition: SurveyDefinition): List<QuestionEntity> =
        definition.questions.mapIndexed { position, question ->
            val entity = QuestionEntity(
                id = UUID.randomUUID(),
                version = version,
                key = question.key,
                position = position,
                type = question.type.name,
                text = question.text,
                required = question.required,
                config = json.encodeToString(
                    QuestionConfig(
                        minSelections = question.minSelections,
                        maxSelections = question.maxSelections,
                        maxLength = question.maxLength,
                        multiline = question.multiline,
                        min = question.min,
                        max = question.max,
                        integerOnly = question.integerOnly,
                        minLabel = question.minLabel,
                        maxLabel = question.maxLabel,
                    ),
                ),
            )
            question.options?.forEachIndexed { optionPosition, option ->
                entity.options += QuestionOptionEntity(UUID.randomUUID(), entity, option.key, optionPosition, option.label)
            }
            entity
        }
}
