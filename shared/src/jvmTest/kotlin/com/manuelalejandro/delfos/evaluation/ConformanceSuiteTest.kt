package com.manuelalejandro.delfos.evaluation

import com.manuelalejandro.delfos.definition.SurveyDefinition
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Runs every case of the conformance suite in `contract/conformance`. Errors are compared as a set;
 * an expected `params` entry must be present with the same value, extra entries are allowed.
 */
class ConformanceSuiteTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val directory = File(System.getProperty("delfos.conformance.dir") ?: fail("delfos.conformance.dir is not set"))

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

    @TestFactory
    fun `evaluation suites`(): List<DynamicTest> =
        suiteFiles { it.startsWith("0") && !it.contains("definition-validation") }.flatMap { file ->
            val suite = json.decodeFromString<EvaluationSuite>(file.readText())
            assertTrue(DefinitionValidator.validate(suite.definition, ValidationMode.PUBLISH).isEmpty(), "${suite.suite}: definition must be valid")
            val evaluator = RulesEvaluator(suite.definition)
            suite.cases.map { case ->
                DynamicTest.dynamicTest("${suite.suite}: ${case.name}") {
                    val actual = evaluator.evaluate(case.answers)
                    assertEquals(case.expected.visibleQuestionKeys, actual.visibleQuestionKeys, "visible questions")
                    assertEquals(case.expected.missingRequiredQuestionKeys, actual.missingRequiredQuestionKeys, "missing required")
                    assertSameErrors(case.expected.errors, actual.errors)
                }
            }
        }

    @TestFactory
    fun `definition validation suite`(): List<DynamicTest> =
        suiteFiles { it.contains("definition-validation") }.flatMap { file ->
            val suite = json.decodeFromString<ValidationSuite>(file.readText())
            suite.cases.map { case ->
                DynamicTest.dynamicTest("${suite.suite}: ${case.name}") {
                    val mode = if (case.mode == "publish") ValidationMode.PUBLISH else ValidationMode.SAVE
                    assertSameErrors(case.expectedErrors, DefinitionValidator.validate(case.definition, mode))
                }
            }
        }

    private fun suiteFiles(select: (String) -> Boolean): List<File> {
        val files = directory.listFiles { f -> f.name.endsWith(".json") && select(f.name) }?.sortedBy { it.name }.orEmpty()
        assertTrue(files.isNotEmpty(), "no suite files in $directory")
        return files
    }

    private fun assertSameErrors(expected: List<FieldError>, actual: List<FieldError>) {
        val remaining = actual.toMutableList()
        for (e in expected) {
            val match = remaining.firstOrNull { a -> a.path == e.path && a.code == e.code && paramsCover(e.params, a.params) }
                ?: fail("missing error $e; actual errors: $actual")
            remaining.remove(match)
        }
        assertTrue(remaining.isEmpty(), "unexpected errors: $remaining")
    }

    private fun paramsCover(expected: Map<String, JsonPrimitive>?, actual: Map<String, JsonPrimitive>?): Boolean =
        expected.orEmpty().all { (key, value) ->
            val found = actual?.get(key) ?: return false
            val numeric = value.doubleOrNull
            if (numeric != null && !value.isString) found.doubleOrNull == numeric else found == value
        }
}
