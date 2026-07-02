package com.agentwallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TextButton
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

/**
 * Glass-style card showing trade details before execution.
 * Contains security check indicators and a swipe-to-confirm button.
 */
@Composable
fun TradeConfirmationCard(
    message: ChatMessage.TradeConfirmation,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .border(1.dp, BorderSubtle, Shapes.card)
            .background(BackgroundGlass)
            .padding(Spacing.lg)
    ) {
        // Header
        Text(
            text = "📊 交易确认",
            style = AgentWalletTypography.titleMedium,
            color = TextPrimary
        )

        Spacer(Modifier.height(Spacing.md))

        // Token + amounts
        Text(
            text = "买入 ${message.token}",
            style = AgentWalletTypography.headlineMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "${message.amount} ${message.fromToken}  →  ~${message.toAmount} ${message.token}",
            style = com.agentwallet.ui.theme.MonoStyle,
            color = TextSecondary
        )

        Spacer(Modifier.height(Spacing.sm))

        // Details row
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            DetailItem("滑点", message.slippage)
            DetailItem("Gas 预估", message.gasEstimate)
            DetailItem("链", message.chain)
        }

        Spacer(Modifier.height(Spacing.md))

        // Security checks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            message.checks.forEach { check ->
                SecurityCheckIndicator(check)
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // Swipe to confirm
        SwipeToConfirmButton(
            label = "滑动确认买入",
            onConfirmed = message.onConfirm
        )

        Spacer(Modifier.height(Spacing.sm))

        // Reject
        TextButton(
            onClick = message.onReject,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("拒绝", color = TextTertiary)
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(label, style = AgentWalletTypography.labelMedium, color = TextTertiary)
        Text(value, style = AgentWalletTypography.labelLarge, color = TextPrimary)
    }
}

@Composable
private fun SecurityCheckIndicator(check: SecurityCheck) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (check.passed) Profit.copy(alpha = 0.15f) else Loss.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (check.passed) "✓" else "✗",
                style = AgentWalletTypography.labelMedium,
                color = if (check.passed) Profit else Loss
            )
        }
        Spacer(Modifier.width(Spacing.xs))
        Text(check.label, style = AgentWalletTypography.labelMedium, color = TextSecondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewTradeConfirmation() {
    TradeConfirmationCard(
        message = ChatMessage.TradeConfirmation(
            token = "NEWCOIN",
            chain = "Ethereum",
            amount = "0.05",
            fromToken = "ETH",
            toAmount = "12,500",
            slippage = "3%",
            gasEstimate = "~$2.30",
            checks = listOf(
                SecurityCheck("Dev 信誉", true),
                SecurityCheck("LP 锁定", true),
                SecurityCheck("捆绑 < 30%", true)
            )
        )
    )
}
