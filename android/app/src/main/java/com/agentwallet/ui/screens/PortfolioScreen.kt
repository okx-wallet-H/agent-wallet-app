package com.agentwallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.theme.AgentWalletTypography
import com.agentwallet.ui.theme.BackgroundPrimary
import com.agentwallet.ui.theme.BackgroundSecondary
import com.agentwallet.ui.theme.BackgroundTertiary
import com.agentwallet.ui.theme.MonoStyle
import com.agentwallet.ui.theme.Profit
import com.agentwallet.ui.theme.Loss
import com.agentwallet.ui.theme.Shapes
import com.agentwallet.ui.theme.Spacing
import com.agentwallet.ui.theme.TextPrimary
import com.agentwallet.ui.theme.TextSecondary
import com.agentwallet.ui.theme.TextTertiary

data class TokenHolding(
    val symbol: String,
    val amount: Double,
    val usdValue: Double,
    val change24h: Double
)

data class RecentTransaction(
    val token: String,
    val action: String,
    val amount: String,
    val pnl: String,
    val isPositive: Boolean
)

@Composable
fun PortfolioScreen(modifier: Modifier = Modifier) {
    // Mock data
    val totalValue = 12450.23
    val dailyPnl = 234.50
    val dailyPnlPercent = 1.92

    val holdings = listOf(
        TokenHolding("ETH", 2.3, 7866.00, 1.8),
        TokenHolding("SOL", 150.0, 2550.00, 3.2),
        TokenHolding("USDC", 5000.0, 5000.00, 0.0)
    )

    val recentTx = listOf(
        RecentTransaction("NEWPUMP", "买入", "0.05 ETH", "+$45.20", true),
        RecentTransaction("MIDCAP", "卖出", "1000 TOKEN", "-$12.30", false),
        RecentTransaction("MEME2", "买入", "0.02 ETH", "+$8.50", true)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(horizontal = Spacing.screenHorizontal)
    ) {
        // ─── Total balance ──────────────────
        item {
            Spacer(Modifier.height(Spacing.xl))
            Text("总资产", style = AgentWalletTypography.bodySmall, color = TextTertiary)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "$${formatAmount(totalValue)}",
                style = com.agentwallet.ui.theme.NumberStyle,
                color = TextPrimary
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+$${formatAmount(dailyPnl)} (+${dailyPnlPercent}%)",
                    style = AgentWalletTypography.bodyMedium,
                    color = Profit
                )
                Text(
                    text = " 今日",
                    style = AgentWalletTypography.bodySmall,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.height(Spacing.xl))
        }

        // ─── Holdings ────────────────────────
        item {
            Text("持仓", style = AgentWalletTypography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(Spacing.sm))
        }

        items(holdings) { holding ->
            HoldingRow(holding)
            Spacer(Modifier.height(Spacing.sm))
        }

        // ─── Recent transactions ────────────
        item {
            Spacer(Modifier.height(Spacing.xl))
            Text("最近交易", style = AgentWalletTypography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(Spacing.sm))
        }

        items(recentTx) { tx ->
            TransactionRow(tx)
            Spacer(Modifier.height(Spacing.sm))
        }

        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}

@Composable
private fun HoldingRow(holding: TokenHolding) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .background(BackgroundSecondary)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Token icon placeholder
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BackgroundTertiary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = holding.symbol.take(1),
                style = AgentWalletTypography.titleMedium,
                color = TextPrimary
            )
        }

        Spacer(Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(holding.symbol, style = AgentWalletTypography.bodyMedium, color = TextPrimary)
            Text(
                text = "${holding.amount}",
                style = AgentWalletTypography.bodySmall,
                color = TextSecondary
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${formatAmount(holding.usdValue)}",
                style = MonoStyle.copy(color = TextPrimary)
            )
            Text(
                text = "${if (holding.change24h > 0) "+" else ""}${holding.change24h}%",
                style = AgentWalletTypography.labelMedium,
                color = if (holding.change24h >= 0) Profit else Loss
            )
        }
    }
}

@Composable
private fun TransactionRow(tx: RecentTransaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .background(BackgroundSecondary)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (tx.isPositive) "⬆️" else "⬇️",
            style = AgentWalletTypography.titleMedium
        )
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${tx.action} ${tx.token}",
                style = AgentWalletTypography.bodyMedium,
                color = TextPrimary
            )
            Text(tx.amount, style = AgentWalletTypography.bodySmall, color = TextSecondary)
        }
        Text(
            text = tx.pnl,
            style = MonoStyle.copy(color = if (tx.isPositive) Profit else Loss)
        )
    }
}

private fun formatAmount(value: Double): String {
    return if (value >= 1000) {
        "%,.2f".format(value)
    } else {
        "%.2f".format(value)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewPortfolio() {
    PortfolioScreen()
}
