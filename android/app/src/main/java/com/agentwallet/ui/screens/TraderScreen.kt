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
import com.agentwallet.data.ApiClient
import com.agentwallet.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class TraderItem(val id: String, val name: String, val emoji: String, val description: String, val winRate: Double, val totalPnl: Double, val followers: Int)

@Composable
fun TraderScreen() {
    var traders by remember { mutableStateOf<List<TraderItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val api = ApiClient()
            val resp = api.getTraders()
            val arr = resp["traders"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())
            traders = arr.map { t ->
                val o = t.jsonObject; val s = o["stats"]!!.jsonObject
                TraderItem(o["id"]!!.jsonPrimitive.content, o["name"]!!.jsonPrimitive.content, o["emoji"]!!.jsonPrimitive.content, o["description"]!!.jsonPrimitive.content, s["winRate"]!!.jsonPrimitive.content.toDouble(), s["totalPnl"]!!.jsonPrimitive.content.toDouble(), s["followers"]!!.jsonPrimitive.content.toInt())
            }
        } catch (e: Exception) {}
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().background(BackgroundPrimary).padding(horizontal = Spacing.screenHorizontal)) {
        item { Spacer(Modifier.height(Spacing.xl)); Text("AI 交易员广场", style = AgentWalletTypography.headlineMedium, color = TextPrimary); Spacer(Modifier.height(Spacing.sm)); Text("7×24 分析 · 战绩公开 · 一键跟单", style = AgentWalletTypography.bodySmall, color = TextSecondary); Spacer(Modifier.height(Spacing.lg)) }
        if (loading) item { Text("加载中...", color = TextSecondary, modifier = Modifier.padding(Spacing.xl)) }
        else if (traders.isEmpty()) item { Text("AI 交易员启动中...", color = TextTertiary, modifier = Modifier.padding(Spacing.xl)) }
        else items(traders) { t ->
            val pnlColor = if (t.totalPnl >= 0) Profit else Loss
            Column(Modifier.fillMaxWidth().clip(Shapes.card).background(BackgroundSecondary).padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(44.dp).clip(CircleShape).background(AccentSubtle), contentAlignment = Alignment.Center) { Text(t.emoji, style = AgentWalletTypography.headlineMedium) }; Spacer(Modifier.width(Spacing.md)); Column(Modifier.weight(1f)) { Text(t.name, style = AgentWalletTypography.titleMedium, color = TextPrimary); Text(t.description, style = AgentWalletTypography.bodySmall, color = TextSecondary, maxLines = 1) } }
                Spacer(Modifier.height(Spacing.md))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column { Text("胜率", style = AgentWalletTypography.labelMedium, color = TextTertiary); Text("${"%.0f".format(t.winRate * 100)}%", style = AgentWalletTypography.titleMedium, color = Profit) }
                    Column { Text("盈亏", style = AgentWalletTypography.labelMedium, color = TextTertiary); Text("${"$%.0f".format(t.totalPnl)}", style = AgentWalletTypography.titleMedium, color = pnlColor) }
                    Column { Text("跟单", style = AgentWalletTypography.labelMedium, color = TextTertiary); Text("${t.followers}人", style = AgentWalletTypography.titleMedium, color = TextPrimary) }
                }
                Spacer(Modifier.height(Spacing.md))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Accent).padding(12.dp), contentAlignment = Alignment.Center) { Text("跟单 →", style = AgentWalletTypography.bodyMedium, color = TextOnAccent) }
            }
            Spacer(Modifier.height(Spacing.md))
        }
        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}
