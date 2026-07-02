package com.agentwallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.theme.Accent
import com.agentwallet.ui.theme.AgentWalletTypography
import com.agentwallet.ui.theme.BackgroundPrimary
import com.agentwallet.ui.theme.BackgroundSecondary
import com.agentwallet.ui.theme.BackgroundTertiary
import com.agentwallet.ui.theme.BorderSubtle
import com.agentwallet.ui.theme.Loss
import com.agentwallet.ui.theme.Profit
import com.agentwallet.ui.theme.Shapes
import com.agentwallet.ui.theme.Spacing
import com.agentwallet.ui.theme.TextPrimary
import com.agentwallet.ui.theme.TextSecondary
import com.agentwallet.ui.theme.TextTertiary
import com.agentwallet.ui.theme.Warning

data class SignalItem(
    val token: String,
    val chain: String,
    val amountUsd: String,
    val walletCount: Int,
    val sourceType: String,
    val timeAgo: String,
    val confidence: String
)

@Composable
fun SignalScreen(modifier: Modifier = Modifier) {
    var selectedFilter by remember { mutableStateOf("全部") }
    val filters = listOf("全部", "聪明钱", "KOL", "巨鲸", "新币")

    val signals = listOf(
        SignalItem("NEWPUMP", "Solana", "$12,500", 3, "聪明钱", "刚刚", "高"),
        SignalItem("MIDCOIN", "Ethereum", "$45,200", 1, "巨鲸", "5 分钟前", "中"),
        SignalItem("MEME2", "Solana", "$8,900", 2, "聪明钱", "12 分钟前", "高"),
        SignalItem("SAFECOIN", "Ethereum", "$15,600", 4, "KOL", "25 分钟前", "中"),
        SignalItem("RUGCHECK", "Base", "$3,200", 1, "新币", "30 分钟前", "低")
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("实时信号", style = AgentWalletTypography.headlineMedium, color = TextPrimary)
                // Live indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Profit)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text("实时", style = AgentWalletTypography.labelMedium, color = Profit)
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                filters.forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Accent else BackgroundSecondary)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                            .clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            style = AgentWalletTypography.labelMedium,
                            color = if (isSelected) androidx.compose.ui.graphics.Color.White else TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }

        // Signal list
        items(signals) { signal ->
            SignalListItem(signal)
            Spacer(Modifier.height(Spacing.md))
        }

        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}

@Composable
private fun SignalListItem(signal: SignalItem) {
    val confidenceColor = when (signal.confidence) {
        "高" -> Profit
        "中" -> Warning
        else -> Loss
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .background(BackgroundSecondary)
            .padding(Spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Confidence dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(confidenceColor)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(signal.token, style = AgentWalletTypography.titleMedium, color = TextPrimary)
                Spacer(Modifier.width(Spacing.sm))
                Text(signal.chain, style = AgentWalletTypography.labelMedium, color = TextTertiary)
            }
            Text(signal.timeAgo, style = AgentWalletTypography.labelMedium, color = TextTertiary)
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${signal.walletCount} 个${signal.sourceType}买入",
                style = AgentWalletTypography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "💰 ${signal.amountUsd}",
                style = com.agentwallet.ui.theme.MonoSmallStyle,
                color = TextPrimary
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewSignal() {
    SignalScreen()
}
