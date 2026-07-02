package com.agentwallet.ui.screens

import androidx.compose.foundation.background
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
import com.agentwallet.data.StrategyDto
import com.agentwallet.ui.theme.*

@Composable
fun StrategyScreen(viewModel: ChatViewModel = viewModel()) {
    var strategies by remember { mutableStateOf<List<StrategyDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { viewModel.loadStrategies { strategies = it; loading = false } }
    LazyColumn(modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(horizontal = Spacing.screenHorizontal)) {
        item {
            Spacer(Modifier.height(Spacing.xl))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("我的策略", style = AgentWalletTypography.headlineMedium, color = TextPrimary)
                Box(Modifier.clip(CircleShape).background(Accent).size(36.dp), contentAlignment = Alignment.Center) { Text("+", style = AgentWalletTypography.titleMedium, color = androidx.compose.ui.graphics.Color.White) }
            }
            Spacer(Modifier.height(Spacing.lg))
        }
        if (loading) { item { Text("加载中...", color = TextSecondary, modifier = Modifier.padding(Spacing.xl)) } }
        else if (strategies.isEmpty()) { item { Text("暂无策略，去对话页创建一个吧", color = TextTertiary, modifier = Modifier.padding(Spacing.xl)) } }
        else {
            items(strategies) { s ->
                val dotColor = when(s.status){"RUNNING"->Profit;"PAUSED"->Warning;else->Loss}
                val statusLabel = when(s.status){"RUNNING"->"● 运行中";"PAUSED"->"○ 暂停中";else->"⏹ 已停止"}
                Column(Modifier.fillMaxWidth().clip(Shapes.card).background(BackgroundSecondary).padding(Spacing.lg)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor));Spacer(Modifier.width(Spacing.sm));Text("${s.name} · ${s.chain}",style=AgentWalletTypography.titleMedium,color=TextPrimary)}
                        Text(statusLabel,style=AgentWalletTypography.labelMedium,color=dotColor)
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text("${s.trades} 笔交易 · ${"$%.2f".format(s.pnl)}",style=AgentWalletTypography.bodySmall,color=TextSecondary)
                    Spacer(Modifier.height(Spacing.md))
                    Row(Modifier.fillMaxWidth(),Arrangement.SpaceEvenly){listOf("⏸ 暂停","✏️ 修改","⏹ 停止").forEach{Text(it,style=AgentWalletTypography.labelMedium,color=TextSecondary,modifier=Modifier.clip(RoundedCornerShape(8.dp)).background(BackgroundTertiary).padding(horizontal=Spacing.md,vertical=Spacing.sm))}}
                }
                Spacer(Modifier.height(Spacing.md))
            }
        }
        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}
