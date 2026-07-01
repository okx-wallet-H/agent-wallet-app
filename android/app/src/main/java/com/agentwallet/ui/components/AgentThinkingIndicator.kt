package com.agentwallet.ui.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.theme.Accent
import com.agentwallet.ui.theme.AccentGlow

/**
 * Three bouncing dots typing indicator.
 * Cycles through accent → accentGlow → accent with 300ms stagger.
 */
@Composable
fun AgentThinkingIndicator(modifier: Modifier = Modifier) {
    val dotSize = 8.dp
    val spacing = 6.dp

    val infiniteTransition = rememberInfiniteTransition(label = "thinking")

    // Three dots with staggered animations
    val dots = listOf(0, 1, 2).map { index ->
        animateDotAlpha(infiniteTransition, delayMs = index * 300)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { alpha ->
            Canvas(modifier = Modifier.size(dotSize)) {
                val color = if (alpha.value > 0.7f) AccentGlow else Accent
                drawCircle(
                    color = color.copy(alpha = alpha.value),
                    radius = size.minDimension / 2f
                )
            }
        }
    }
}

@Composable
private fun animateDotAlpha(
    infiniteTransition: InfiniteTransition,
    delayMs: Int
) = infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
        animation = keyframes {
            durationMillis = 900
            0.3f at 0
            0.3f at delayMs          // stay dim until stagger point
            1.0f at delayMs + 150    // rise
            0.3f at delayMs + 450    // fall
            0.3f at 900             // hold
        },
        repeatMode = RepeatMode.Restart
    ),
    label = "dot_${delayMs}"
)

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewThinking() {
    AgentThinkingIndicator()
}
