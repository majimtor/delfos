package com.manuelalejandro.delfos.mobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The blue of the web client, so that both clients read as one product. */
private val Blue = Color(0xFF1976D2)
private val BlueOnDark = Color(0xFF90CAF9)
private val Amber = Color(0xFFB45309)

private val LightColors = lightColorScheme(primary = Blue, tertiary = Amber)
private val DarkColors = darkColorScheme(primary = BlueOnDark, tertiary = Amber)

@Composable
internal fun DelfosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
