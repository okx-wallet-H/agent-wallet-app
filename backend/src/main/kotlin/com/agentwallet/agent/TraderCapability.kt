package com.agentwallet.agent

import kotlinx.serialization.Serializable

/**
 * Each AI trader has a set of capabilities it can combine to produce signals.
 * Capabilities are like "skills" — the trader chooses which to apply based on strategy.
 */
@Serializable
data class TraderCapability(
    val id: String,          // "trenches:scan"
    val name: String,        // "新币扫链"
    val icon: String,        // "🔍"
    val description: String, // "扫描链上新创建的代币"
    val appliesTo: List<String> // which data sources this uses: ["trenches","token"]
)

/** All available capabilities. Each trader picks a subset. */
object Capabilities {
    val scanNewTokens = TraderCapability("trenches:scan", "新币扫链", "🔍", "扫描链上新创建的代币合约", listOf("trenches"))
    val devReputation  = TraderCapability("trenches:dev", "开发者画像", "👤", "分析开发者历史：创建数、Rug率、存活率", listOf("trenches"))
    val tokenSecurity  = TraderCapability("token:security", "安全检查", "🛡️", "代币安全：蜜罐检测、持有者分布、LP锁定", listOf("token"))
    val smartMoney     = TraderCapability("signal:smart", "聪明钱追踪", "🧠", "跟踪链上高胜率钱包的实时买入", listOf("signal"))
    val whaleWatch     = TraderCapability("signal:whale", "巨鲸监控", "🐋", "监控大额转账和巨鲸建仓行为", listOf("signal"))
    val kolRadar       = TraderCapability("social:kol", "KOL 雷达", "📡", "监控 KOL 首次提及和讨论热度", listOf("social"))
    val sentiment      = TraderCapability("social:sentiment", "情绪分析", "📊", "社交媒体情绪：看多/看空比例、热度趋势", listOf("social"))
    val bundleCheck    = TraderCapability("token:bundle", "捆绑检测", "🔗", "检测代币是否被狙击工具捆绑买入", listOf("token"))
    val liquidityCheck = TraderCapability("token:liquidity", "流动性评估", "💧", "评估流动性深度和锁仓情况", listOf("token"))
    val priceTrend     = TraderCapability("market:trend", "趋势分析", "📈", "价格和成交量趋势分析", listOf("market"))

    val all = listOf(scanNewTokens, devReputation, tokenSecurity, smartMoney, whaleWatch, kolRadar, sentiment, bundleCheck, liquidityCheck, priceTrend)
}
