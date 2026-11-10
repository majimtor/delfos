package com.manuelalejandro.delfos.config

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** One clock for the whole application, so time-dependent rules can be tested by replacing it. */
@Configuration
class TimeConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
