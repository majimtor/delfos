package com.manuelalejandro.delfos.auth

import com.manuelalejandro.delfos.api.ApiTest
import com.manuelalejandro.delfos.api.MutableClock
import com.manuelalejandro.delfos.domain.AccountStatus
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class AuthApiTest : ApiTest() {

    @Autowired lateinit var accounts: AccountRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var clock: MutableClock

    private val json = JsonMapper.builder().build()

    @AfterEach
    fun resetClock() = clock.reset()

    @Test
    fun `bearer login returns the token once and the session works until logout`() {
        val username = newAccount("secret-1")

        val login = post("/api/v1/auth/sessions", """{"username":"$username","password":"secret-1","transport":"BEARER"}""")
        assertThat(login.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = json.readTree(login.body)
        val token = body.get("token").asString()
        assertThat(token).hasSizeGreaterThanOrEqualTo(43)
        assertThat(body.get("username").asString()).isEqualTo(username)
        assertThat(body.get("isAdmin").asBoolean()).isFalse()
        assertThat(login.headers[HttpHeaders.SET_COOKIE]).isNull()

        val me = get("/api/v1/auth/me", bearer = token)
        assertThat(me.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(json.readTree(me.body).get("username").asString()).isEqualTo(username)

        val logout = delete("/api/v1/auth/sessions/current", bearer = token)
        assertThat(logout.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val afterLogout = get("/api/v1/auth/me", bearer = token)
        assertThat(afterLogout.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(json.readTree(afterLogout.body).get("code").asString()).isEqualTo("UNAUTHENTICATED")
    }

    @Test
    fun `cookie login sets a hardened cookie, hides the token and enforces the client header`() {
        val username = newAccount("secret-2")

        val withoutHeader = post("/api/v1/auth/sessions", """{"username":"$username","password":"secret-2"}""")
        assertThat(withoutHeader.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(json.readTree(withoutHeader.body).get("code").asString()).isEqualTo("CSRF_REJECTED")

        val login = post("/api/v1/auth/sessions", """{"username":"$username","password":"secret-2"}""", clientHeader = true)
        assertThat(login.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(json.readTree(login.body).get("token")).isNull()
        val setCookie = login.headers.getFirst(HttpHeaders.SET_COOKIE)!!
        assertThat(setCookie).startsWith("__Host-session=").contains("HttpOnly").contains("Secure").contains("SameSite=Strict").contains("Path=/")
        val cookie = setCookie.substringBefore(';')

        val me = get("/api/v1/auth/me", cookie = cookie)
        assertThat(me.statusCode).isEqualTo(HttpStatus.OK)

        val mutatingWithoutHeader = delete("/api/v1/auth/sessions/current", cookie = cookie)
        assertThat(mutatingWithoutHeader.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(json.readTree(mutatingWithoutHeader.body).get("code").asString()).isEqualTo("CSRF_REJECTED")

        val logout = delete("/api/v1/auth/sessions/current", cookie = cookie, clientHeader = true)
        assertThat(logout.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(logout.headers.getFirst(HttpHeaders.SET_COOKIE)).startsWith("__Host-session=;").contains("Max-Age=0")
        assertThat(get("/api/v1/auth/me", cookie = cookie).statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `wrong password, unknown user and disabled account answer 401 with their codes`() {
        val username = newAccount("secret-3")
        val wrong = post("/api/v1/auth/sessions", """{"username":"$username","password":"nope","transport":"BEARER"}""")
        assertThat(wrong.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(json.readTree(wrong.body).get("code").asString()).isEqualTo("INVALID_CREDENTIALS")

        val unknown = post("/api/v1/auth/sessions", """{"username":"nobody-${UUID.randomUUID()}","password":"nope","transport":"BEARER"}""")
        assertThat(unknown.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(json.readTree(unknown.body).get("code").asString()).isEqualTo("INVALID_CREDENTIALS")

        val disabledName = newAccount("secret-4", status = AccountStatus.DISABLED)
        val disabled = post("/api/v1/auth/sessions", """{"username":"$disabledName","password":"secret-4","transport":"BEARER"}""")
        assertThat(disabled.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(json.readTree(disabled.body).get("code").asString()).isEqualTo("ACCOUNT_DISABLED")
    }

    @Test
    fun `a missing required field, an unknown field and a wrong type are field errors`() {
        val missing = post("/api/v1/auth/sessions", """{"username":"someone"}""")
        assertFieldError(missing, "password", "REQUIRED")

        val unknown = post("/api/v1/auth/sessions", """{"username":"someone","password":"x","remember":true}""")
        assertFieldError(unknown, "remember", "UNKNOWN_FIELD")

        val wrongType = post("/api/v1/auth/sessions", """{"username":"someone","password":"x","transport":"CARRIER_PIGEON"}""")
        assertFieldError(wrongType, "transport", "INVALID_VALUE")

        val tooLong = post("/api/v1/auth/sessions", """{"username":"${"a".repeat(65)}","password":"x","transport":"BEARER"}""")
        assertFieldError(tooLong, "username", "TOO_LONG")
    }

    @Test
    fun `a body that is not JSON is malformed`() {
        val response = post("/api/v1/auth/sessions", "{not json")
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(json.readTree(response.body).get("code").asString()).isEqualTo("MALFORMED_REQUEST")
    }

    private fun assertFieldError(response: ResponseEntity<String>, path: String, code: String) {
        assertThat(response.statusCode).describedAs(path).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON)
        val body = json.readTree(response.body)
        assertThat(body.get("code").asString()).describedAs(path).isEqualTo("VALIDATION_ERROR")
        val errors = body.get("errors")
        assertThat(errors.size()).describedAs(response.body).isEqualTo(1)
        assertThat(errors.get(0).get("path").asString()).isEqualTo(path)
        assertThat(errors.get(0).get("code").asString()).describedAs(path).isEqualTo(code)
    }

    @Test
    fun `a session expires after thirty idle minutes and after twelve hours in any case`() {
        val username = newAccount("secret-5")
        val idleToken = token(username, "secret-5")
        clock.advance(Duration.ofMinutes(29))
        assertThat(get("/api/v1/auth/me", bearer = idleToken).statusCode).isEqualTo(HttpStatus.OK)
        clock.advance(Duration.ofMinutes(31))
        assertThat(get("/api/v1/auth/me", bearer = idleToken).statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)

        // Twenty-four requests 29 minutes apart keep the session alive for 11.6 hours; the next crosses 12 hours.
        val activeToken = token(username, "secret-5")
        repeat(24) {
            clock.advance(Duration.ofMinutes(29))
            assertThat(get("/api/v1/auth/me", bearer = activeToken).statusCode).describedAs("request $it").isEqualTo(HttpStatus.OK)
        }
        clock.advance(Duration.ofMinutes(29))
        assertThat(get("/api/v1/auth/me", bearer = activeToken).statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    private fun newAccount(password: String, status: AccountStatus = AccountStatus.ACTIVE): String {
        val username = "manager-${UUID.randomUUID().toString().take(8)}"
        val now = Instant.now()
        accounts.save(AccountEntity(UUID.randomUUID(), username, requireNotNull(passwordEncoder.encode(password)), status = status, createdAt = now, updatedAt = now))
        return username
    }

    private fun token(username: String, password: String): String {
        val login = post("/api/v1/auth/sessions", """{"username":"$username","password":"$password","transport":"BEARER"}""")
        assertThat(login.statusCode).isEqualTo(HttpStatus.CREATED)
        return json.readTree(login.body).get("token").asString()
    }

    private fun post(path: String, body: String, clientHeader: Boolean = false): ResponseEntity<String> =
        client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
            .headers { if (clientHeader) it.set(SessionAuthenticationFilter.CLIENT_HEADER, "test") }
            .retrieve().toEntity(String::class.java)

    private fun get(path: String, bearer: String? = null, cookie: String? = null): ResponseEntity<String> =
        client.get().uri(path).headers { auth(it, bearer, cookie) }.retrieve().toEntity(String::class.java)

    private fun delete(path: String, bearer: String? = null, cookie: String? = null, clientHeader: Boolean = false): ResponseEntity<String> =
        client.delete().uri(path).headers {
            auth(it, bearer, cookie)
            if (clientHeader) it.set(SessionAuthenticationFilter.CLIENT_HEADER, "test")
        }.retrieve().toEntity(String::class.java)

    private fun auth(headers: HttpHeaders, bearer: String?, cookie: String?) {
        bearer?.let { headers.setBearerAuth(it) }
        cookie?.let { headers.set(HttpHeaders.COOKIE, it) }
    }

    @Suppress("unused")
    private fun JsonNode.text(field: String): String = get(field).asString()
}
