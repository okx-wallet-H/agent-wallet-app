package com.agentwallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.components.StrategyStatus
import com.agentwallet.ui.theme.Accent
import com.agentwallet.ui.theme.AgentWalletTypography
import com.agentwallet.ui.theme.BackgroundPrimary
import com.agentwallet.ui.theme.BackgroundSecondary
import com.agentwallet.ui.theme.BackgroundTertiary
import com.agentwallet.ui.theme.Loss
import com.agentwallet.ui.theme.Profit
import com.agentwallet.ui.theme.Shapes
import com.agentwallet.ui.theme.Spacing
import com.agentwallet.ui.theme.TextPrimary
import com.agentwallet.ui.theme.TextSecondary
import com.agentwallet.ui.theme.TextTertiary
import com.agentwallet.ui.theme.Warning

data class StrategyItem(
    val name: String,
    val chain: String,
    val status: StrategyStatus,
    val runtime: String,
    val trades: Int,
    val pnl: String
)

@Composable
fun StrategyScreen(modifier: Modifier = Modifier) {
    val strategies = listOf(
        StrategyItem("ETH 新币狙击", "Ethereum", StrategyStatus.RUNNING, "已运行 3 天", 8, "+$234.50"),
        StrategyItem("SOL 网格交易", "Solana", StrategyStatus.PAUSED, "暂停 2 小时", 15, "+$89.00"),
        StrategyItem("BTC 定投", "Ethereum", StrategyStatus.RUNNING, "已运行 2 周", 3, "+$12.30")
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(horizontal = Spacing.screenHorizontal)
    ) {
        item {
            Spacer(Modifier.height(Spacing.xl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("我的策略", style = AgentWalletTypography.headlineMedium, color = TextPrimary)
                // New strategy button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Accent)
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", style = AgentWalletTypography.titleMedium, color = androidx.compose.ui.graphics.Color.White)
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }

        items(strategies) { strategy ->
            StrategyListItem(strategy)
            Spacer(Modifier.height(Spacing.md))
        }

        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}

@Composable
private fun StrategyListItem(strategy: StrategyItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .background(BackgroundSecondary)
            .padding(Spacing.lg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status dot
                val dotColor = when (strategy.status) {
                    StrategyStatus.RUNNING -> Profit
                    StrategyStatus.PAUSED  -> Warning
                    StrategyStatus.STOPPED -> Loss
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "${strategy.name} · ${strategy.chain}",
                    style = AgentWalletTypography.titleMedium,
                    color = TextPrimary
                )
            }
            Text(
                text = when (strategy.status) {
                    StrategyStatus.RUNNING -> "● 运行中"
                    StrategyStatus.PAUSED  -> "○ 暂停中"
                    StrategyStatus.STOPPED -> "⏹ 已停止"
                },
                style = AgentWalletTypography.labelMedium,
                color = when (strategy.status) {
                    StrategyStatus.RUNNING -> Profit
                    StrategyStatus.PAUSED  -> Warning
                    StrategyStatus.STOPPED -> Loss
                }
            )
        }

        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "${strategy.runtime} · ${strategy.trades} 笔交易 · ${strategy.pnl}",
            style = AgentWalletTypography.bodySmall,
            color = TextSecondary
        )

        Spacer(Modifier.height(Spacing.md))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            ActionChip("⏸", "暂停")
            ActionChip("✏️", "修改")
            ActionChip("⏹", "停止")
        }
    }
}

@Composable
private fun ActionChip(icon: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundTertiary)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = AgentWalletTypography.bodySmall)
        Spacer(Modifier.width(Spacing.xs))
        Text(label, style = AgentWalletTypography.labelMedium, color = TextSecondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewStrategy() {
    StrategyScreen()
}
