package com.agentwallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
 * Post-execution trade result card.
 * Shows token, action, amount, txHash, and status.
 */
@Composable
fun TradeResultCard(
    message: ChatMessage.TradeResult,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current

    val statusColor = when (message.status) {
        TxStatus.PENDING   -> Warning
        TxStatus.CONFIRMED -> Profit
        TxStatus.FAILED    -> Loss
    }
    val statusLabel = when (message.status) {
        TxStatus.PENDING   -> "确认中"
        TxStatus.CONFIRMED -> "已完成"
        TxStatus.FAILED    -> "失败"
    }
    val actionArrow = if (message.action == TradeAction.BUY) "⬆️" else "⬇️"
    val pnlColor = if (message.action == TradeAction.BUY) Profit else Loss

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .border(1.dp, BorderSubtle, Shapes.card)
            .background(BackgroundGlass)
            .padding(Spacing.lg)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(actionArrow, style = AgentWalletTypography.titleMedium)
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = "${message.action.name} ${message.token}",
                    style = AgentWalletTypography.titleMedium,
                    color = TextPrimary
                )
            }
            // Status badge
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = Spacing.sm, vertical = 2.dp)
            ) {
                Text(statusLabel, style = AgentWalletTypography.labelMedium, color = statusColor)
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // Amount
        Text(
            text = "${message.amount} ${message.token}",
            style = com.agentwallet.ui.theme.MonoStyle,
            color = TextPrimary
        )
        Text(
            text = "@ ${message.price}",
            style = AgentWalletTypography.bodySmall,
            color = TextSecondary
        )

        Spacer(Modifier.height(Spacing.sm))

        // TxHash (full, clickable to copy)
        Row(
            modifier = Modifier
                .clickable { clipboard.setText(AnnotatedString(message.txHash)) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "txHash: ",
                style = AgentWalletTypography.labelMedium,
                color = TextTertiary
            )
            Text(
                text = message.txHash,
                style = com.agentwallet.ui.theme.MonoSmallStyle,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewTradeResult() {
    TradeResultCard(
        message = ChatMessage.TradeResult(
            token = "NEWCOIN",
            action = TradeAction.BUY,
            amount = "0.05 ETH",
            price = "$3,420",
            txHash = "0xabc123def456abc123def456abc123def456abc123def456abc123def456abc1",
            status = TxStatus.CONFIRMED
        )
    )
}
