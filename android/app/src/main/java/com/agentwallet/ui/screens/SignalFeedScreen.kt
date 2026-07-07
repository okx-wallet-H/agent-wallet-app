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
import com.agentwallet.data.ApiClient
import com.agentwallet.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class LiveSignal(val traderEmoji: String, val traderName: String, val token: String, val confidence: String, val reason: String, val action: String, val timestamp: Long)

@Composable
fun SignalFeedScreen() {
    var signals by remember { mutableStateOf<List<LiveSignal>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        try {
            val api = ApiClient()
            val resp = api.getSignals2()
            val arr = resp["signals"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())
            signals = arr.map { t -> val o=t.jsonObject; LiveSignal(o["traderEmoji"]!!.jsonPrimitive.content, o["traderName"]!!.jsonPrimitive.content, o["token"]!!.jsonPrimitive.content, o["confidence"]!!.jsonPrimitive.content, o["reason"]!!.jsonPrimitive.content, o["action"]!!.jsonPrimitive.content, o["timestamp"]!!.jsonPrimitive.content.toLong()) }
        } catch (e: Exception) {}
        loading = false
    }
    LazyColumn(Modifier.fillMaxSize().background(BackgroundPrimary).padding(horizontal = Spacing.screenHorizontal)) {
        item { Spacer(Modifier.height(Spacing.xl)); Text("实时信号", style = AgentWalletTypography.headlineMedium, color = TextPrimary); Spacer(Modifier.height(Spacing.lg)) }
        if (loading) item { Text("加载中...", color = TextSecondary) }
        else if (signals.isEmpty()) item { Text("暂无信号，AI 交易员分析中...", color = TextTertiary) }
        else items(signals) { s ->
            val confColor = when(s.confidence){"high"->Profit;"medium"->Warning;else->Loss}
            Column(Modifier.fillMaxWidth().clip(Shapes.card).background(BackgroundSecondary).padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(s.traderEmoji, style = AgentWalletTypography.titleMedium); Spacer(Modifier.width(Spacing.sm)); Column(Modifier.weight(1f)) { Text(s.traderName, style = AgentWalletTypography.labelMedium, color = TextTertiary); Text(s.token, style = AgentWalletTypography.titleMedium, color = TextPrimary) } }
                Spacer(Modifier.height(Spacing.sm))
                Text(s.reason, style = AgentWalletTypography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(Spacing.sm))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(s.confidence.uppercase(), style = AgentWalletTypography.labelMedium, color = confColor)
                    Text(if(s.action=="buy") "建议买入" else "观望", style = AgentWalletTypography.labelMedium, color = TextTertiary)
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}
