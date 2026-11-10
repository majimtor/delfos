package com.manuelalejandro.delfos.api

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.web.client.RestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Base of the API tests: the whole application on a random port against a real PostgreSQL 18
 * in a container. The datasource placeholders are satisfied so the configuration loads; the
 * container connection then takes precedence through the service connection.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "DELFOS_DB_URL=set-by-testcontainers",
        "DELFOS_DB_USERNAME=set-by-testcontainers",
        "DELFOS_DB_PASSWORD=set-by-testcontainers",
    ],
)
@Testcontainers
abstract class ApiTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:18")
    }

    @LocalServerPort
    protected var port: Int = 0

    /** A client that returns every status instead of throwing, so tests assert on the response. */
    protected val client: RestClient by lazy {
        RestClient.builder()
            .baseUrl("http://localhost:$port")
            .defaultStatusHandler({ true }) { _, _ -> }
            .build()
    }
}
