package com.agentwallet.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Strategy status panel with running indicator, stats, and action buttons.
 */
@Composable
fun StrategyPanel(
    message: ChatMessage.StrategyCard,
    modifier: Modifier = Modifier
) {
    val isRunning = message.status == StrategyStatus.RUNNING

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .border(1.dp, BorderSubtle, Shapes.card)
            .background(BackgroundGlass)
            .padding(Spacing.lg)
    ) {
        // Header: name + status dot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status dot with breathing animation when running
                StatusDot(isRunning = isRunning)
                Spacer(Modifier.width(Spacing.sm))
                Column {
                    Text(
                        text = "${message.name} · ${message.chain}",
                        style = AgentWalletTypography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = message.runtime,
                        style = AgentWalletTypography.labelMedium,
                        color = TextTertiary
                    )
                }
            }
            // Status label
            Text(
                text = when (message.status) {
                    StrategyStatus.RUNNING -> "● 运行中"
                    StrategyStatus.PAUSED  -> "○ 暂停中"
                    StrategyStatus.STOPPED -> "⏹ 已停止"
                },
                style = AgentWalletTypography.labelMedium,
                color = when (message.status) {
                    StrategyStatus.RUNNING -> Profit
                    StrategyStatus.PAUSED  -> com.agentwallet.ui.theme.Warning
                    StrategyStatus.STOPPED -> Loss
                }
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("已交易", "${message.trades} 笔")
            StatItem("盈亏", message.pnl, isPnl = true)
        }

        Spacer(Modifier.height(Spacing.md))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton("⏸", "暂停", message.onPause)
            ActionButton("✏️", "修改", message.onEdit)
            ActionButton("⏹", "停止", message.onStop)  // always red
        }
    }
}

@Composable
private fun StatusDot(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    val dotColor = if (isRunning) Profit else com.agentwallet.ui.theme.TextTertiary
    val alpha = if (isRunning) pulse else 1f

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(dotColor.copy(alpha = alpha))
    )
}

@Composable
private fun StatItem(label: String, value: String, isPnl: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = AgentWalletTypography.labelMedium, color = TextTertiary)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = value,
            style = MonoStyle.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
            color = when {
                isPnl && value.startsWith("+") -> Profit
                isPnl && value.startsWith("-") -> Loss
                else -> TextPrimary
            }
        )
    }
}

@Composable
private fun ActionButton(icon: String, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, style = AgentWalletTypography.titleMedium)
            Text(label, style = AgentWalletTypography.labelMedium, color = TextSecondary)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewStrategy() {
    StrategyPanel(
        message = ChatMessage.StrategyCard(
            name = "ETH 新币狙击",
            chain = "Ethereum",
            status = StrategyStatus.RUNNING,
            runtime = "已运行 3 天 12 小时",
            trades = 8,
            pnl = "+$234.50"
        )
    )
}
