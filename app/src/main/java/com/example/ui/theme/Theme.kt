package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val SmileLightColorScheme = lightColorScheme(
    primary = SmileYellow,
    onPrimary = SmileInk,
    primaryContainer = SmileYellowSoft,
    onPrimaryContainer = SmileInk,
    secondary = SmileInk,
    onSecondary = SmileWhite,
    secondaryContainer = SmileSurfaceSoft,
    onSecondaryContainer = SmileInk,
    tertiary = SmileOnlineGreen,
    onTertiary = SmileWhite,
    background = SmileWhite,
    onBackground = SmileInk,
    surface = SmileWhite,
    onSurface = SmileInk,
    surfaceVariant = SmileSurfaceSoft,
    onSurfaceVariant = SmileMuted,
    outline = SmileSurfaceBorder,
    error = SmileErrorRed,
    onError = SmileWhite
)

private val SmileDarkColorScheme = darkColorScheme(
    primary = SmileYellow,
    onPrimary = SmileInk,
    primaryContainer = SmileDarkBubbleOutgoing,
    onPrimaryContainer = SmileYellowBright,
    secondary = SmileYellowBright,
    onSecondary = SmileInk,
    secondaryContainer = SmileDarkCard,
    onSecondaryContainer = SmileDarkInk,
    tertiary = SmileDeliveredGreen,
    onTertiary = SmileInk,
    background = SmileDarkBg,
    onBackground = SmileDarkInk,
    surface = SmileDarkSurface,
    onSurface = SmileDarkInk,
    surfaceVariant = SmileDarkCard,
    onSurfaceVariant = SmileDarkMuted,
    outline = Color(0xFF2E313B),
    error = SmileErrorRed,
    onError = SmileWhite
)

private val SmileShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmileTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SmileDarkColorScheme else SmileLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SmileShapes,
        content = content
    )
}
