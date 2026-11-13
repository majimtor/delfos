package com.manuelalejandro.delfos.seed

import com.manuelalejandro.delfos.definition.Condition
import com.manuelalejandro.delfos.definition.ConditionOperator
import com.manuelalejandro.delfos.definition.ConditionValue
import com.manuelalejandro.delfos.definition.MatchMode
import com.manuelalejandro.delfos.definition.Option
import com.manuelalejandro.delfos.definition.Question
import com.manuelalejandro.delfos.definition.QuestionType
import com.manuelalejandro.delfos.definition.SurveyDefinition
import com.manuelalejandro.delfos.definition.VisibilityRule

/**
 * The English sample surveys of the demo: the same shapes and rules as [DemoSurveys], so both
 * languages exercise every question type and operator, with English keys and texts.
 */
object EnglishDemoSurveys {

    private fun options(vararg pairs: Pair<String, String>) = pairs.map { Option(it.first, it.second) }
    private fun rule(target: String, match: MatchMode, vararg conditions: Condition) = VisibilityRule(target, match, conditions.toList())
    private fun cond(source: String, operator: ConditionOperator, value: ConditionValue? = null) = Condition(source, operator, value)
    private fun option(key: String) = ConditionValue(optionKey = key)
    private fun options(vararg keys: String) = ConditionValue(optionKeys = keys.toList())
    private fun number(value: Double) = ConditionValue(number = value)
    private fun between(min: Double, max: Double) = ConditionValue(min = min, max = max)

    val mobility: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("transport", QuestionType.SINGLE_CHOICE, "How do you usually travel to work or to your place of study?", required = true,
                options = options("car" to "By car", "bike" to "By bicycle", "public" to "By public transport", "foot" to "On foot")),
            Question("reasons", QuestionType.MULTIPLE_CHOICE, "Which reasons weigh most in your choice?", minSelections = 1, maxSelections = 3,
                options = options("price" to "The price", "time" to "The time", "health" to "Health", "environment" to "The environment")),
            Question("km", QuestionType.NUMBER, "How many kilometres do you travel a week?", required = true, min = 0.0, max = 500.0, integerOnly = true),
            Question("satisfaction", QuestionType.SCALE, "How do you rate the way you travel?", required = true, max = 5.0, minLabel = "Very bad", maxLabel = "Very good"),
            Question("comment", QuestionType.TEXT, "Anything you would like to add?", maxLength = 300, multiline = true),
            Question("licence", QuestionType.SINGLE_CHOICE, "Do you have a driving licence?", options = options("yes" to "Yes", "no" to "No")),
            Question("years", QuestionType.NUMBER, "How many years have you had it?", min = 0.0, max = 70.0, integerOnly = true),
            Question("bike", QuestionType.SCALE, "How likely are you to switch to a bicycle this year?", max = 10.0, minLabel = "Not at all", maxLabel = "Certain"),
            Question("cost", QuestionType.TEXT, "Tell us what worries you about cost and time.", maxLength = 300),
            Question("followup", QuestionType.TEXT, "Would you like to join a follow-up group? Tell us how to reach you without personal data.", maxLength = 200),
        ),
        rules = listOf(
            rule("licence", MatchMode.ANY, cond("transport", ConditionOperator.EQUALS, option("car")), cond("transport", ConditionOperator.IN, options("public", "foot"))),
            rule("years", MatchMode.ALL, cond("licence", ConditionOperator.EQUALS, option("yes")), cond("transport", ConditionOperator.NOT_EQUALS, option("bike"))),
            rule("bike", MatchMode.ANY, cond("reasons", ConditionOperator.INCLUDES_ANY, options("health", "environment")), cond("km", ConditionOperator.BETWEEN, between(1.0, 50.0)), cond("satisfaction", ConditionOperator.LTE, number(2.0))),
            rule("cost", MatchMode.ALL, cond("reasons", ConditionOperator.INCLUDES_ALL, options("price", "time")), cond("reasons", ConditionOperator.EXCLUDES, options("health")), cond("km", ConditionOperator.GT, number(0.0)), cond("km", ConditionOperator.NE, number(1.0))),
            rule("followup", MatchMode.ANY, cond("comment", ConditionOperator.IS_ANSWERED), cond("satisfaction", ConditionOperator.GTE, number(4.0)), cond("km", ConditionOperator.LT, number(5.0)), cond("km", ConditionOperator.EQ, number(100.0)), cond("years", ConditionOperator.IS_NOT_ANSWERED)),
        ),
    )

    val serviceV1: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("channel", QuestionType.SINGLE_CHOICE, "Through which channel did you contact us?", required = true,
                options = options("phone" to "Phone", "email" to "Email", "chat" to "Chat")),
            Question("wait", QuestionType.NUMBER, "How many minutes did you wait?", min = 0.0, max = 120.0, integerOnly = true),
            Question("rating", QuestionType.SCALE, "How do you rate the service you received?", required = true, max = 5.0),
            Question("improvement", QuestionType.TEXT, "What would you improve?", maxLength = 200),
        ),
        rules = listOf(rule("improvement", MatchMode.ALL, cond("rating", ConditionOperator.LTE, number(3.0)))),
    )

    /** Second version: a new channel makes the first question no longer comparable; the rest keep their shape. */
    val serviceV2: SurveyDefinition = serviceV1.copy(
        questions = serviceV1.questions.map { question ->
            if (question.key == "channel") question.copy(options = question.options!! + Option("in-person", "In person")) else question
        },
    )

    val meetup: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("attending", QuestionType.SINGLE_CHOICE, "Will you attend the annual meetup?", required = true, options = options("yes" to "Yes", "no" to "No")),
            Question("activities", QuestionType.MULTIPLE_CHOICE, "Which activities interest you?", options = options("talks" to "Talks", "workshops" to "Workshops", "networking" to "Networking")),
            Question("diet", QuestionType.TEXT, "Any dietary preference?", maxLength = 100),
        ),
        rules = listOf(
            rule("activities", MatchMode.ALL, cond("attending", ConditionOperator.EQUALS, option("yes"))),
            rule("diet", MatchMode.ALL, cond("attending", ConditionOperator.EQUALS, option("yes"))),
        ),
    )

    val readingDraft: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("books", QuestionType.NUMBER, "How many books did you read last year?", required = true, min = 0.0, max = 200.0, integerOnly = true),
            Question("formats", QuestionType.MULTIPLE_CHOICE, "Which formats do you read in?", options = options("paper" to "Paper", "digital" to "Digital", "audio" to "Audiobook")),
            Question("recommendation", QuestionType.TEXT, "Recommend us a book.", maxLength = 150),
        ),
        rules = listOf(rule("recommendation", MatchMode.ALL, cond("books", ConditionOperator.GT, number(0.0)))),
    )

    val workshop: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("rating", QuestionType.SCALE, "How do you rate the workshop?", required = true, max = 5.0, minLabel = "Very bad", maxLabel = "Very good"),
            Question("parts", QuestionType.MULTIPLE_CHOICE, "Which part was most useful?", options = options("demo" to "The demonstration", "exercises" to "The exercises", "discussion" to "The discussion")),
            Question("changes", QuestionType.TEXT, "What would you change?", maxLength = 200),
        ),
    )

    val privateSurvey: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("remote", QuestionType.SCALE, "How many days a week do you work remotely?", required = true, min = 0.0, max = 5.0),
            Question("tools", QuestionType.TEXT, "Which tool do you miss?", maxLength = 100),
        ),
    )

    /** Free-text answers of the synthetic responses. */
    val texts: List<String> = listOf(
        "No comments.", "All good.", "I would improve the frequency.", "Very satisfied overall.",
        "It depends on the day.", "I would rather not say.", "Good experience.", "Could be better.",
    )
}
