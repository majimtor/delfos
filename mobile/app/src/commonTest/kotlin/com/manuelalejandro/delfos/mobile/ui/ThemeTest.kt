package com.manuelalejandro.delfos.mobile.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The tokens of ADR 0017 promise WCAG AA contrast: 4.5:1 for text on the surface it sits on and
 * 3:1 for the border of a control against its surface. A value that breaks the promise fails here
 * before it reaches a screen, in both schemes.
 */
class ThemeTest {

    private fun luminance(colour: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(colour.red) + 0.7152 * channel(colour.green) + 0.0722 * channel(colour.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun assertReadable(name: String, foreground: Color, background: Color, minimum: Double = 4.5) {
        val ratio = contrast(foreground, background)
        assertTrue(ratio >= minimum, "$name: $ratio is below $minimum")
    }

    private fun check(scheme: ColorScheme, extra: DelfosColors, label: String) {
        assertReadable("$label onPrimary", scheme.onPrimary, scheme.primary)
        assertReadable("$label onPrimaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer)
        assertReadable("$label onSecondary", scheme.onSecondary, scheme.secondary)
        assertReadable("$label onSecondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer)
        assertReadable("$label onTertiary", scheme.onTertiary, scheme.tertiary)
        assertReadable("$label onTertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer)
        assertReadable("$label onError", scheme.onError, scheme.error)
        assertReadable("$label onErrorContainer", scheme.onErrorContainer, scheme.errorContainer)
        assertReadable("$label onBackground", scheme.onBackground, scheme.background)
        assertReadable("$label onSurface", scheme.onSurface, scheme.surface)
        assertReadable("$label onSurfaceVariant", scheme.onSurfaceVariant, scheme.surfaceVariant)
        assertReadable("$label onSurface on container high", scheme.onSurface, scheme.surfaceContainerHigh)
        assertReadable("$label onSurface on container highest", scheme.onSurface, scheme.surfaceContainerHighest)
        assertReadable("$label primary on surface", scheme.primary, scheme.surface)
        assertReadable("$label primary on background", scheme.primary, scheme.background)
        assertReadable("$label error on surface", scheme.error, scheme.surface)
        assertReadable("$label tertiary on surface", scheme.tertiary, scheme.surface)
        assertReadable("$label outline on surface", scheme.outline, scheme.surface, minimum = 3.0)
        assertReadable("$label outline on background", scheme.outline, scheme.background, minimum = 3.0)
        assertReadable("$label onSuccess", extra.onSuccess, extra.success)
        assertReadable("$label onSuccessContainer", extra.onSuccessContainer, extra.successContainer)
        assertReadable("$label success on surface", extra.success, scheme.surface)
    }

    @Test
    fun lightSchemeKeepsEveryPairReadable() = check(LightColors, LightDelfosColors, "light")

    @Test
    fun darkSchemeKeepsEveryPairReadable() = check(DarkColors, DarkDelfosColors, "dark")
}
