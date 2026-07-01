package com.agentwallet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Corner radius tokens used across the app.
 *
 * Bubble: 16dp everywhere except the tail corner (4dp).
 * Card:   16dp uniform.
 * Pill:   24dp (capsule for input fields / small buttons).
 */
object Shapes {
    val bubbleUser  = RoundedCornerShape(
        topStart     = 16.dp,
        topEnd       = 16.dp,
        bottomStart  = 16.dp,
        bottomEnd    = 4.dp   // tail bottom-right
    )

    val bubbleAgent = RoundedCornerShape(
        topStart     = 16.dp,
        topEnd       = 16.dp,
        bottomStart  = 4.dp,  // tail bottom-left
        bottomEnd    = 16.dp
    )

    val card   = RoundedCornerShape(16.dp)
    val pill   = RoundedCornerShape(24.dp)
    val circle = RoundedCornerShape(50)
}
