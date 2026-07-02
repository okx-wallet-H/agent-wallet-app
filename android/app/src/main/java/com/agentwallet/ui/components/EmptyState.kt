package com.agentwallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
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

@Composable
fun EmptyState(
    onSuggestionTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Accent, AccentGlow))),
            contentAlignment = Alignment.Center
        ) { Text("🧿", style = AgentWalletTypography.displayLarge) }

        Spacer(Modifier.height(Spacing.xl))
        Text("我是你的交易 Agent", style = AgentWalletTypography.displayLarge, color = TextPrimary)
        Spacer(Modifier.height(Spacing.md))
        Text("你可以直接跟我说：", style = AgentWalletTypography.bodyLarge, color = TextSecondary)
        Spacer(Modifier.height(Spacing.xl))

        listOf("帮我监控 Solana 新币", "ETH 跌到 3000 就买", "帮我看看最近聪明钱在买什么").forEach { s ->
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)).background(BackgroundTertiary)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSuggestionTap(s) }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                Text("💬  $s", style = AgentWalletTypography.bodyLarge, color = TextPrimary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}
