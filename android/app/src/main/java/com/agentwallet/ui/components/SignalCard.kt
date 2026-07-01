package com.agentwallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.agentwallet.ui.theme.AgentWalletTypography
import com.agentwallet.ui.theme.BackgroundGlass
import com.agentwallet.ui.theme.BorderSubtle
import com.agentwallet.ui.theme.Loss
import com.agentwallet.ui.theme.Profit
import com.agentwallet.ui.theme.Shapes
import com.agentwallet.ui.theme.Spacing
import com.agentwallet.ui.theme.TextPrimary
import com.agentwallet.ui.theme.TextSecondary
import com.agentwallet.ui.theme.TextTertiary
import com.agentwallet.ui.theme.Warning

/**
 * Signal alert card with color-coded left border:
 *   red (new signal), yellow (rising), green (executed).
 */
@Composable
fun SignalCard(
    message: ChatMessage.SignalAlert,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        message.isNew && message.confidence == "high" -> Profit
        message.isNew && message.confidence == "medium" -> Warning
        message.isNew -> Loss
        else -> TextTertiary  // already seen / executed
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .border(1.dp, BorderSubtle, Shapes.card)
            .background(BackgroundGlass)
            .padding(Spacing.lg)
    ) {
        // Top row: status dot + freshness
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(borderColor)
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = if (message.isNew) "新" else "已执行",
                style = AgentWalletTypography.labelMedium,
                color = borderColor
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = message.token,
                style = AgentWalletTypography.titleMedium,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        // Chain + amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(message.chain, style = AgentWalletTypography.labelMedium, color = TextTertiary)
            Text("💰 ${message.amountUsd}", style = MonoStyle, color = TextPrimary)
        }

        Spacer(Modifier.height(Spacing.sm))

        // Wallet count
        Text(
            text = "${message.walletCount} 个聪明钱买入",
            style = AgentWalletTypography.bodySmall,
            color = TextSecondary
        )

        Spacer(Modifier.height(Spacing.sm))

        // Security row
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            SecurityDot("Dev", message.devReputation)
            SecurityDot("捆绑 ${message.bundleRatio}", message.bundleRatio.replace("%", "").toFloatOrNull() ?: 100f < 30f)
            SecurityDot("LP", message.lpLocked)
        }

        // Divider
        Spacer(Modifier.height(Spacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSubtle)
        )
        Spacer(Modifier.height(Spacing.sm))

        // Confidence + action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "分析通过 · 置信度 ${message.confidence}",
                style = AgentWalletTypography.labelMedium,
                color = Profit
            )
            Text(
                text = "查看详情 →",
                style = AgentWalletTypography.labelMedium,
                color = com.agentwallet.ui.theme.Accent
            )
        }
    }
}

@Composable
private fun SecurityDot(label: String, passed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (passed) Profit else Loss)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(label, style = AgentWalletTypography.labelMedium, color = if (passed) TextSecondary else Loss)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewSignalCard() {
    SignalCard(
        message = ChatMessage.SignalAlert(
            token = "$NEWPUMP",
            chain = "Solana",
            amountUsd = "$12,500",
            walletCount = 3,
            devReputation = true,
            bundleRatio = "8%",
            lpLocked = true,
            confidence = "high",
            isNew = true
        )
    )
}
