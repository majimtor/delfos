package com.manuelalejandro.delfos.survey

import com.manuelalejandro.delfos.api.ApiTest
import com.manuelalejandro.delfos.auth.SessionAuthenticationFilter
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class SurveyApiTest : ApiTest() {

    @Autowired lateinit var accounts: AccountRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    private val json = JsonMapper.builder().build()
    private lateinit var token: String

    @BeforeEach
    fun login() {
        token = tokenOf(newAccount())
    }

    @Test
    fun `a new survey is a draft with an empty first version`() {
        val created = call(HttpMethod.POST, "/api/v1/surveys", """{"title":"Transport habits","language":"es","description":"About commuting"}""")
        assertThat(created.statusCode).isEqualTo(HttpStatus.CREATED)
        val survey = json.readTree(created.body)
        val id = survey.get("id").asString()
        assertThat(created.headers.getFirst(HttpHeaders.LOCATION)).isEqualTo("/api/v1/surveys/$id")
        assertThat(survey.get("status").asString()).isEqualTo("DRAFT")
        assertThat(survey.get("hasDraft").asBoolean()).isTrue()
        assertThat(survey.get("publishedVersionNumber")).isNull()
        assertThat(survey.get("responseCount").asInt()).isZero()
        assertThat(survey.get("publicId").asString()).hasSize(12)
        assertThat(survey.get("respondentMode").asString()).isEqualTo("ANONYMOUS")

        val draft = json.readTree(call(HttpMethod.GET, "/api/v1/surveys/$id/draft").body)
        assertThat(draft.get("number").asInt()).isEqualTo(1)
        assertThat(draft.get("status").asString()).isEqualTo("DRAFT")
        assertThat(draft.get("questions").size()).isZero()
        assertThat(draft.get("rules").size()).isZero()
    }

    @Test
    fun `surveys are listed by owner, most recently updated first, with a working cursor`() {
        val ids = (1..3).map { createSurvey("Survey $it") }
        val other = tokenOf(newAccount())
        createSurveyAs(other, "Someone else's")

        val first = json.readTree(call(HttpMethod.GET, "/api/v1/surveys?limit=2").body)
        assertThat(first.get("items").values().map { it.get("id").asString() }).containsExactly(ids[2], ids[1])
        val cursor = first.get("nextCursor").asString()
        assertThat(cursor).isNotBlank()

        val second = json.readTree(call(HttpMethod.GET, "/api/v1/surveys?limit=2&cursor=$cursor").body)
        assertThat(second.get("items").values().map { it.get("id").asString() }).containsExactly(ids[0])
        assertThat(second.get("nextCursor")).isNull()

        val published = json.readTree(call(HttpMethod.GET, "/api/v1/surveys?status=PUBLISHED").body)
        assertThat(published.get("items").size()).isZero()

        val badCursor = call(HttpMethod.GET, "/api/v1/surveys?cursor=not-a-cursor")
        assertThat(badCursor.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(json.readTree(badCursor.body).get("errors").get(0).get("path").asString()).isEqualTo("cursor")
    }

    @Test
    fun `another manager's survey does not exist for me`() {
        val other = tokenOf(newAccount())
        val theirs = createSurveyAs(other, "Theirs")
        for (path in listOf("/api/v1/surveys/$theirs", "/api/v1/surveys/$theirs/draft", "/api/v1/surveys/$theirs/versions")) {
            val response = call(HttpMethod.GET, path)
            assertThat(response.statusCode).describedAs(path).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(json.readTree(response.body).get("code").asString()).isEqualTo("NOT_FOUND")
        }
        assertThat(call(HttpMethod.DELETE, "/api/v1/surveys/$theirs").statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `patch changes only the fields present and null clears a field`() {
        val id = createSurvey("Before", description = "Keep or clear")
        val patched = json.readTree(call(HttpMethod.PATCH, "/api/v1/surveys/$id", """{"title":"After","maxResponses":10}""").body)
        assertThat(patched.get("title").asString()).isEqualTo("After")
        assertThat(patched.get("description").asString()).isEqualTo("Keep or clear")
        assertThat(patched.get("maxResponses").asInt()).isEqualTo(10)

        val cleared = json.readTree(call(HttpMethod.PATCH, "/api/v1/surveys/$id", """{"description":null,"closesAt":"2030-01-01T00:00:00Z"}""").body)
        assertThat(cleared.get("description")).isNull()
        assertThat(cleared.get("title").asString()).isEqualTo("After")
        assertThat(cleared.get("closesAt").asString()).startsWith("2030-01-01T00:00")

        val invalid = call(HttpMethod.PATCH, "/api/v1/surveys/$id", """{"maxResponses":0}""")
        assertThat(invalid.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(json.readTree(invalid.body).get("code").asString()).isEqualTo("VALIDATION_ERROR")
    }

    @Test
    fun `the draft is validated as a whole and keys are assigned to new questions and options`() {
        val id = createSurvey("Definition")
        val saved = call(HttpMethod.PUT, "/api/v1/surveys/$id/draft", VALID_DEFINITION)
        assertThat(saved.statusCode).describedAs(saved.body).isEqualTo(HttpStatus.OK)
        val version = json.readTree(saved.body)
        val questions = version.get("questions")
        assertThat(questions.size()).isEqualTo(4)
        assertThat(questions.get(0).get("key").asString()).isEqualTo("owns-car")
        assertThat(questions.get(1).get("key").asString()).isEqualTo("fuel")
        assertThat(questions.get(3).get("key").asString()).startsWith("q-")
        assertThat(questions.get(0).get("options").get(0).get("key").asString()).isEqualTo("yes")
        assertThat(questions.get(0).get("options").get(1).get("key").asString()).startsWith("o-")
        assertThat(questions.get(2).get("maxLength").asInt()).isEqualTo(200)
        assertThat(questions.get(2).has("multiline")).isFalse()
        assertThat(version.get("rules").get(0).get("targetQuestionKey").asString()).isEqualTo("fuel")

        val reread = json.readTree(call(HttpMethod.GET, "/api/v1/surveys/$id/versions/1").body)
        assertThat(reread).isEqualTo(version)

        val forward = call(
            HttpMethod.PUT,
            "/api/v1/surveys/$id/draft",
            """{"questions":[{"key":"a","type":"TEXT","text":"A"},{"key":"b","type":"NUMBER","text":"B"}],
                "rules":[{"targetQuestionKey":"a","match":"ALL","conditions":[{"sourceQuestionKey":"b","operator":"GT","value":{"number":1}}]}]}""",
        )
        assertThat(forward.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val error = json.readTree(forward.body).get("errors").get(0)
        assertThat(error.get("path").asString()).isEqualTo("rules[0].conditions[0].sourceQuestionKey")
        assertThat(error.get("code").asString()).isEqualTo("FORWARD_REFERENCE")
        assertThat(error.get("params").get("sourcePosition").asInt()).isEqualTo(2)

        val scaleWithoutMax = call(HttpMethod.PUT, "/api/v1/surveys/$id/draft", """{"questions":[{"type":"SCALE","text":"Rate"}],"rules":[]}""")
        assertThat(json.readTree(scaleWithoutMax.body).get("errors").get(0).get("path").asString()).isEqualTo("questions[0].max")

        // A rejected definition leaves the saved one untouched.
        assertThat(json.readTree(call(HttpMethod.GET, "/api/v1/surveys/$id/draft").body)).isEqualTo(version)
    }

    @Test
    fun `publishing, drafting again, publishing again and closing follow the lifecycle`() {
        val id = createSurvey("Lifecycle")

        val empty = call(HttpMethod.POST, "/api/v1/surveys/$id/publish")
        assertThat(empty.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(json.readTree(empty.body).get("errors").get(0).get("code").asString()).isEqualTo("NO_QUESTIONS")

        call(HttpMethod.PUT, "/api/v1/surveys/$id/draft", VALID_DEFINITION).also { assertThat(it.statusCode).isEqualTo(HttpStatus.OK) }
        val published = json.readTree(call(HttpMethod.POST, "/api/v1/surveys/$id/publish").body)
        assertThat(published.get("status").asString()).isEqualTo("PUBLISHED")
        assertThat(published.get("publishedVersionNumber").asInt()).isEqualTo(1)
        assertThat(published.get("hasDraft").asBoolean()).isFalse()

        assertCode(call(HttpMethod.POST, "/api/v1/surveys/$id/publish"), HttpStatus.CONFLICT, "NO_DRAFT")
        assertCode(call(HttpMethod.GET, "/api/v1/surveys/$id/draft"), HttpStatus.NOT_FOUND, "NO_DRAFT")

        val draft2 = call(HttpMethod.POST, "/api/v1/surveys/$id/draft")
        assertThat(draft2.statusCode).isEqualTo(HttpStatus.CREATED)
        val v2 = json.readTree(draft2.body)
        assertThat(v2.get("number").asInt()).isEqualTo(2)
        assertThat(v2.get("questions").values().map { it.get("key").asString() }).contains("owns-car", "fuel")
        assertCode(call(HttpMethod.POST, "/api/v1/surveys/$id/draft"), HttpStatus.CONFLICT, "DRAFT_ALREADY_EXISTS")

        val v2Published = json.readTree(call(HttpMethod.POST, "/api/v1/surveys/$id/publish").body)
        assertThat(v2Published.get("publishedVersionNumber").asInt()).isEqualTo(2)
        val versions = json.readTree(call(HttpMethod.GET, "/api/v1/surveys/$id/versions").body).get("items")
        assertThat(versions.values().map { it.get("number").asInt() }).containsExactly(2, 1)
        assertThat(versions.values().map { it.get("status").asString() }).containsExactly("PUBLISHED", "SUPERSEDED")
        assertThat(versions.get(0).get("questionCount").asInt()).isEqualTo(4)

        val closed = json.readTree(call(HttpMethod.POST, "/api/v1/surveys/$id/close").body)
        assertThat(closed.get("status").asString()).isEqualTo("CLOSED")
        assertCode(call(HttpMethod.POST, "/api/v1/surveys/$id/close"), HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION")
        assertCode(call(HttpMethod.POST, "/api/v1/surveys/$id/publish"), HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION")
        assertCode(call(HttpMethod.POST, "/api/v1/surveys/$id/draft"), HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION")
    }

    @Test
    fun `simultaneous draft creations yield one draft and one conflict, never an error`() {
        val id = createSurvey("Race")
        call(HttpMethod.PUT, "/api/v1/surveys/$id/draft", VALID_DEFINITION)
        assertThat(call(HttpMethod.POST, "/api/v1/surveys/$id/publish").statusCode).isEqualTo(HttpStatus.OK)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val responses = try {
            val futures = (1..2).map { pool.submit(Callable { start.await(); call(HttpMethod.POST, "/api/v1/surveys/$id/draft") }) }
            start.countDown()
            futures.map { it.get(60, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }

        assertThat(responses.map { it.statusCode }).containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT)
        val conflict = responses.first { it.statusCode == HttpStatus.CONFLICT }
        assertThat(json.readTree(conflict.body).get("code").asString()).isEqualTo("DRAFT_ALREADY_EXISTS")
    }

    @Test
    fun `deleting a survey removes it with its versions`() {
        val id = createSurvey("Gone")
        call(HttpMethod.PUT, "/api/v1/surveys/$id/draft", VALID_DEFINITION)
        assertThat(call(HttpMethod.DELETE, "/api/v1/surveys/$id").statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertCode(call(HttpMethod.GET, "/api/v1/surveys/$id"), HttpStatus.NOT_FOUND, "NOT_FOUND")
        assertCode(call(HttpMethod.GET, "/api/v1/surveys/$id/versions/1"), HttpStatus.NOT_FOUND, "NOT_FOUND")
    }

    private fun assertCode(response: ResponseEntity<String>, status: HttpStatus, code: String) {
        assertThat(response.statusCode).describedAs(response.body).isEqualTo(status)
        assertThat(json.readTree(response.body).get("code").asString()).isEqualTo(code)
    }

    private fun createSurvey(title: String, description: String? = null): String = createSurveyAs(token, title, description)

    private fun createSurveyAs(bearer: String, title: String, description: String? = null): String {
        val body = buildString {
            append("""{"title":"$title","language":"es"""")
            if (description != null) append(""","description":"$description"""")
            append("}")
        }
        val response = call(HttpMethod.POST, "/api/v1/surveys", body, bearer)
        assertThat(response.statusCode).describedAs(response.body).isEqualTo(HttpStatus.CREATED)
        return json.readTree(response.body).get("id").asString()
    }

    private fun call(method: HttpMethod, path: String, body: String? = null, bearer: String = token): ResponseEntity<String> {
        val request = client.method(method).uri(path).headers {
            it.setBearerAuth(bearer)
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

    @Suppress("unused")
    private fun JsonNode.text(field: String): String = get(field).asString()

    private companion object {
        val VALID_DEFINITION = """
            {"questions":[
              {"key":"owns-car","type":"SINGLE_CHOICE","text":"Do you own a car?","required":true,"options":[{"key":"yes","label":"Yes"},{"label":"No"}]},
              {"type":"SINGLE_CHOICE","text":"Which fuel?","options":[{"key":"petrol","label":"Petrol"},{"key":"electric","label":"Electric"}]},
              {"key":"comment","type":"TEXT","text":"Any comment?","maxLength":200},
              {"type":"TEXT","text":"Anything else?"}
            ],
            "rules":[{"targetQuestionKey":"fuel","match":"ALL","conditions":[{"sourceQuestionKey":"owns-car","operator":"EQUALS","value":{"optionKey":"yes"}}]}]}
        """.trimIndent().replace("\"type\":\"SINGLE_CHOICE\",\"text\":\"Which fuel?\"", "\"key\":\"fuel\",\"type\":\"SINGLE_CHOICE\",\"text\":\"Which fuel?\"")
    }
}
