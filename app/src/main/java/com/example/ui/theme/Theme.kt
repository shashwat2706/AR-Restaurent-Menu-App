package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ClayPrimary,
    onPrimary = Color.White,
    secondary = WarmGoldAccent,
    onSecondary = Color.Black,
    tertiary = SageGreen,
    onTertiary = Color.Black,
    background = SlateGreyDark,
    onBackground = BoneWhite,
    surface = SlateMediumSurface,
    onSurface = BoneWhite,
    surfaceVariant = SlateBorderLight,
    onSurfaceVariant = TextMutedSlate,
    outline = SlateBorderLight,
    error = WarmRedMuted,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ClayPrimaryDim,
    onPrimary = Color.White,
    secondary = WarmGoldAccent,
    onSecondary = Color.Black,
    tertiary = SageGreen,
    onTertiary = Color.White,
    background = Color(0xFFFBFBFD),
    onBackground = SlateGreyDark,
    surface = Color.White,
    onSurface = SlateGreyDark,
    surfaceVariant = Color(0xFFF1F3F9),
    onSurfaceVariant = TextMutedSlate,
    outline = Color(0xFFE2E8F0),
    error = WarmRedMuted,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme for a camera/AR ambient gourmet app experience
    dynamicColor: Boolean = false, // Disable dynamic colors to keep our premium branding consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
