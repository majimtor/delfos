package com.manuelalejandro.delfos.mobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manuelalejandro.delfos.mobile.resources.Res
import com.manuelalejandro.delfos.mobile.resources.ibm_plex_sans_medium
import com.manuelalejandro.delfos.mobile.resources.ibm_plex_sans_regular
import com.manuelalejandro.delfos.mobile.resources.ibm_plex_sans_semibold
import org.jetbrains.compose.resources.Font

/*
 * The design tokens of ADR 0017, the ones the web declares, as a Material 3 theme: the petrol
 * brand colour with neutrals biased towards it, a dark scheme with the same roles, IBM Plex Sans
 * served with the application, and the shapes of the web. The scheme follows the device's
 * setting, as Material and the platform guidelines expect; the dynamic colour of the device is not
 * used, so both clients read as one product. ThemeTest keeps every pair of the schemes at the
 * contrast WCAG asks.
 */

private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)

internal val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0B5B6B),
    onPrimary = White,
    primaryContainer = Color(0xFFD5E8EB),
    onPrimaryContainer = Color(0xFF0F1F20),
    inversePrimary = Color(0xFF6CC5D6),
    secondary = Color(0xFF44585A),
    onSecondary = White,
    secondaryContainer = Color(0xFFD5E8EB),
    onSecondaryContainer = Color(0xFF0F1F20),
    tertiary = Color(0xFFA95E0C),
    onTertiary = White,
    tertiaryContainer = Color(0xFFFBE7D0),
    onTertiaryContainer = Color(0xFF4A2A05),
    error = Color(0xFFB42318),
    onError = White,
    errorContainer = Color(0xFFFBE3E0),
    onErrorContainer = Color(0xFF5A110B),
    background = Color(0xFFF4F7F7),
    onBackground = Color(0xFF0F1F20),
    surface = White,
    onSurface = Color(0xFF0F1F20),
    surfaceVariant = Color(0xFFEAF0F0),
    onSurfaceVariant = Color(0xFF44585A),
    surfaceContainerLowest = White,
    surfaceContainerLow = Color(0xFFF4F7F7),
    surfaceContainer = Color(0xFFEAF0F0),
    surfaceContainerHigh = Color(0xFFE0E9E9),
    surfaceContainerHighest = Color(0xFFD5E1E1),
    inverseSurface = Color(0xFF0F1F20),
    inverseOnSurface = Color(0xFFE6EEEE),
    outline = Color(0xFF5E7577),
    outlineVariant = Color(0xFFC9D6D6),
    scrim = Black,
)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF6CC5D6),
    onPrimary = Color(0xFF0C1517),
    primaryContainer = Color(0xFF15353C),
    onPrimaryContainer = Color(0xFFE6EEEE),
    inversePrimary = Color(0xFF0B5B6B),
    secondary = Color(0xFF9DB2B3),
    onSecondary = Color(0xFF0C1517),
    secondaryContainer = Color(0xFF15353C),
    onSecondaryContainer = Color(0xFFE6EEEE),
    tertiary = Color(0xFFF2B35A),
    onTertiary = Color(0xFF0C1517),
    tertiaryContainer = Color(0xFF4A2E08),
    onTertiaryContainer = Color(0xFFFBE3C3),
    error = Color(0xFFF28B82),
    onError = Color(0xFF0C1517),
    errorContainer = Color(0xFF5A1A14),
    onErrorContainer = Color(0xFFFCD9D5),
    background = Color(0xFF0C1517),
    onBackground = Color(0xFFE6EEEE),
    surface = Color(0xFF122023),
    onSurface = Color(0xFFE6EEEE),
    surfaceVariant = Color(0xFF18292D),
    onSurfaceVariant = Color(0xFF9DB2B3),
    surfaceContainerLowest = Color(0xFF0C1517),
    surfaceContainerLow = Color(0xFF0F1B1E),
    surfaceContainer = Color(0xFF18292D),
    surfaceContainerHigh = Color(0xFF1E3237),
    surfaceContainerHighest = Color(0xFF253B41),
    inverseSurface = Color(0xFFE6EEEE),
    inverseOnSurface = Color(0xFF0F1F20),
    outline = Color(0xFF8DA3A5),
    outlineVariant = Color(0xFF2B4145),
    scrim = Black,
)

/** The colours Material 3 has no role for: a state that went well, as the web's success chip. */
@Immutable
internal class DelfosColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

internal val LightDelfosColors = DelfosColors(
    success = Color(0xFF137A4B),
    onSuccess = White,
    successContainer = Color(0xFFD7F0E2),
    onSuccessContainer = Color(0xFF0B3D26),
)

internal val DarkDelfosColors = DelfosColors(
    success = Color(0xFF5BD39A),
    onSuccess = Color(0xFF0C1517),
    successContainer = Color(0xFF113B2A),
    onSuccessContainer = Color(0xFFC9F2DC),
)

internal val LocalDelfosColors = staticCompositionLocalOf { LightDelfosColors }

/** 6 dp on controls and 10 dp on cards and sheets, as the web; buttons keep Material's pill. */
internal val DelfosShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
private fun plexSans(): FontFamily = FontFamily(
    Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
)

/** Material's type scale in IBM Plex Sans: headings semibold with slightly tightened tracking. */
internal fun delfosTypography(family: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.heading(tracking: Float = -0.01f): TextStyle =
        copy(fontFamily = family, fontWeight = FontWeight.SemiBold, letterSpacing = (fontSize.value * tracking).sp)
    fun TextStyle.body(weight: FontWeight = FontWeight.Normal): TextStyle = copy(fontFamily = family, fontWeight = weight)
    return Typography(
        displayLarge = base.displayLarge.heading(-0.02f),
        displayMedium = base.displayMedium.heading(-0.02f),
        displaySmall = base.displaySmall.heading(-0.02f),
        headlineLarge = base.headlineLarge.heading(),
        headlineMedium = base.headlineMedium.heading(),
        headlineSmall = base.headlineSmall.heading(),
        titleLarge = base.titleLarge.heading(),
        titleMedium = base.titleMedium.body(FontWeight.SemiBold),
        titleSmall = base.titleSmall.body(FontWeight.Medium),
        bodyLarge = base.bodyLarge.body(),
        bodyMedium = base.bodyMedium.body(),
        bodySmall = base.bodySmall.body(),
        labelLarge = base.labelLarge.body(FontWeight.Medium),
        labelMedium = base.labelMedium.body(FontWeight.Medium),
        labelSmall = base.labelSmall.body(FontWeight.Medium),
    )
}

@Composable
internal fun DelfosTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val family = plexSans()
    val typography = remember(family) { delfosTypography(family) }
    CompositionLocalProvider(LocalDelfosColors provides if (dark) DarkDelfosColors else LightDelfosColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = typography,
            shapes = DelfosShapes,
            content = content,
        )
    }
}

/** The colours beyond Material's roles, from the current scheme. */
internal val MaterialTheme.delfosColors: DelfosColors
    @Composable get() = LocalDelfosColors.current
