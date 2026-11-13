package com.manuelalejandro.delfos.evaluation

import com.manuelalejandro.delfos.definition.SurveyDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * Runs every case of the conformance suite in `contract/conformance`, embedded at build time so
 * the same cases run on the JVM, on Android and on iOS. Errors are compared as a set; an expected
 * `params` entry must be present with the same value, extra entries are allowed.
 */
class ConformanceSuiteTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class EvaluationSuite(val suite: String, val definition: SurveyDefinition, val cases: List<EvaluationCase>)

    @Serializable
    private data class EvaluationCase(val name: String, val answers: List<Answer>, val expected: EvaluationResult)

    @Serializable
    private data class ValidationSuite(val suite: String, val cases: List<ValidationCase>)

    @Serializable
    private data class ValidationCase(
        val name: String,
        val mode: String = "save",
        val definition: SurveyDefinition,
        val expectedErrors: List<FieldError>,
    )

    @Test
    fun evaluationSuites() {
        val files = suiteFiles { it.startsWith("0") && !it.contains("definition-validation") }
        var cases = 0
        for ((name, text) in files) {
            val suite = json.decodeFromString<EvaluationSuite>(text)
            assertTrue(DefinitionValidator.validate(suite.definition, ValidationMode.PUBLISH).isEmpty(), "$name: definition must be valid")
            val evaluator = RulesEvaluator(suite.definition)
            for (case in suite.cases) {
                val label = "${suite.suite}: ${case.name}"
                val actual = evaluator.evaluate(case.answers)
                assertEquals(case.expected.visibleQuestionKeys, actual.visibleQuestionKeys, "$label: visible questions")
                assertEquals(case.expected.missingRequiredQuestionKeys, actual.missingRequiredQuestionKeys, "$label: missing required")
                assertSameErrors(label, case.expected.errors, actual.errors)
                cases++
            }
        }
        assertTrue(cases > 0, "no evaluation cases")
    }

    @Test
    fun definitionValidationSuite() {
        val files = suiteFiles { it.contains("definition-validation") }
        var cases = 0
        for ((_, text) in files) {
            val suite = json.decodeFromString<ValidationSuite>(text)
            for (case in suite.cases) {
                val mode = if (case.mode == "publish") ValidationMode.PUBLISH else ValidationMode.SAVE
                assertSameErrors("${suite.suite}: ${case.name}", case.expectedErrors, DefinitionValidator.validate(case.definition, mode))
                cases++
            }
        }
        assertTrue(cases > 0, "no validation cases")
    }

    private fun suiteFiles(select: (String) -> Boolean): List<Pair<String, String>> {
        val files = CONFORMANCE_CASES.filter { (name, _) -> select(name) }
        assertTrue(files.isNotEmpty(), "no suite files embedded")
        return files
    }

    private fun assertSameErrors(label: String, expected: List<FieldError>, actual: List<FieldError>) {
        val remaining = actual.toMutableList()
        for (e in expected) {
            val match = remaining.firstOrNull { a -> a.path == e.path && a.code == e.code && paramsCover(e.params, a.params) }
                ?: fail("$label: missing error $e; actual errors: $actual")
            remaining.remove(match)
        }
        assertTrue(remaining.isEmpty(), "$label: unexpected errors: $remaining")
    }

    private fun paramsCover(expected: Map<String, JsonPrimitive>?, actual: Map<String, JsonPrimitive>?): Boolean =
        expected.orEmpty().all { (key, value) ->
            val found = actual?.get(key) ?: return false
            val numeric = value.doubleOrNull
            if (numeric != null && !value.isString) found.doubleOrNull == numeric else found == value
        }
}
