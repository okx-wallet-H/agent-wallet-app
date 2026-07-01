package com.agentwallet.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.theme.Accent
import com.agentwallet.ui.theme.BackgroundSecondary
import com.agentwallet.ui.theme.BackgroundTertiary
import com.agentwallet.ui.theme.Shapes
import com.agentwallet.ui.theme.Spacing
import com.agentwallet.ui.theme.TextPrimary
import com.agentwallet.ui.theme.TextTertiary

/**
 * Bottom chat input bar with voice, attachment, and send buttons.
 */
@Composable
fun ChatInputBar(
    onSend: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    val sendBg by animateColorAsState(
        targetValue = if (text.isNotBlank()) Accent else BackgroundTertiary,
        animationSpec = tween(200),
        label = "send_bg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundSecondary)
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ─── Text field ────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(Shapes.pill)
                .background(BackgroundTertiary)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "输入交易指令...",
                    style = com.agentwallet.ui.theme.AgentWalletTypography.bodyLarge,
                    color = TextTertiary
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                enabled = enabled,
                textStyle = com.agentwallet.ui.theme.AgentWalletTypography.bodyLarge.copy(
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true
            )
        }

        Spacer(Modifier.width(Spacing.xs))

        // ─── Voice button ─────────────────
        IconButton(
            onClick = { /* TODO: voice input */ },
            enabled = enabled,
            modifier = Modifier.size(40.dp)
        ) {
            Text("🎤", style = com.agentwallet.ui.theme.AgentWalletTypography.bodyLarge)
        }

        // ─── Attachment button ────────────
        IconButton(
            onClick = { /* TODO: attachment */ },
            enabled = enabled,
            modifier = Modifier.size(40.dp)
        ) {
            Text("📎", style = com.agentwallet.ui.theme.AgentWalletTypography.bodyLarge)
        }

        // ─── Send button ──────────────────
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text.trim())
                    text = ""
                }
            },
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(sendBg)
        ) {
            Text(
                "➤",
                style = com.agentwallet.ui.theme.AgentWalletTypography.titleMedium,
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewInputBar() {
    ChatInputBar(onSend = {})
}
