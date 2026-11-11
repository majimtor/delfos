package com.manuelalejandro.delfos.api

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/** A ticking clock the tests can move forward to exercise expiry rules: real time plus an offset. */
class MutableClock : Clock() {

    @Volatile
    private var offset: Duration = Duration.ZERO

    fun advance(duration: Duration) {
        offset = offset.plus(duration)
    }

    fun reset() {
        offset = Duration.ZERO
    }

    override fun instant(): Instant = Instant.now().plus(offset)

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}

@TestConfiguration
class TestClockConfig {

    @Bean
    @Primary
    fun mutableClock(): MutableClock = MutableClock()
}
