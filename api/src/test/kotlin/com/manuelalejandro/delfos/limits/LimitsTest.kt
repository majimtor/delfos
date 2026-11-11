package com.manuelalejandro.delfos.limits

import com.manuelalejandro.delfos.api.ApiTest
import com.manuelalejandro.delfos.api.MutableClock
import java.time.Duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import tools.jackson.databind.json.JsonMapper

/** Its own application context, with limits low enough to hit in a test. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "DELFOS_DB_URL=set-by-testcontainers",
        "DELFOS_DB_USERNAME=set-by-testcontainers",
        "DELFOS_DB_PASSWORD=set-by-testcontainers",
        "delfos.limits.login-per-minute=3",
        "delfos.limits.public-per-minute=5",
        "delfos.limits.max-body-bytes=512",
    ],
)
class LimitsTest : ApiTest() {

    @Autowired lateinit var clock: MutableClock

    private val json = JsonMapper.builder().build()

    @AfterEach
    fun resetClock() = clock.reset()

    @Test
    fun `login attempts are throttled per client and the window passes`() {
        clock.advance(Duration.ofMinutes(5))
        repeat(3) { assertThat(login().statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) }
        val limited = login()
        assertThat(limited.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        assertThat(limited.headers.contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON)
        assertThat(json.readTree(limited.body).get("code").asString()).isEqualTo("RATE_LIMITED")
        assertThat(limited.headers.getFirst(HttpHeaders.RETRY_AFTER)!!.toLong()).isBetween(1L, 60L)

        clock.advance(Duration.ofMinutes(1))
        assertThat(login().statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `public operations are throttled per client`() {
        clock.advance(Duration.ofMinutes(10))
        repeat(5) { assertThat(publicGet().statusCode).isEqualTo(HttpStatus.NOT_FOUND) }
        assertThat(json.readTree(publicGet().body).get("code").asString()).isEqualTo("RATE_LIMITED")
    }

    @Test
    fun `public requests and login attempts have separate windows`() {
        clock.advance(Duration.ofMinutes(30))
        repeat(5) { assertThat(publicGet().statusCode).isEqualTo(HttpStatus.NOT_FOUND) }
        assertThat(publicGet().statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)

        assertThat(login().statusCode).describedAs("the public window must not count against the login").isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `a body above the limit is rejected before it is read`() {
        clock.advance(Duration.ofMinutes(20))
        val big = """{"username":"${"a".repeat(600)}","password":"x"}"""
        val response = client.post().uri("/api/v1/auth/sessions").contentType(MediaType.APPLICATION_JSON).body(big).retrieve().toEntity(String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONTENT_TOO_LARGE)
        assertThat(json.readTree(response.body).get("code").asString()).isEqualTo("PAYLOAD_TOO_LARGE")
    }

    private fun login(): ResponseEntity<String> =
        client.post().uri("/api/v1/auth/sessions").contentType(MediaType.APPLICATION_JSON)
            .body("""{"username":"nobody","password":"nope","transport":"BEARER"}""").retrieve().toEntity(String::class.java)

    private fun publicGet(): ResponseEntity<String> =
        client.get().uri("/api/v1/public/surveys/nothing-here-0").retrieve().toEntity(String::class.java)
}
