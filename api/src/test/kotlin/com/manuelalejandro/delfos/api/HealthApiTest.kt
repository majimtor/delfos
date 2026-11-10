package com.manuelalejandro.delfos.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.json.JsonMapper

class HealthApiTest : ApiTest() {

    private val json = JsonMapper.builder().build()

    @Test
    fun `health answers UP as JSON`() {
        val response = client.get().uri("/api/v1/health").retrieve().toEntity(String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
        assertThat(json.readTree(response.body).get("status").asString()).isEqualTo("UP")
    }

    @Test
    fun `a manager operation without a session answers 401 as a problem`() {
        val response = client.get().uri("/api/v1/surveys").retrieve().toEntity(String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON)
        val body = json.readTree(response.body)
        assertThat(body.get("code").asString()).isEqualTo("UNAUTHENTICATED")
        assertThat(body.get("status").asInt()).isEqualTo(401)
        assertThat(body.get("type").asString()).isEqualTo("urn:delfos:problem:unauthenticated")
        assertThat(body.get("instance").asString()).isEqualTo("/api/v1/surveys")
    }

    @Test
    fun `an unknown public path answers 404 as a problem`() {
        val response = client.get().uri("/api/v1/public/nothing-here").retrieve().toEntity(String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON)
        assertThat(json.readTree(response.body).get("code").asString()).isEqualTo("NOT_FOUND")
    }

    @Test
    fun `liveness and readiness probes answer`() {
        for (probe in listOf("liveness", "readiness")) {
            val response = client.get().uri("/actuator/health/$probe").retrieve().toEntity(String::class.java)
            assertThat(response.statusCode).describedAs(probe).isEqualTo(HttpStatus.OK)
        }
    }
}
