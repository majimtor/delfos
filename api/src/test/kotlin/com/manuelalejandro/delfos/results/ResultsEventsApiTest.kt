package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.api.ApiTest
import com.manuelalejandro.delfos.auth.SessionAuthenticationFilter
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import java.io.BufferedReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
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
import tools.jackson.databind.json.JsonMapper

class ResultsEventsApiTest : ApiTest() {

    @Autowired lateinit var accounts: AccountRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var broker: ResultsEventBroker

    private val json = JsonMapper.builder().build()
    private lateinit var token: String
    private lateinit var id: String
    private lateinit var publicId: String

    @BeforeEach
    fun publishedSurvey() {
        token = tokenOf(newAccount())
        val created = json.readTree(manager(HttpMethod.POST, "/api/v1/surveys", """{"title":"Live","language":"en"}""").body)
        id = created.get("id").asString()
        publicId = created.get("publicId").asString()
        assertThat(manager(HttpMethod.PUT, "/api/v1/surveys/$id/draft", DEFINITION).statusCode).isEqualTo(HttpStatus.OK)
        assertThat(manager(HttpMethod.POST, "/api/v1/surveys/$id/publish").statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `the stream sends the totals on connection, again when a response arrives, and heartbeats in between`() {
        val lines = LinkedBlockingQueue<String>()
        val http = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port/api/v1/surveys/$id/results/events?heartbeat=1"))
            .header("Authorization", "Bearer $token").GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("text/event-stream")
        val reader = Thread {
            response.body().bufferedReader().use { r -> r.lineSequence().forEach { lines.put(it) } }
        }.apply { isDaemon = true; start() }
        try {
            val first = nextEvent(lines)
            assertThat(first).contains("event:results")
            assertThat(first).contains("\"responseCount\":0")
            assertThat(first).contains("\"lastSubmittedAt\":null")
            assertThat(broker.openStreams).isGreaterThanOrEqualTo(1)

            val submitted = client.post().uri("/api/v1/public/surveys/$publicId/responses").contentType(MediaType.APPLICATION_JSON)
                .body("""{"versionNumber":1,"answers":[{"questionKey":"rating","number":4}]}""").retrieve().toEntity(String::class.java)
            assertThat(submitted.statusCode).isEqualTo(HttpStatus.CREATED)
            val second = nextEvent(lines)
            assertThat(second).contains("\"responseCount\":1")
            assertThat(second).contains("\"lastSubmittedAt\":\"")

            val comments = generateSequence { lines.poll(5, TimeUnit.SECONDS) }.take(20).filter { it.startsWith(":") }.toList()
            assertThat(comments).isNotEmpty()
            assertThat(comments.first()).isEqualTo(":keep-alive")
        } finally {
            response.body().close()
            reader.interrupt()
        }
    }

    @Test
    fun `the stream is refused for another manager's survey and for an out-of-range heartbeat`() {
        val other = tokenOf(newAccount())
        assertThat(manager(HttpMethod.GET, "/api/v1/surveys/$id/results/events", bearer = other).statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(manager(HttpMethod.GET, "/api/v1/surveys/$id/results/events?heartbeat=61").statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(client.get().uri("/api/v1/surveys/$id/results/events").retrieve().toEntity(String::class.java).statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    /** The lines of the next event, joined; waits for the blank line that ends it. */
    private fun nextEvent(lines: LinkedBlockingQueue<String>): String {
        val event = mutableListOf<String>()
        val deadline = Instant.now().plus(Duration.ofSeconds(15))
        while (Instant.now().isBefore(deadline)) {
            val line = lines.poll(1, TimeUnit.SECONDS) ?: continue
            if (line.startsWith(":")) continue
            if (line.isEmpty()) {
                if (event.isNotEmpty()) return event.joinToString("\n")
                continue
            }
            event += line
        }
        error("no event within 15 seconds; got $event")
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
    private fun BufferedReader.drain(): List<String> = readLines()

    private companion object {
        val DEFINITION = """{"questions":[{"key":"rating","type":"SCALE","text":"Rating","required":true,"max":5}],"rules":[]}"""
    }
}
