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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentwallet.data.PortfolioDto
import com.agentwallet.ui.theme.*

@Composable
fun PortfolioScreen(viewModel: ChatViewModel = viewModel()) {
    var data by remember { mutableStateOf<PortfolioDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    val uiState by viewModel.uiState.collectAsState()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    LaunchedEffect(Unit) { viewModel.loadPortfolio { data = it; loading = false } }
    LazyColumn(modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(horizontal = Spacing.screenHorizontal)) {
        item {
            Spacer(Modifier.height(Spacing.xl))
            // Wallet address with copy
            val addr = uiState.evmAddress ?: ""
            if (addr.isNotBlank()) {
                Text("钱包地址", style = AgentWalletTypography.bodySmall, color = TextTertiary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(addr, style = MonoSmallStyle.copy(color = TextPrimary), modifier = Modifier.weight(1f), maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    Text("📋", style = AgentWalletTypography.labelMedium, modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)).background(BackgroundTertiary).clickable { clipboard.setText(AnnotatedString(addr)) }.padding(8.dp))
                }
                Spacer(Modifier.height(Spacing.lg))
            }
            Text("总资产", style = AgentWalletTypography.bodySmall, color = TextTertiary)
            Spacer(Modifier.height(Spacing.xs))
            Text("$${String.format("%,.2f", data?.totalUsd ?: 0.0)}", style = NumberStyle, color = TextPrimary)
            if (data != null && data!!.dailyPnl != 0.0) { Spacer(Modifier.height(Spacing.xs)); Text("+$${String.format("%,.2f", data!!.dailyPnl)} 今日", style = AgentWalletTypography.bodyMedium, color = Profit) }
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
