package com.manuelalejandro.delfos.mobile.platform

import io.ktor.client.engine.HttpClientEngine

/** The HTTP engine of the platform: OkHttp on Android, the system session on iOS. */
internal expect fun httpClientEngine(): HttpClientEngine
