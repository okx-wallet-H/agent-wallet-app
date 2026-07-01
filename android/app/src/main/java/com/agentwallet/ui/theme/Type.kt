package com.agentwallet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Placeholder — real project should bundle font files in res/font/
// and reference them here. For now we use system fonts that approximate
// the design: Inter (sans-serif) and JetBrains Mono (monospace).

private val SansSerifFamily  = FontFamily.Default
private val MonospaceFamily  = FontFamily.Monospace

val AgentWalletTypography = Typography(
    // ─── Headlines ────────────────────────────
    displayLarge = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),

    // ─── Body ──────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),

    // ─── Caption ───────────────────────────────
    bodySmall = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    ),

    // ─── Labels ────────────────────────────────
    labelLarge = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = SansSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    )
)

// Convenience styles for mono (addresses / hashes / amounts)
val MonoStyle = TextStyle(
    fontFamily = MonospaceFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    color = TextPrimary
)

val MonoSmallStyle = TextStyle(
    fontFamily = MonospaceFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    color = TextSecondary
)

// Number style for tabular lining
val NumberStyle = TextStyle(
    fontFamily = SansSerifFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 28.sp,
    lineHeight = 36.sp,
    color = TextPrimary
)
