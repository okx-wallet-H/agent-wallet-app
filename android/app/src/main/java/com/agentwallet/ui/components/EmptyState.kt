package com.agentwallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.theme.Accent
import com.agentwallet.ui.theme.AccentGlow
import com.agentwallet.ui.theme.AgentWalletTypography
import com.agentwallet.ui.theme.BackgroundTertiary
import com.agentwallet.ui.theme.Spacing
import com.agentwallet.ui.theme.TextPrimary
import com.agentwallet.ui.theme.TextSecondary
import com.agentwallet.ui.theme.TextTertiary

/**
 * First-launch empty state with Agent greeting and example prompts.
 */
@Composable
fun EmptyState(
    onSuggestionTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Agent brand icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Accent, AccentGlow)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🧿",
                style = AgentWalletTypography.displayLarge.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                )
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        // Greeting
        Text(
            text = "我是你的交易 Agent",
            style = AgentWalletTypography.displayLarge,
            color = TextPrimary
        )

        Spacer(Modifier.height(Spacing.md))

        Text(
            text = "你可以直接跟我说：",
            style = AgentWalletTypography.bodyLarge,
            color = TextSecondary
        )

        Spacer(Modifier.height(Spacing.xl))

        // Example prompts
        val suggestions = listOf(
            "帮我监控 Solana 新币",
            "ETH 跌到 3000 就买",
            "帮我看看最近聪明钱在买什么"
        )

        suggestions.forEach { suggestion ->
            SuggestionPill(
                text = suggestion,
                onClick = { onSuggestionTap(suggestion) }
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun SuggestionPill(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundTertiary)
            .clickableNoRipple(onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Text(
            text = "💬  $text",
            style = AgentWalletTypography.bodyLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** No-ripple clickable for suggestions. */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return this.then(
        androidx.compose.foundation.clickable(
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        ) { onClick() }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewEmptyState() {
    EmptyState(onSuggestionTap = {})
}
