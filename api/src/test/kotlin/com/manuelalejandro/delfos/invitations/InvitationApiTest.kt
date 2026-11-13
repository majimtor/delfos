package com.manuelalejandro.delfos.invitations

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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class InvitationApiTest : ApiTest() {

    @Autowired lateinit var accounts: AccountRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    private val json = JsonMapper.builder().build()
    private lateinit var token: String
    private lateinit var id: String
    private lateinit var publicId: String

    @BeforeEach
    fun publishedInvitedSurvey() {
        token = tokenOf(newAccount())
        val created = json.readTree(manager(HttpMethod.POST, "/api/v1/surveys", """{"title":"By invitation","language":"en","respondentMode":"INVITED"}""").body)
        id = created.get("id").asString()
        publicId = created.get("publicId").asString()
        assertThat(manager(HttpMethod.PUT, "/api/v1/surveys/$id/draft", DEFINITION).statusCode).isEqualTo(HttpStatus.OK)
        assertThat(manager(HttpMethod.POST, "/api/v1/surveys/$id/publish").statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `an invited survey opens only with a pending invitation, which one response uses up`() {
        assertCode(public(HttpMethod.GET, "/api/v1/public/surveys/$publicId"), HttpStatus.FORBIDDEN, "INVITATION_REQUIRED")
        assertCode(public(HttpMethod.GET, "/api/v1/public/surveys/$publicId?invitation=nope"), HttpStatus.FORBIDDEN, "INVITATION_INVALID")

        val created = json.readTree(manager(HttpMethod.POST, "/api/v1/surveys/$id/invitations", """{"count":2}""").body)
        val tokens = created.get("items").values().map { it.get("token").asString() }
        assertThat(tokens).hasSize(2)
        assertThat(tokens).allMatch { it.length >= 20 }
        assertThat(tokens.toSet()).hasSize(2)
        val first = tokens[0]

        val shown = public(HttpMethod.GET, "/api/v1/public/surveys/$publicId?invitation=$first")
        assertThat(shown.statusCode).describedAs(shown.body).isEqualTo(HttpStatus.OK)
        assertThat(json.readTree(shown.body).get("respondentMode").asString()).isEqualTo("INVITED")
        val evaluated = public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/evaluations?invitation=$first", """{"versionNumber":1,"answers":[]}""")
        assertThat(evaluated.statusCode).isEqualTo(HttpStatus.OK)
        assertCode(public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/evaluations", """{"versionNumber":1,"answers":[]}"""), HttpStatus.FORBIDDEN, "INVITATION_REQUIRED")

        // A refused submission does not use the invitation up.
        val invalid = public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses?invitation=$first", """{"versionNumber":1,"answers":[]}""")
        assertThat(invalid.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses", ANSWERS).statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        val submitted = public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses?invitation=$first", ANSWERS)
        assertThat(submitted.statusCode).describedAs(submitted.body).isEqualTo(HttpStatus.CREATED)
        assertCode(public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses?invitation=$first", ANSWERS), HttpStatus.CONFLICT, "INVITATION_USED")
        assertCode(public(HttpMethod.GET, "/api/v1/public/surveys/$publicId?invitation=$first"), HttpStatus.CONFLICT, "INVITATION_USED")

        val listed = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id/invitations").body).get("items")
        assertThat(listed.size()).isEqualTo(2)
        assertThat(listed.values().map { it.get("status").asString() }).containsExactlyInAnyOrder("USED", "PENDING")
        assertThat(listed.values().first { it.get("status").asString() == "USED" }.has("usedAt")).isTrue()
        assertThat(listed.values().none { it.has("token") }).isTrue()
        assertThat(json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id").body).get("responseCount").asInt()).isEqualTo(1)
    }

    @Test
    fun `a revoked invitation no longer opens the survey and a used one cannot be revoked`() {
        val created = json.readTree(manager(HttpMethod.POST, "/api/v1/surveys/$id/invitations", """{"count":2}""").body).get("items")
        val pending = created.get(0)
        val used = created.get(1)
        assertThat(public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses?invitation=${used.get("token").asString()}", ANSWERS).statusCode).isEqualTo(HttpStatus.CREATED)

        assertThat(manager(HttpMethod.DELETE, "/api/v1/surveys/$id/invitations/${pending.get("id").asString()}").statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(manager(HttpMethod.DELETE, "/api/v1/surveys/$id/invitations/${pending.get("id").asString()}").statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertCode(public(HttpMethod.GET, "/api/v1/public/surveys/$publicId?invitation=${pending.get("token").asString()}"), HttpStatus.FORBIDDEN, "INVITATION_INVALID")
        assertCode(manager(HttpMethod.DELETE, "/api/v1/surveys/$id/invitations/${used.get("id").asString()}"), HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION")
        assertThat(manager(HttpMethod.DELETE, "/api/v1/surveys/$id/invitations/${UUID.randomUUID()}").statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        val statuses = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id/invitations").body).get("items").values().map { it.get("status").asString() }
        assertThat(statuses).containsExactlyInAnyOrder("REVOKED", "USED")
    }

    @Test
    fun `invitations are paged newest first and belong to their manager and mode`() {
        val created = json.readTree(manager(HttpMethod.POST, "/api/v1/surveys/$id/invitations", """{"count":3}""").body).get("items")
        assertThat(created.size()).isEqualTo(3)
        val page1 = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id/invitations?limit=2").body)
        assertThat(page1.get("items").size()).isEqualTo(2)
        val page2 = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id/invitations?limit=2&cursor=${page1.get("nextCursor").asString()}").body)
        assertThat(page2.get("items").size()).isEqualTo(1)
        assertThat(page2.get("nextCursor")).isNull()
        assertThat((page1.get("items").values() + page2.get("items").values()).map { it.get("id").asString() }.toSet()).hasSize(3)

        assertThat(manager(HttpMethod.POST, "/api/v1/surveys/$id/invitations", """{"count":201}""").statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        val other = tokenOf(newAccount())
        assertThat(manager(HttpMethod.GET, "/api/v1/surveys/$id/invitations", bearer = other).statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(manager(HttpMethod.POST, "/api/v1/surveys/$id/invitations", """{"count":1}""", bearer = other).statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        val anonymous = json.readTree(manager(HttpMethod.POST, "/api/v1/surveys", """{"title":"Open","language":"en"}""").body).get("id").asString()
        assertCode(manager(HttpMethod.POST, "/api/v1/surveys/$anonymous/invitations", """{"count":1}"""), HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION")
        // An anonymous survey ignores a token that travels with the request.
        assertThat(manager(HttpMethod.PUT, "/api/v1/surveys/$anonymous/draft", DEFINITION).statusCode).isEqualTo(HttpStatus.OK)
        assertThat(manager(HttpMethod.POST, "/api/v1/surveys/$anonymous/publish").statusCode).isEqualTo(HttpStatus.OK)
        val openId = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$anonymous").body).get("publicId").asString()
        assertThat(public(HttpMethod.GET, "/api/v1/public/surveys/$openId?invitation=whatever").statusCode).isEqualTo(HttpStatus.OK)

        assertThat(manager(HttpMethod.POST, "/api/v1/surveys/$id/close").statusCode).isEqualTo(HttpStatus.OK)
        assertCode(manager(HttpMethod.POST, "/api/v1/surveys/$id/invitations", """{"count":1}"""), HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION")
    }

    @Test
    fun `simultaneous submissions with one invitation leave exactly one response`() {
        val created = json.readTree(manager(HttpMethod.POST, "/api/v1/surveys/$id/invitations", """{"count":1}""").body).get("items").get(0).get("token").asString()
        val attempts = 8
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(attempts)
        try {
            val futures = (1..attempts).map {
                pool.submit(
                    Callable {
                        start.await(10, TimeUnit.SECONDS)
                        public(HttpMethod.POST, "/api/v1/public/surveys/$publicId/responses?invitation=$created", ANSWERS).statusCode
                    },
                )
            }
            start.countDown()
            val statuses = futures.map { it.get(30, TimeUnit.SECONDS) }
            assertThat(statuses.count { it == HttpStatus.CREATED }).isEqualTo(1)
            assertThat(statuses.count { it == HttpStatus.CONFLICT }).isEqualTo(attempts - 1)
        } finally {
            pool.shutdownNow()
        }
        assertThat(json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id").body).get("responseCount").asInt()).isEqualTo(1)
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

    private fun manager(method: HttpMethod, path: String, body: String? = null, bearer: String = token): ResponseEntity<String> {
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
        val DEFINITION = """
            {"questions":[
              {"key":"rating","type":"SCALE","text":"Rating","required":true,"max":5},
              {"key":"note","type":"TEXT","text":"Note","maxLength":100}
            ],"rules":[]}
        """.trimIndent()
        const val ANSWERS = """{"versionNumber":1,"answers":[{"questionKey":"rating","number":4}]}"""
    }
}
