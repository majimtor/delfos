package com.manuelalejandro.delfos.api

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/** A clock the tests can move forward to exercise expiry rules. */
class MutableClock(@Volatile private var now: Instant = Instant.now()) : Clock() {

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }

    fun reset() {
        now = Instant.now()
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}

@TestConfiguration
class TestClockConfig {

    @Bean
    @Primary
    fun mutableClock(): MutableClock = MutableClock()
}
