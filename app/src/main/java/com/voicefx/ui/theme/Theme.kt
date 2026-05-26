package com.voicefx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = VoiceFXPrimary,
    onPrimary = VoiceFXOnPrimary,
    secondary = VoiceFXSecondary,
    background = VoiceFXBackground,
    surface = VoiceFXSurface,
    onSurface = VoiceFXOnSurface,
    error = VoiceFXError,
    surfaceVariant = WhatsAppLightGray,
    tertiary = WhatsAppBlue
)

@Composable
fun VoiceFXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = VoiceFXTypography,
        content = content
    )
}
