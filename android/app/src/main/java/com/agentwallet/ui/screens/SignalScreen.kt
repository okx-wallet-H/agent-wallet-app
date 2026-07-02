package com.agentwallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentwallet.data.SignalDto
import com.agentwallet.ui.theme.*

@Composable
fun SignalScreen(viewModel: ChatViewModel = viewModel()) {
    var selectedFilter by remember { mutableStateOf("全部") }
    var signals by remember { mutableStateOf<List<SignalDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val filters = listOf("全部", "聪明钱", "KOL", "巨鲸")
    LaunchedEffect(Unit) { viewModel.loadSignals { signals = it; loading = false } }
    LazyColumn(modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(horizontal = Spacing.screenHorizontal)) {
        item {
            Spacer(Modifier.height(Spacing.xl))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("实时信号", style = AgentWalletTypography.headlineMedium, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(6.dp).clip(CircleShape).background(Profit)); Spacer(Modifier.width(Spacing.xs)); Text("实时", style = AgentWalletTypography.labelMedium, color = Profit) }
            }
            Spacer(Modifier.height(Spacing.md))
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                filters.forEach { f -> val sel = f == selectedFilter; Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if(sel)Accent else BackgroundSecondary).padding(horizontal=Spacing.md,vertical=Spacing.sm).clickable{selectedFilter=f}) { Text(f, style=AgentWalletTypography.labelMedium, color=if(sel)androidx.compose.ui.graphics.Color.White else TextSecondary) } }
            }
            Spacer(Modifier.height(Spacing.lg))
        }
        if (loading) { item { Text("加载中...", color = TextSecondary, modifier = Modifier.padding(Spacing.xl)) } }
        else if (signals.isEmpty()) { item { Text("暂无信号", color = TextTertiary, modifier = Modifier.padding(Spacing.xl)) } }
        else {
            val filtered = signals.filter { selectedFilter == "全部" || it.sourceType == selectedFilter }
            items(filtered) { s ->
                val cc = when(s.confidence){"高"->Profit;"中"->Warning;else->Loss}
                Column(Modifier.fillMaxWidth().clip(Shapes.card).background(BackgroundSecondary).padding(Spacing.lg)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(6.dp).clip(CircleShape).background(cc));Spacer(Modifier.width(Spacing.sm));Text(s.token,style=AgentWalletTypography.titleMedium,color=TextPrimary);Spacer(Modifier.width(Spacing.sm));Text(s.chain,style=AgentWalletTypography.labelMedium,color=TextTertiary)}
                        Text(s.timeAgo,style=AgentWalletTypography.labelMedium,color=TextTertiary)
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("${s.walletCount} 个${s.sourceType}买入",style=AgentWalletTypography.bodySmall,color=TextSecondary);Text("💰 ${s.amountUsd}",style=MonoSmallStyle,color=TextPrimary)}
                }
                Spacer(Modifier.height(Spacing.md))
            }
        }
        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}
