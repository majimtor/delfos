package com.manuelalejandro.delfos.api

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.web.client.RestClient
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Base of the API tests: the whole application on a random port against a real PostgreSQL 18
 * in a container. The container is started once per JVM and shared by every test class, because
 * Spring caches the application context across classes and a container stopped after the first
 * class would leave that context without a database. Testcontainers removes it when the JVM ends.
 * The datasource placeholders are satisfied so the configuration loads; the container connection
 * then takes precedence through the service connection.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "DELFOS_DB_URL=set-by-testcontainers",
        "DELFOS_DB_USERNAME=set-by-testcontainers",
        "DELFOS_DB_PASSWORD=set-by-testcontainers",
        // Functional tests log in freely from one address; throttling has its own context in LimitsTest.
        "delfos.limits.login-per-minute=100000",
        "delfos.limits.public-per-minute=100000",
        // The demo reset exists in the shared context; DemoSeedTest exercises it.
        "delfos.seed.reset-token=test-reset-token",
    ],
)
@Import(TestClockConfig::class)
abstract class ApiTest {

    companion object {
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:18").also { it.start() }
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
