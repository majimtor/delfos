package com.manuelalejandro.delfos.mobile.answer

import com.manuelalejandro.delfos.definition.Condition
import com.manuelalejandro.delfos.definition.ConditionOperator
import com.manuelalejandro.delfos.definition.ConditionValue
import com.manuelalejandro.delfos.definition.MatchMode
import com.manuelalejandro.delfos.definition.Option
import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.definition.SurveyDefinition
import com.manuelalejandro.delfos.definition.VisibilityRule
import com.manuelalejandro.delfos.mobile.api.PublicSurvey
import com.manuelalejandro.delfos.mobile.api.SurveyVersion

/** A small survey with one rule and one question of every type. */
internal val DEFINITION = SurveyDefinition(
    questions = listOf(
        Question(key = "q1", type = QuestionType.SINGLE_CHOICE, text = "Do you drive?", required = true, options = listOf(Option("yes", "Yes"), Option("no", "No"))),
        Question(key = "q2", type = QuestionType.TEXT, text = "Which car?", maxLength = 10),
        Question(key = "q3", type = QuestionType.NUMBER, text = "Kilometres a day", min = 1.0, max = 10.0, integerOnly = true),
        Question(key = "q4", type = QuestionType.MULTIPLE_CHOICE, text = "Which days?", options = listOf(Option("a", "A"), Option("b", "B"), Option("c", "C")), maxSelections = 2),
        Question(key = "q5", type = QuestionType.SCALE, text = "How satisfied?", min = 1.0, max = 5.0),
    ),
    rules = listOf(
        VisibilityRule(
            targetQuestionKey = "q2",
            match = MatchMode.ALL,
            conditions = listOf(Condition("q1", ConditionOperator.EQUALS, ConditionValue(optionKey = "yes"))),
        ),
    ),
)

internal val SURVEY = PublicSurvey(
    publicId = "demo-survey1",
    title = "Commuting",
    description = "Five questions.",
    language = "en",
    status = "PUBLISHED",
    acceptingResponses = true,
    respondentMode = "ANONYMOUS",
    version = SurveyVersion(
        number = 1,
        status = "PUBLISHED",
        createdAt = "2026-11-13T10:00:00Z",
        publishedAt = "2026-11-13T10:00:00Z",
        questions = DEFINITION.questions,
        rules = DEFINITION.rules,
    ),
)
