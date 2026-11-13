package com.manuelalejandro.delfos.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** The scheduled work of the API: for now, the tick that feeds the results event streams. */
@Configuration
@EnableScheduling
class SchedulingConfig
