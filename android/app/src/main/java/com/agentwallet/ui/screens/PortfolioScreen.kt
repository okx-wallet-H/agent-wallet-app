package com.agentwallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentwallet.data.PortfolioDto
import com.agentwallet.ui.theme.*

@Composable
fun PortfolioScreen(viewModel: ChatViewModel = viewModel()) {
    var data by remember { mutableStateOf<PortfolioDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { viewModel.loadPortfolio { data = it; loading = false } }
    LazyColumn(modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(horizontal = Spacing.screenHorizontal)) {
        item {
            Spacer(Modifier.height(Spacing.xl))
            Text("总资产", style = AgentWalletTypography.bodySmall, color = TextTertiary)
            Spacer(Modifier.height(Spacing.xs))
            Text("$${String.format("%,.2f", data?.totalUsd ?: 0.0)}", style = NumberStyle, color = TextPrimary)
            Spacer(Modifier.height(Spacing.xs))
            if (data != null) Text("+$${String.format("%,.2f", data!!.dailyPnl)} (${data!!.dailyPnlPercent}%) 今日", style = AgentWalletTypography.bodyMedium, color = Profit)
            Spacer(Modifier.height(Spacing.xl))
        }
        if (loading) { item { Text("加载中...", color = TextSecondary, modifier = Modifier.padding(Spacing.xl)) } }
        else if (data == null || data!!.tokens.isEmpty()) { item { Text("暂无资产", color = TextTertiary, modifier = Modifier.padding(Spacing.xl)) } }
        else {
            item { Text("持仓", style = AgentWalletTypography.titleMedium, color = TextPrimary); Spacer(Modifier.height(Spacing.sm)) }
            items(data!!.tokens) { t ->
                Row(Modifier.fillMaxWidth().clip(Shapes.card).background(BackgroundSecondary).padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(BackgroundTertiary), contentAlignment = Alignment.Center) { Text(t.symbol.take(1), style = AgentWalletTypography.titleMedium, color = TextPrimary) }
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f)) { Text(t.symbol, style = AgentWalletTypography.bodyMedium, color = TextPrimary); Text("${t.amount}", style = AgentWalletTypography.bodySmall, color = TextSecondary) }
                    Column(horizontalAlignment = Alignment.End) { Text("$${String.format("%,.2f", t.usdValue)}", style = MonoSmallStyle.copy(color = TextPrimary)); Text("${if(t.change24h>0)"+" else ""}${t.change24h}%", style = AgentWalletTypography.labelMedium, color = if(t.change24h>=0)Profit else Loss) }
                }
                Spacer(Modifier.height(Spacing.sm))
            }
        }
        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}
