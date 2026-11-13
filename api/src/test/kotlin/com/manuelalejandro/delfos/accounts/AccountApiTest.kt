package com.manuelalejandro.delfos.accounts

import com.manuelalejandro.delfos.api.ApiTest
import com.manuelalejandro.delfos.auth.SessionAuthenticationFilter
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import java.time.Instant
import java.util.UUID
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

class AccountApiTest : ApiTest() {

    @Autowired lateinit var accounts: AccountRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    private val json = JsonMapper.builder().build()
    private lateinit var admin: String
    private lateinit var adminToken: String

    @BeforeEach
    fun loginAsAdmin() {
        admin = newAccount(isAdmin = true)
        adminToken = tokenOf(admin)
    }

    @Test
    fun `an administrator creates an account that can log in and sees only its own surveys`() {
        val username = "created-${UUID.randomUUID().toString().take(8)}"
        val created = request(HttpMethod.POST, "/api/v1/accounts", """{"username":"$username","password":"a-long-enough-password"}""")
        assertThat(created.statusCode).describedAs(created.body).isEqualTo(HttpStatus.CREATED)
        val body = json.readTree(created.body)
        assertThat(body.get("username").asString()).isEqualTo(username)
        assertThat(body.get("isAdmin").asBoolean()).isFalse()
        assertThat(body.get("status").asString()).isEqualTo("ACTIVE")
        assertThat(body.has("password")).isFalse()
        assertThat(body.has("passwordHash")).isFalse()

        val token = tokenOf(username, "a-long-enough-password")
        val surveys = json.readTree(request(HttpMethod.GET, "/api/v1/surveys", bearer = token).body)
        assertThat(surveys.get("items").size()).isEqualTo(0)
        assertCode(request(HttpMethod.GET, "/api/v1/accounts", bearer = token), HttpStatus.FORBIDDEN, "ADMIN_REQUIRED")
        assertCode(request(HttpMethod.POST, "/api/v1/accounts", """{"username":"x-$username","password":"a-long-enough-password"}""", bearer = token), HttpStatus.FORBIDDEN, "ADMIN_REQUIRED")
        assertCode(request(HttpMethod.POST, "/api/v1/accounts/${UUID.randomUUID()}/disable", bearer = token), HttpStatus.FORBIDDEN, "ADMIN_REQUIRED")

        assertCode(request(HttpMethod.POST, "/api/v1/accounts", """{"username":"$username","password":"a-long-enough-password"}"""), HttpStatus.CONFLICT, "USERNAME_TAKEN")
        val invalid = request(HttpMethod.POST, "/api/v1/accounts", """{"username":"Bad Name","password":"short"}""")
        assertThat(invalid.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(json.readTree(invalid.body).get("errors").values().map { it.get("path").asString() }).containsExactlyInAnyOrder("username", "password")

        val anotherAdmin = json.readTree(request(HttpMethod.POST, "/api/v1/accounts", """{"username":"adm-${UUID.randomUUID().toString().take(8)}","password":"a-long-enough-password","isAdmin":true}""").body)
        assertThat(anotherAdmin.get("isAdmin").asBoolean()).isTrue()
    }

    @Test
    fun `disabling an account ends its sessions, refuses its login and cannot touch the administrator's own account`() {
        val username = "created-${UUID.randomUUID().toString().take(8)}"
        val created = json.readTree(request(HttpMethod.POST, "/api/v1/accounts", """{"username":"$username","password":"a-long-enough-password"}""").body)
        val id = created.get("id").asString()
        val token = tokenOf(username, "a-long-enough-password")
        assertThat(request(HttpMethod.GET, "/api/v1/auth/me", bearer = token).statusCode).isEqualTo(HttpStatus.OK)

        val disabled = request(HttpMethod.POST, "/api/v1/accounts/$id/disable")
        assertThat(disabled.statusCode).describedAs(disabled.body).isEqualTo(HttpStatus.OK)
        assertThat(json.readTree(disabled.body).get("status").asString()).isEqualTo("DISABLED")
        assertThat(request(HttpMethod.GET, "/api/v1/auth/me", bearer = token).statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        val login = client.post().uri("/api/v1/auth/sessions").contentType(MediaType.APPLICATION_JSON)
            .body("""{"username":"$username","password":"a-long-enough-password","transport":"BEARER"}""").retrieve().toEntity(String::class.java)
        assertCode(login, HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED")
        assertThat(request(HttpMethod.POST, "/api/v1/accounts/$id/disable").statusCode).isEqualTo(HttpStatus.OK)

        val me = json.readTree(request(HttpMethod.GET, "/api/v1/auth/me").body).get("accountId").asString()
        assertCode(request(HttpMethod.POST, "/api/v1/accounts/$me/disable"), HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION")
        assertThat(request(HttpMethod.POST, "/api/v1/accounts/${UUID.randomUUID()}/disable").statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `accounts are listed newest first with cursors`() {
        val names = (1..3).map { "listed-$it-${UUID.randomUUID().toString().take(6)}" }
        names.forEach { assertThat(request(HttpMethod.POST, "/api/v1/accounts", """{"username":"$it","password":"a-long-enough-password"}""").statusCode).isEqualTo(HttpStatus.CREATED) }
        val page1 = json.readTree(request(HttpMethod.GET, "/api/v1/accounts?limit=2").body)
        assertThat(page1.get("items").size()).isEqualTo(2)
        assertThat(page1.get("items").values().map { it.get("username").asString() }).containsExactly(names[2], names[1])
        val page2 = json.readTree(request(HttpMethod.GET, "/api/v1/accounts?limit=2&cursor=${page1.get("nextCursor").asString()}").body)
        assertThat(page2.get("items").values().map { it.get("username").asString() }).startsWith(names[0])
    }

    private fun assertCode(response: ResponseEntity<String>, status: HttpStatus, code: String) {
        assertThat(response.statusCode).describedAs(response.body).isEqualTo(status)
        assertThat(json.readTree(response.body).get("code").asString()).isEqualTo(code)
    }

    private fun request(method: HttpMethod, path: String, body: String? = null, bearer: String = adminToken): ResponseEntity<String> {
        val request = client.method(method).uri(path).headers {
            it.setBearerAuth(bearer)
            it.set(SessionAuthenticationFilter.CLIENT_HEADER, "test")
        }
        val withBody = if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body) else request
        return withBody.retrieve().toEntity(String::class.java)
    }

    private fun newAccount(isAdmin: Boolean): String {
        val username = "manager-${UUID.randomUUID().toString().take(8)}"
        val now = Instant.now()
        accounts.save(AccountEntity(UUID.randomUUID(), username, requireNotNull(passwordEncoder.encode("pw")), isAdmin = isAdmin, createdAt = now, updatedAt = now))
        return username
    }

    private fun tokenOf(username: String, password: String = "pw"): String {
        val response = client.post().uri("/api/v1/auth/sessions").contentType(MediaType.APPLICATION_JSON)
            .body("""{"username":"$username","password":"$password","transport":"BEARER"}""")
            .retrieve().toEntity(String::class.java)
        assertThat(response.statusCode).describedAs(response.body).isEqualTo(HttpStatus.CREATED)
        return json.readTree(response.body).get("token").asString()
    }
}
