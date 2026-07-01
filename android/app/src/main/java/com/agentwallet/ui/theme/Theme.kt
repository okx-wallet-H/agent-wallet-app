package com.agentwallet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val AgentWalletColorScheme = darkColorScheme(
    primary                = Accent,
    onPrimary              = TextOnAccent,
    primaryContainer       = AccentSubtle,
    onPrimaryContainer     = AccentGlow,
    secondary              = AccentGlow,
    onSecondary            = TextOnAccent,
    secondaryContainer     = AccentSubtle,
    onSecondaryContainer   = AccentGlow,
    tertiary               = Info,
    onTertiary             = TextOnAccent,
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
    errorContainer         = Color(0x1AFF6B6B),
    onErrorContainer       = Loss,
    inverseSurface         = BackgroundSecondary,
    inverseOnSurface       = TextSecondary,
    inversePrimary         = AccentGlow,
    scrim                  = Color(0x80000000)
)

@Composable
fun AgentWalletTheme(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()

    SideEffect {
        // Set status bar to blend into our dark background
        systemUiController.setStatusBarColor(
            color = BackgroundPrimary,
            darkIcons = false
        )
        // Set nav bar to match
        systemUiController.setNavigationBarColor(
            color = BackgroundSecondary
        )
    }

    MaterialTheme(
        colorScheme = AgentWalletColorScheme,
        typography = AgentWalletTypography,
        content = content
    )
}
