package com.shitu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 深色与浅色配色。颜色只影响观感，不参与游戏逻辑。
 */
private val ShituLightColors = lightColorScheme(
    primary = InkBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F1),
    onPrimaryContainer = InkBlueDeep,
    secondary = GoldDeep,
    onSecondary = Color.White,
    secondaryContainer = GoldSoft,
    onSecondaryContainer = Color(0xFF3D2F0C),
    tertiary = Jade,
    onTertiary = Color.White,
    background = Paper,
    onBackground = InkText,
    surface = PaperCard,
    onSurface = InkText,
    surfaceVariant = Color(0xFFEDE7DB),
    onSurfaceVariant = Slate,
    outline = PaperLine,
    error = Crimson,
    onError = Color.White
)

private val ShituDarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF2A2208),
    primaryContainer = Color(0xFF33414F),
    onPrimaryContainer = GoldSoft,
    secondary = GoldSoft,
    onSecondary = Color(0xFF2A2208),
    secondaryContainer = Color(0xFF3A4661),
    onSecondaryContainer = GoldSoft,
    tertiary = Color(0xFF7FC8B4),
    onTertiary = Color(0xFF06251E),
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceAlt,
    onSurfaceVariant = NightTextDim,
    outline = NightLine,
    error = Color(0xFFE38B84),
    onError = Color(0xFF3A0A06)
)

@Composable
fun ShituTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ShituDarkColors else ShituLightColors,
        typography = Typography,
        content = content
    )
}
