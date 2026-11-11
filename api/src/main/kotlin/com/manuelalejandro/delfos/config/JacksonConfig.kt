package com.manuelalejandro.delfos.config

import org.openapitools.jackson.nullable.JsonNullableJackson3Module
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.JacksonModule

@Configuration
class JacksonConfig {

    /** Reads and writes the `JsonNullable` fields of generated DTOs: absent, null or a value. */
    @Bean
    fun jsonNullableModule(): JacksonModule = JsonNullableJackson3Module()
}
