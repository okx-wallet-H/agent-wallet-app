package com.agentwallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.theme.Accent
import com.agentwallet.ui.theme.BackgroundSecondary
import com.agentwallet.ui.theme.Shapes
import com.agentwallet.ui.theme.Spacing
import com.agentwallet.ui.theme.TextPrimary
import com.agentwallet.ui.theme.TextSecondary
import com.agentwallet.ui.theme.TextTertiary
import com.agentwallet.ui.theme.Profit
import com.agentwallet.ui.theme.AccentGlow
import com.agentwallet.ui.theme.Loss
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders a single chat bubble.
 *
 * User messages: right-aligned, tail at bottom-right.
 * Agent messages: left-aligned with avatar + state, tail at bottom-left.
 */
@Composable
fun MessageBubble(
    message: ChatMessage.Text,
    agentState: AgentAvatarState = AgentAvatarState.NORMAL,
    modifier: Modifier = Modifier
) {
    if (message.isUser) {
        UserBubble(message, modifier)
    } else {
        AgentBubble(message, agentState, modifier)
    }
}

// ─── User bubble ───────────────────────────────────────

@Composable
private fun UserBubble(message: ChatMessage.Text, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(Shapes.bubbleUser)
                .background(BackgroundSecondary)
                .padding(horizontal = Spacing.bubbleHorizontal, vertical = Spacing.bubbleVertical)
        ) {
            Column {
                Text(
                    text = message.content,
                    style = com.agentwallet.ui.theme.AgentWalletTypography.bodyLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.size(Spacing.xs))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = com.agentwallet.ui.theme.AgentWalletTypography.labelMedium,
                    color = TextTertiary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// ─── Agent bubble ──────────────────────────────────────

@Composable
private fun AgentBubble(
    message: ChatMessage.Text,
    state: AgentAvatarState,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.Start
    ) {
        AgentAvatar(
            state = state,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.width(Spacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(Shapes.bubbleAgent)
                .background(BackgroundSecondary)
                .padding(horizontal = Spacing.bubbleHorizontal, vertical = Spacing.bubbleVertical)
        ) {
            Column {
                Text(
                    text = message.content,
                    style = com.agentwallet.ui.theme.AgentWalletTypography.bodyLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.size(Spacing.xs))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = com.agentwallet.ui.theme.AgentWalletTypography.labelMedium,
                    color = TextTertiary
                )
            }
        }
    }
}

// ─── Agent avatar ──────────────────────────────────────

enum class AgentAvatarState { NORMAL, THINKING, EXECUTING, COMPLETED }

@Composable
fun AgentAvatar(
    state: AgentAvatarState = AgentAvatarState.NORMAL,
    modifier: Modifier = Modifier
) {
    val bg = when (state) {
        AgentAvatarState.NORMAL    -> Accent
        AgentAvatarState.THINKING  -> AccentGlow
        AgentAvatarState.EXECUTING -> Accent
        AgentAvatarState.COMPLETED -> Profit
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        val indicator = when (state) {
            AgentAvatarState.NORMAL    -> "◉"
            AgentAvatarState.THINKING  -> "⚡"
            AgentAvatarState.EXECUTING -> "↻"
            AgentAvatarState.COMPLETED -> "✓"
        }
        Text(
            text = indicator,
            style = com.agentwallet.ui.theme.AgentWalletTypography.labelMedium.copy(
                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
            ),
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

// ─── Helpers ───────────────────────────────────────────

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun formatTimestamp(ts: Long): String = timeFmt.format(Date(ts))

// ─── Previews ──────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewUserBubble() {
    MessageBubble(
        message = ChatMessage.Text(
            content = "帮我设个 ETH 新币狙击策略",
            isUser = true
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewAgentBubble() {
    MessageBubble(
        message = ChatMessage.Text(
            content = "好的，请确认以下策略参数：",
            isUser = false
        ),
        agentState = AgentAvatarState.NORMAL
    )
}
