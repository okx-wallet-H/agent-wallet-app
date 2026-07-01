package com.agentwallet.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.theme.BackgroundSecondary
import com.agentwallet.ui.theme.BackgroundTertiary

/**
 * A single shimmer placeholder matching a message bubble shape.
 */
@Composable
fun ShimmerBubble(
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberShimmerBrush()

    Box(
        modifier = modifier
            .fillMaxWidth(if (isUser) 0.7f else 0.8f)
            .height(if (isUser) 36.dp else 60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(shimmerBrush)
    )
}

/**
 * Full chat skeleton with agent + user bubbles.
 */
@Composable
fun ChatSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Agent message skeleton
        Row {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(rememberShimmerBrush())
            )
            Spacer(Modifier.width(8.dp))
            ShimmerBubble(isUser = false)
        }

        Spacer(Modifier.height(16.dp))

        // User message skeleton
        ShimmerBubble(isUser = true)

        Spacer(Modifier.height(12.dp))

        // Another agent message
        Row {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(rememberShimmerBrush())
            )
            Spacer(Modifier.width(8.dp))
            ShimmerBubble(isUser = false)
        }
    }
}

@Composable
private fun rememberShimmerBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val baseColor = BackgroundSecondary
    val highlightColor = BackgroundTertiary

    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translate - 200, 0f),
        end = Offset(translate, 0f)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewSkeleton() {
    ChatSkeleton()
}
