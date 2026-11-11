package com.manuelalejandro.delfos.respond

import com.manuelalejandro.delfos.api.ApiTest
import com.manuelalejandro.delfos.api.MutableClock
import com.manuelalejandro.delfos.auth.SessionAuthenticationFilter
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class PublicApiTest : ApiTest() {

    @Autowired lateinit var accounts: AccountRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var clock: MutableClock

    private val json = JsonMapper.builder().build()
    private lateinit var token: String

    @BeforeEach
    fun login() {
        token = tokenOf(newAccount())
    }

    @AfterEach
    fun resetClock() = clock.reset()

    @Test
    fun `a survey is public once published and shows whether it accepts responses`() {
        val (id, publicId) = createSurvey()
        assertCode(public(HttpMethod.GET, "/api/v1/public/surveys/$publicId"), HttpStatus.NOT_FOUND, "NOT_FOUND")
        assertCode(public(HttpMethod.GET, "/api/v1/public/surveys/unknown-id-00"), HttpStatus.NOT_FOUND, "NOT_FOUND")

        defineAndPublish(id)
        val shown = json.readTree(public(HttpMethod.GET, "/api/v1/public/surveys/$publicId").body)
        assertThat(shown.get("status").asString()).isEqualTo("PUBLISHED")
        assertThat(shown.get("acceptingResponses").asBoolean()).isTrue()
        assertThat(shown.get("version").get("number").asInt()).isEqualTo(1)
        assertThat(shown.get("version").get("questions").size()).isEqualTo(3)
        assertThat(shown.get("version").get("rules").size()).isEqualTo(1)
        assertThat(shown.has("id")).isFalse()

        manager(HttpMethod.POST, "/api/v1/surveys/$id/close")
        val closed = json.readTree(public(HttpMethod.GET, "/api/v1/public/surveys/$publicId").body)
        assertThat(closed.get("status").asString()).isEqualTo("CLOSED")
        assertThat(closed.get("acceptingResponses").asBoolean()).isFalse()
    }

    @Test
    fun `evaluation reports visibility, errors and missing required answers without storing anything`() {
        val (id, publicId) = createSurvey()
        defineAndPublish(id)

        val none = evaluate(publicId, 1, "[]")
        assertThat(none.get("visibleQuestionKeys").values().map { it.asString() }).containsExactly("owns-car", "comment")
        assertThat(none.get("missingRequiredQuestionKeys").values().map { it.asString() }).containsExactly("owns-car")

        val yes = evaluate(publicId, 1, """[{"questionKey":"owns-car","optionKey":"yes"}]""")
        assertThat(yes.get("visibleQuestionKeys").values().map { it.asString() }).containsExactly("owns-car", "fuel", "comment")
        assertThat(yes.get("missingRequiredQuestionKeys").size()).isZero()

        val hidden = evaluate(publicId, 1, """[{"questionKey":"owns-car","optionKey":"no"},{"questionKey":"fuel","optionKey":"petrol"}]""")
        assertThat(hidden.get("errors").get(0).get("code").asString()).isEqualTo("HIDDEN_QUESTION_ANSWERED")
        assertThat(hidden.get("errors").get(0).get("path").asString()).isEqualTo("answers[1]")

        assertCode(public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/evaluations", """{"versionNumber":99,"answers":[]}"""), HttpStatus.NOT_FOUND, "NOT_FOUND")
        manager(HttpMethod.POST, "/api/v1/surveys/$id/draft")
        assertCode(public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/evaluations", """{"versionNumber":2,"answers":[]}"""), HttpStatus.CONFLICT, "VERSION_NOT_ANSWERABLE")
        assertThat(jdbc.queryForObject("select count(*) from response r join survey_version v on v.id = r.version_id where v.survey_id = ?", Long::class.java, UUID.fromString(id))).isZero()
    }

    @Test
    fun `a valid response is stored with its answers and a superseded version still accepts them`() {
        val (id, publicId) = createSurvey()
        defineAndPublish(id)

        val submitted = public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses", """{"versionNumber":1,"answers":[
            {"questionKey":"owns-car","optionKey":"yes"},{"questionKey":"fuel","optionKey":"electric"},{"questionKey":"comment","text":"Quiet"}]}""")
        assertThat(submitted.statusCode).describedAs(submitted.body).isEqualTo(HttpStatus.CREATED)
        val responseId = json.readTree(submitted.body).get("responseId").asString()
        assertThat(jdbc.queryForObject("select count(*) from answer where response_id = ?", Long::class.java, UUID.fromString(responseId))).isEqualTo(3)
        assertThat(jdbc.queryForObject("select option_keys[1] from answer where response_id = ? and question_key = 'fuel'", String::class.java, UUID.fromString(responseId))).isEqualTo("electric")
        assertThat(json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id").body).get("responseCount").asInt()).isEqualTo(1)

        val incomplete = public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses", """{"versionNumber":1,"answers":[{"questionKey":"comment","text":"Only this"}]}""")
        assertCode(incomplete, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR")
        val error = json.readTree(incomplete.body).get("errors").get(0)
        assertThat(error.get("code").asString()).isEqualTo("MISSING_REQUIRED_ANSWER")
        assertThat(error.get("params").get("questionKey").asString()).isEqualTo("owns-car")

        manager(HttpMethod.POST, "/api/v1/surveys/$id/draft")
        manager(HttpMethod.POST, "/api/v1/surveys/$id/publish")
        val toSuperseded = public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses", """{"versionNumber":1,"answers":[{"questionKey":"owns-car","optionKey":"no"}]}""")
        assertThat(toSuperseded.statusCode).describedAs(toSuperseded.body).isEqualTo(HttpStatus.CREATED)
        assertThat(json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id").body).get("responseCount").asInt()).isEqualTo(2)
    }

    @Test
    fun `a survey closes by itself when its quota is reached or its closing time passes`() {
        val (id, publicId) = createSurvey()
        manager(HttpMethod.PATCH, "/api/v1/surveys/$id", """{"maxResponses":1}""")
        defineAndPublish(id)
        val one = """{"versionNumber":1,"answers":[{"questionKey":"owns-car","optionKey":"no"}]}"""
        assertThat(public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses", one).statusCode).isEqualTo(HttpStatus.CREATED)
        assertCode(public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses", one), HttpStatus.CONFLICT, "RESPONSE_QUOTA_REACHED")
        assertThat(json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id").body).get("status").asString()).isEqualTo("CLOSED")

        val (id2, publicId2) = createSurvey()
        manager(HttpMethod.PATCH, "/api/v1/surveys/$id2", """{"closesAt":"${Instant.now().plus(Duration.ofHours(1))}"}""")
        defineAndPublish(id2)
        assertThat(json.readTree(public(HttpMethod.GET, "/api/v1/public/surveys/$publicId2").body).get("acceptingResponses").asBoolean()).isTrue()
        clock.advance(Duration.ofHours(2))
        val shown = json.readTree(public(HttpMethod.GET, "/api/v1/public/surveys/$publicId2").body)
        assertThat(shown.get("acceptingResponses").asBoolean()).isFalse()
        assertThat(shown.get("status").asString()).isEqualTo("CLOSED")
        assertCode(public(HttpMethod.POST, "/api/v1/public/surveys/$publicId2/responses", one), HttpStatus.CONFLICT, "SURVEY_NOT_ACCEPTING_RESPONSES")
    }

    @Test
    fun `simultaneous submissions never exceed the quota`() {
        val (id, publicId) = createSurvey()
        manager(HttpMethod.PATCH, "/api/v1/surveys/$id", """{"maxResponses":3}""")
        defineAndPublish(id)
        val one = """{"versionNumber":1,"answers":[{"questionKey":"owns-car","optionKey":"no"}]}"""
        val attempts = 20
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(attempts)
        val responses = try {
            val futures = (1..attempts).map {
                pool.submit(Callable { start.await(); public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses", one) })
            }
            start.countDown()
            futures.map { it.get(60, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }

        assertThat(responses.count { it.statusCode == HttpStatus.CREATED }).isEqualTo(3)
        val rejected = responses.filter { it.statusCode != HttpStatus.CREATED }
        assertThat(rejected).hasSize(attempts - 3)
        assertThat(rejected.map { it.statusCode }.toSet()).containsExactly(HttpStatus.CONFLICT)
        assertThat(rejected.map { json.readTree(it.body).get("code").asString() }.toSet()).containsExactly("RESPONSE_QUOTA_REACHED")
        val survey = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id").body)
        assertThat(survey.get("status").asString()).isEqualTo("CLOSED")
        assertThat(survey.get("responseCount").asInt()).isEqualTo(3)
    }

    private fun evaluate(publicId: String, version: Int, answers: String): JsonNode {
        val response = public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/evaluations", """{"versionNumber":$version,"answers":$answers}""")
        assertThat(response.statusCode).describedAs(response.body).isEqualTo(HttpStatus.OK)
        return json.readTree(response.body)
    }

    private fun createSurvey(): Pair<String, String> {
        val response = manager(HttpMethod.POST, "/api/v1/surveys", """{"title":"Cars","language":"es"}""")
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = json.readTree(response.body)
        return body.get("id").asString() to body.get("publicId").asString()
    }

    private fun defineAndPublish(id: String) {
        assertThat(manager(HttpMethod.PUT, "/api/v1/surveys/$id/draft", DEFINITION).statusCode).isEqualTo(HttpStatus.OK)
        assertThat(manager(HttpMethod.POST, "/api/v1/surveys/$id/publish").statusCode).isEqualTo(HttpStatus.OK)
    }

    private fun assertCode(response: ResponseEntity<String>, status: HttpStatus, code: String) {
        assertThat(response.statusCode).describedAs(response.body).isEqualTo(status)
        assertThat(json.readTree(response.body).get("code").asString()).isEqualTo(code)
    }

    private fun public(method: HttpMethod, path: String, body: String? = null): ResponseEntity<String> {
        val request = client.method(method).uri(path)
        val withBody = if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body) else request
        return withBody.retrieve().toEntity(String::class.java)
    }

    private fun manager(method: HttpMethod, path: String, body: String? = null): ResponseEntity<String> {
        val request = client.method(method).uri(path).headers {
            it.setBearerAuth(token)
            it.set(SessionAuthenticationFilter.CLIENT_HEADER, "test")
        }
        val withBody = if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body) else request
        return withBody.retrieve().toEntity(String::class.java)
    }

    private fun newAccount(): Pair<String, String> {
        val username = "manager-${UUID.randomUUID().toString().take(8)}"
        val now = Instant.now()
        accounts.save(AccountEntity(UUID.randomUUID(), username, requireNotNull(passwordEncoder.encode("pw")), createdAt = now, updatedAt = now))
        return username to "pw"
    }

    private fun tokenOf(credentials: Pair<String, String>): String {
        val response = client.post().uri("/api/v1/auth/sessions").contentType(MediaType.APPLICATION_JSON)
            .body("""{"username":"${credentials.first}","password":"${credentials.second}","transport":"BEARER"}""")
            .retrieve().toEntity(String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        return json.readTree(response.body).get("token").asString()
    }

    private companion object {
        val DEFINITION = """
            {"questions":[
              {"key":"owns-car","type":"SINGLE_CHOICE","text":"Do you own a car?","required":true,"options":[{"key":"yes","label":"Yes"},{"key":"no","label":"No"}]},
              {"key":"fuel","type":"SINGLE_CHOICE","text":"Which fuel?","options":[{"key":"petrol","label":"Petrol"},{"key":"electric","label":"Electric"}]},
              {"key":"comment","type":"TEXT","text":"Any comment?","maxLength":200}
            ],
            "rules":[{"targetQuestionKey":"fuel","match":"ALL","conditions":[{"sourceQuestionKey":"owns-car","operator":"EQUALS","value":{"optionKey":"yes"}}]}]}
        """.trimIndent()
    }
}
