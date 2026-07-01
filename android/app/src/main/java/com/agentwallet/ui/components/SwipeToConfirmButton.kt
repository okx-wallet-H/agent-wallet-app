package com.agentwallet.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.theme.Accent
import com.agentwallet.ui.theme.AccentGlow
import com.agentwallet.ui.theme.BackgroundSecondary
import com.agentwallet.ui.theme.Shapes
import com.agentwallet.ui.theme.TextOnAccent
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SwipeState { IDLE, DRAGGING, CONFIRMING, SUCCESS, FAILED }

/**
 * Slide-right-to-confirm button.
 * User drags the thumb from left to right; at 80% it auto-snaps to confirm.
 */
@Composable
fun SwipeToConfirmButton(
    label: String,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var state by remember { mutableStateOf(SwipeState.IDLE) }
    val coroutineScope = rememberCoroutineScope()
    val thumbOffset = remember { Animatable(0f) }

    val density = LocalDensity.current
    val thumbSizePx = with(density) { 40.dp.toPx() }
    val paddingPx = with(density) { 4.dp.toPx() }

    // Calculate the max drag distance (track width minus thumb and padding)
    val trackWidthPx = with(density) { 300.dp.toPx() } // will be measured
    var maxDragPx by remember { mutableStateOf(0f) }

    val threshold = 0.80f  // 80% to snap

    val trackBg = if (state == SwipeState.SUCCESS) Accent.copy(alpha = 0.3f)
    else BackgroundSecondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(trackBg),
        contentAlignment = Alignment.CenterStart
    ) {
        // Background label (fades as thumb moves right)
        if (state != SwipeState.CONFIRMING && state != SwipeState.SUCCESS) {
            Text(
                text = "⟵  $label  ⟶",
                style = com.agentwallet.ui.theme.AgentWalletTypography.labelLarge,
                color = com.agentwallet.ui.theme.TextSecondary,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (state == SwipeState.CONFIRMING) {
            Text(
                text = "确认中...",
                style = com.agentwallet.ui.theme.AgentWalletTypography.labelLarge,
                color = TextOnAccent,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (state == SwipeState.SUCCESS) {
            Text(
                text = "✓ 已确认",
                style = com.agentwallet.ui.theme.AgentWalletTypography.labelLarge,
                color = com.agentwallet.ui.theme.Profit,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Draggable thumb
        Box(
            modifier = Modifier
                .padding(4.dp)
                .offset { IntOffset(thumbOffset.value.roundToInt(), 0) }
                .size(40.dp)
                .clip(CircleShape)
                .background(Accent)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput

                    // Measure actual max drag on first touch
                    if (maxDragPx == 0f) {
                        maxDragPx = size.width.toFloat() - thumbSizePx - paddingPx * 2
                    }

                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (thumbOffset.value / maxDragPx >= threshold) {
                                // Snap to confirm
                                state = SwipeState.CONFIRMING
                                coroutineScope.launch {
                                    thumbOffset.animateTo(maxDragPx, spring(stiffness = Spring.StiffnessMedium))
                                    onConfirmed()
                                    state = SwipeState.SUCCESS
                                }
                            } else {
                                // Snap back
                                state = SwipeState.IDLE
                                coroutineScope.launch {
                                    thumbOffset.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                                }
                            }
                        },
                        onDragCancel = {
                            state = SwipeState.IDLE
                            coroutineScope.launch {
                                thumbOffset.animateTo(0f, spring())
                            }
                        }
                    ) { _, dragAmount ->
                        state = SwipeState.DRAGGING
                        coroutineScope.launch {
                            val newValue = (thumbOffset.value + dragAmount)
                                .coerceIn(0f, maxDragPx)
                            thumbOffset.snapTo(newValue)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text("⟶", color = Color.White)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewSwipe() {
    Box(modifier = Modifier.padding(16.dp)) {
        SwipeToConfirmButton(
            label = "滑动确认买入",
            onConfirmed = {}
        )
    }
}
