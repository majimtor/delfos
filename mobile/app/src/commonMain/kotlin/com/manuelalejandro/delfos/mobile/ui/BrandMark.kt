package com.manuelalejandro.delfos.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The delta of the web's favicon and of the launcher icon, in the colours of the current scheme. */
@Composable
internal fun BrandMark(size: Dp = 32.dp, modifier: Modifier = Modifier) {
    val colours = MaterialTheme.colorScheme
    Box(
        modifier = modifier.size(size).background(colours.primary, RoundedCornerShape(percent = 20)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(OuterDelta, contentDescription = null, tint = colours.onPrimary, modifier = Modifier.fillMaxSize())
        Icon(InnerDelta, contentDescription = null, tint = colours.primary, modifier = Modifier.fillMaxSize())
    }
}

private fun delta(name: String, top: Float, left: Float, right: Float, bottom: Float): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 64.dp, defaultHeight = 64.dp, viewportWidth = 64f, viewportHeight = 64f)
        .path(fill = SolidColor(Color.Black)) {
            moveTo(32f, top)
            lineTo(left, bottom)
            lineTo(right, bottom)
            close()
        }
        .build()

private val OuterDelta: ImageVector by lazy { delta("OuterDelta", top = 13f, left = 13f, right = 51f, bottom = 51f) }
private val InnerDelta: ImageVector by lazy { delta("InnerDelta", top = 24.5f, left = 21.5f, right = 42.5f, bottom = 46f) }
