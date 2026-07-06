package com.agentwallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AgentWalletColorScheme = lightColorScheme(
    primary                = Accent,
    onPrimary              = TextOnAccent,
    primaryContainer       = AccentSubtle,
    onPrimaryContainer     = AccentGlow,
    background             = BackgroundPrimary,
    onBackground           = TextPrimary,
    surface                = BackgroundSecondary,
    onSurface              = TextPrimary,
    surfaceVariant         = BackgroundTertiary,
    onSurfaceVariant       = TextSecondary,
    outline                = BorderSubtle,
    outlineVariant         = DividerSubtle,
    error                  = Loss,
    onError                = Color.White,
    inverseSurface         = BackgroundSecondary,
    inverseOnSurface       = TextSecondary,
    scrim                  = Color(0x80000000)
)

@Composable
fun AgentWalletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AgentWalletColorScheme,
        typography = AgentWalletTypography,
        content = content
    )
}
