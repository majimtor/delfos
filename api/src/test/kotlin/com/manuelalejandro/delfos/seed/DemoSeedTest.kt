package com.manuelalejandro.delfos.seed

import com.manuelalejandro.delfos.api.ApiTest
import com.manuelalejandro.delfos.auth.SessionAuthenticationFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import tools.jackson.databind.json.JsonMapper

class DemoSeedTest : ApiTest() {

    @Autowired lateinit var seed: DemoSeed

    private val json = JsonMapper.builder().build()

    @Test
    fun `the seed creates the demo accounts and surveys and can be applied again`() {
        seed.seed()
        seed.seed()

        val manager = tokenOf(DemoSeed.MANAGER)
        val surveys = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys", manager).body).get("items").values().associateBy { it.get("title").asString() }
        assertThat(surveys.keys).containsExactlyInAnyOrder("Hábitos de lectura", "Movilidad urbana", "Satisfacción con el servicio", "Encuentro anual")
        assertThat(surveys.getValue("Hábitos de lectura").get("status").asString()).isEqualTo("DRAFT")
        assertThat(surveys.getValue("Movilidad urbana").get("responseCount").asInt()).isEqualTo(40)
        assertThat(surveys.getValue("Satisfacción con el servicio").get("responseCount").asInt()).isEqualTo(25)
        assertThat(surveys.getValue("Satisfacción con el servicio").get("publishedVersionNumber").asInt()).isEqualTo(2)
        assertThat(surveys.getValue("Encuentro anual").get("status").asString()).isEqualTo("CLOSED")

        val mobilityId = surveys.getValue("Movilidad urbana").get("id").asString()
        val results = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$mobilityId/versions/1/results", manager).body)
        assertThat(results.get("responseCount").asInt()).isEqualTo(40)
        assertThat(results.get("questions").values().map { it.get("answerCount").asInt() }).allMatch { it > 0 }

        val serviceId = surveys.getValue("Satisfacción con el servicio").get("id").asString()
        val comparable = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$serviceId/results?scope=comparable", manager).body)
        assertThat(comparable.get("versionNumbers").values().map { it.asInt() }).containsExactly(1, 2)
        assertThat(comparable.get("questions").values().first { it.get("key").asString() == "canal" }.get("comparable").asBoolean()).isFalse()

        val public = client.get().uri("/api/v1/public/surveys/demo-movilidad").retrieve().toEntity(String::class.java)
        assertThat(public.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(json.readTree(public.body).get("acceptingResponses").asBoolean()).isTrue()

        val other = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys", tokenOf(DemoSeed.MANAGER_2)).body).get("items")
        assertThat(other.size()).isEqualTo(1)
        assertThat(json.readTree(manager(HttpMethod.GET, "/api/v1/auth/me", tokenOf(DemoSeed.ADMIN)).body).get("isAdmin").asBoolean()).isTrue()
    }

    @Test
    fun `the reset operation needs the configured token`() {
        val missing = client.post().uri("/api/v1/demo/reset").retrieve().toEntity(String::class.java)
        assertThat(missing.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(missing.headers.contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON)
        assertThat(json.readTree(missing.body).get("code").asString()).isEqualTo("UNAUTHENTICATED")

        val wrong = client.post().uri("/api/v1/demo/reset").headers { it.setBearerAuth("not-the-token") }.retrieve().toEntity(String::class.java)
        assertThat(wrong.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `the reset operation removes what the demo accounts created and restores the samples`() {
        seed.seed()
        val manager = tokenOf(DemoSeed.MANAGER)
        val created = client.post().uri("/api/v1/surveys").contentType(MediaType.APPLICATION_JSON)
            .headers { it.setBearerAuth(manager); it.set(SessionAuthenticationFilter.CLIENT_HEADER, "test") }
            .body("""{"title":"Left by a visitor","language":"es"}""").retrieve().toEntity(String::class.java)
        assertThat(created.statusCode).describedAs(created.body).isEqualTo(HttpStatus.CREATED)

        val reset = client.post().uri("/api/v1/demo/reset").headers { it.setBearerAuth("test-reset-token") }.retrieve().toEntity(String::class.java)
        assertThat(reset.statusCode).describedAs(reset.body).isEqualTo(HttpStatus.NO_CONTENT)

        val titles = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys", manager).body).get("items").values().map { it.get("title").asString() }
        assertThat(titles).containsExactlyInAnyOrder("Hábitos de lectura", "Movilidad urbana", "Satisfacción con el servicio", "Encuentro anual")
    }

    private fun manager(method: HttpMethod, path: String, bearer: String): ResponseEntity<String> =
        client.method(method).uri(path).headers {
            it.setBearerAuth(bearer)
            it.set(SessionAuthenticationFilter.CLIENT_HEADER, "test")
        }.retrieve().toEntity(String::class.java)

    private fun tokenOf(username: String): String {
        val response = client.post().uri("/api/v1/auth/sessions").contentType(MediaType.APPLICATION_JSON)
            .body("""{"username":"$username","password":"${DemoSeed.PASSWORD}","transport":"BEARER"}""")
            .retrieve().toEntity(String::class.java)
        assertThat(response.statusCode).describedAs(response.body).isEqualTo(HttpStatus.CREATED)
        return json.readTree(response.body).get("token").asString()
    }
}
