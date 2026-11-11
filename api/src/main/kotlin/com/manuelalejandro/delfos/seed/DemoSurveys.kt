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

/** The Spanish sample surveys of the demo. Content, not interface text, hence the language. */
object DemoSurveys {

    private fun options(vararg pairs: Pair<String, String>) = pairs.map { Option(it.first, it.second) }
    private fun rule(target: String, match: MatchMode, vararg conditions: Condition) = VisibilityRule(target, match, conditions.toList())
    private fun cond(source: String, operator: ConditionOperator, value: ConditionValue? = null) = Condition(source, operator, value)
    private fun option(key: String) = ConditionValue(optionKey = key)
    private fun options(vararg keys: String) = ConditionValue(optionKeys = keys.toList())
    private fun number(value: Double) = ConditionValue(number = value)
    private fun between(min: Double, max: Double) = ConditionValue(min = min, max = max)

    /** Ten questions, the five types, and every operator in at least one rule. */
    val mobility: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("transporte", QuestionType.SINGLE_CHOICE, "¿Cómo te desplazas habitualmente al trabajo o al centro de estudios?", required = true,
                options = options("coche" to "En coche", "bici" to "En bicicleta", "publico" to "En transporte público", "pie" to "A pie")),
            Question("motivos", QuestionType.MULTIPLE_CHOICE, "¿Qué motivos pesan más en tu elección?", minSelections = 1, maxSelections = 3,
                options = options("precio" to "El precio", "tiempo" to "El tiempo", "salud" to "La salud", "ambiente" to "El medio ambiente")),
            Question("km", QuestionType.NUMBER, "¿Cuántos kilómetros recorres a la semana?", required = true, min = 0.0, max = 500.0, integerOnly = true),
            Question("satisfaccion", QuestionType.SCALE, "¿Cómo valoras tu forma de desplazarte?", required = true, max = 5.0, minLabel = "Muy mal", maxLabel = "Muy bien"),
            Question("comentario", QuestionType.TEXT, "¿Algo que quieras añadir?", maxLength = 300, multiline = true),
            Question("carnet", QuestionType.SINGLE_CHOICE, "¿Tienes carnet de conducir?", options = options("si" to "Sí", "no" to "No")),
            Question("anios", QuestionType.NUMBER, "¿Cuántos años llevas con carnet?", min = 0.0, max = 70.0, integerOnly = true),
            Question("bici", QuestionType.SCALE, "¿Qué probabilidad hay de que cambies a la bicicleta este año?", max = 10.0, minLabel = "Ninguna", maxLabel = "Seguro"),
            Question("coste", QuestionType.TEXT, "Cuéntanos qué te preocupa del coste y el tiempo.", maxLength = 300),
            Question("seguimiento", QuestionType.TEXT, "¿Te gustaría participar en un grupo de seguimiento? Indica cómo contactarte sin datos personales.", maxLength = 200),
        ),
        rules = listOf(
            rule("carnet", MatchMode.ANY, cond("transporte", ConditionOperator.EQUALS, option("coche")), cond("transporte", ConditionOperator.IN, options("publico", "pie"))),
            rule("anios", MatchMode.ALL, cond("carnet", ConditionOperator.EQUALS, option("si")), cond("transporte", ConditionOperator.NOT_EQUALS, option("bici"))),
            rule("bici", MatchMode.ANY, cond("motivos", ConditionOperator.INCLUDES_ANY, options("salud", "ambiente")), cond("km", ConditionOperator.BETWEEN, between(1.0, 50.0)), cond("satisfaccion", ConditionOperator.LTE, number(2.0))),
            rule("coste", MatchMode.ALL, cond("motivos", ConditionOperator.INCLUDES_ALL, options("precio", "tiempo")), cond("motivos", ConditionOperator.EXCLUDES, options("salud")), cond("km", ConditionOperator.GT, number(0.0)), cond("km", ConditionOperator.NE, number(1.0))),
            rule("seguimiento", MatchMode.ANY, cond("comentario", ConditionOperator.IS_ANSWERED), cond("satisfaccion", ConditionOperator.GTE, number(4.0)), cond("km", ConditionOperator.LT, number(5.0)), cond("km", ConditionOperator.EQ, number(100.0)), cond("anios", ConditionOperator.IS_NOT_ANSWERED)),
        ),
    )

    /** First version of the service survey. */
    val serviceV1: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("canal", QuestionType.SINGLE_CHOICE, "¿Por qué canal contactaste con nosotros?", required = true,
                options = options("telefono" to "Teléfono", "correo" to "Correo electrónico", "chat" to "Chat")),
            Question("espera", QuestionType.NUMBER, "¿Cuántos minutos esperaste?", min = 0.0, max = 120.0, integerOnly = true),
            Question("valoracion", QuestionType.SCALE, "¿Cómo valoras la atención recibida?", required = true, max = 5.0),
            Question("mejora", QuestionType.TEXT, "¿Qué mejorarías?", maxLength = 200),
        ),
        rules = listOf(rule("mejora", MatchMode.ALL, cond("valoracion", ConditionOperator.LTE, number(3.0)))),
    )

    /** Second version: a new channel makes the first question no longer comparable; the rest keep their shape. */
    val serviceV2: SurveyDefinition = serviceV1.copy(
        questions = serviceV1.questions.map { question ->
            if (question.key == "canal") question.copy(options = question.options!! + Option("presencial", "En persona")) else question
        },
    )

    val event: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("asistira", QuestionType.SINGLE_CHOICE, "¿Asistirás al encuentro anual?", required = true, options = options("si" to "Sí", "no" to "No")),
            Question("actividades", QuestionType.MULTIPLE_CHOICE, "¿Qué actividades te interesan?", options = options("charlas" to "Charlas", "talleres" to "Talleres", "networking" to "Networking")),
            Question("dieta", QuestionType.TEXT, "¿Alguna preferencia alimentaria?", maxLength = 100),
        ),
        rules = listOf(
            rule("actividades", MatchMode.ALL, cond("asistira", ConditionOperator.EQUALS, option("si"))),
            rule("dieta", MatchMode.ALL, cond("asistira", ConditionOperator.EQUALS, option("si"))),
        ),
    )

    val readingDraft: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("libros", QuestionType.NUMBER, "¿Cuántos libros leíste el año pasado?", required = true, min = 0.0, max = 200.0, integerOnly = true),
            Question("formato", QuestionType.MULTIPLE_CHOICE, "¿En qué formatos lees?", options = options("papel" to "Papel", "digital" to "Digital", "audio" to "Audiolibro")),
            Question("recomendacion", QuestionType.TEXT, "Recomiéndanos un libro.", maxLength = 150),
        ),
        rules = listOf(rule("recomendacion", MatchMode.ALL, cond("libros", ConditionOperator.GT, number(0.0)))),
    )

    val privateSurvey: SurveyDefinition = SurveyDefinition(
        questions = listOf(
            Question("teletrabajo", QuestionType.SCALE, "¿Cuántos días a la semana teletrabajas?", required = true, min = 0.0, max = 5.0),
            Question("herramientas", QuestionType.TEXT, "¿Qué herramienta echas en falta?", maxLength = 100),
        ),
    )
}
