package com.agentwallet.agent

import com.agentwallet.services.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * AI Trader Engine — manages 7×24 trading agents that analyze OKX data
 * and publish high-quality signals for users to copy-trade.
 */
class AiTraderEngine(
    private val llmClient: LlmClient,
    private val signalService: OkxSignalService,
    private val okxTokenService: OkxSignalService
) {
    private val logger = LoggerFactory.getLogger(AiTraderEngine::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val publishedSignals = ConcurrentHashMap<String, MutableList<TraderSignal>>()
    private val followerCounts = ConcurrentHashMap<String, Int>()

    val traders = listOf(
        TraderProfile(
            id = "solana-sniper",
            name = "Solana 新币狙击手",
            emoji = "🔫",
            description = "7×24 扫描 Solana 新币，过滤 Rug 盘，狙击优质早期项目",
            chain = "501",
            dataSource = "trenches",
            claudeFilter = "过滤条件: Dev无Rug历史, 捆绑<30%, LP已锁定, 流动性>$5000",
            stats = TraderStats(0, 0.0, 0.0, 0)
        ),
        TraderProfile(
            id = "smart-money",
            name = "聪明钱追踪",
            emoji = "🧠",
            description = "跟踪链上高胜率聪明钱地址，实时跟单优质交易",
            chain = "501",
            dataSource = "signal",
            claudeFilter = "过滤条件: 信号源胜率>60%, 单笔金额>$5000, 已卖出比例<20%",
            stats = TraderStats(0, 0.0, 0.0, 0)
        ),
        TraderProfile(
            id = "kol-radar",
            name = "KOL 雷达",
            emoji = "📡",
            description = "监控 KOL 首次提及的代币，抢占信息差先机",
            chain = "1",
            dataSource = "social",
            claudeFilter = "过滤条件: 首次提及<30分钟, 至少2个KOL同时讨论, 代币市值<$10M",
            stats = TraderStats(0, 0.0, 0.0, 0)
        ),
        TraderProfile(
            id = "whale-watcher",
            name = "巨鲸动向",
            emoji = "🐋",
            description = "追踪巨鲸大额转账和买入，发现主力资金动向",
            chain = "1",
            dataSource = "signal",
            claudeFilter = "过滤条件: 巨鲸类型, 金额>$50000, 代币市值<$100M",
            stats = TraderStats(0, 0.0, 0.0, 0)
        )
    )

    /** Start all AI traders in background */
    fun startAll(scope: CoroutineScope) {
        traders.forEach { trader ->
            scope.launch(Dispatchers.IO) {
                logger.info("AI Trader [${trader.name}] started")
                while (isActive) {
                    try {
                        val signals = fetchAndFilter(trader)
                        if (signals.isNotEmpty()) {
                            publishedSignals.getOrPut(trader.id) { mutableListOf() }.addAll(signals)
                            logger.info("AI Trader [${trader.name}] published ${signals.size} signals")
                        }
                    } catch (e: Exception) {
                        logger.warn("AI Trader [${trader.name}] error: ${e.message}")
                    }
                    delay(60_000) // Check every 60 seconds
                }
            }
        }
        logger.info("All ${traders.size} AI Traders started")
    }

    /** Fetch OKX data and run Claude filter */
    private suspend fun fetchAndFilter(trader: TraderProfile): List<TraderSignal> {
        val signals = when (trader.dataSource) {
            "signal" -> signalService.getSignals(trader.chain, if (trader.id == "whale-watcher") "3" else "1")
                .map { s -> mapOf("token" to s.tokenSymbol, "amount" to s.amountUsd, "wallets" to s.triggerWallets.size.toString(), "soldRatio" to s.soldRatio) }
            else -> emptyList()
        }

        if (signals.isEmpty()) return emptyList()

        // Use Claude to filter and score
        val prompt = """
你是「${trader.name}」。${trader.description}
${trader.claudeFilter}

以下是最近获取的信号数据（JSON 格式）：
${json.encodeToString(signals.take(5))}

请分析并返回最多 3 个高质量信号。对每个信号，给出：
1. token 名称
2. confidence (high/medium/low)
3. 一句话理由
4. 建议操作 (buy/skip)

只返回 JSON 数组，格式：[{"token":"...","confidence":"...","reason":"...","action":"..."}]
""".trimIndent()

        try {
            val response = llmClient.chat(
                messages = listOf(LlmMessage("user", prompt)),
                model = "claude-sonnet-4-6"
            )
            val text = response.content ?: return emptyList()
            // Extract JSON from Claude's response
            val jsonStart = text.indexOf('[')
            val jsonEnd = text.lastIndexOf(']') + 1
            if (jsonStart < 0 || jsonEnd <= jsonStart) return emptyList()

            val jsonArray = json.parseToJsonElement(text.substring(jsonStart, jsonEnd)).jsonArray
            return jsonArray.map { item ->
                val obj = item.jsonObject
                TraderSignal(
                    traderId = trader.id,
                    traderName = trader.name,
                    traderEmoji = trader.emoji,
                    token = obj["token"]?.jsonPrimitive?.content ?: "?",
                    confidence = obj["confidence"]?.jsonPrimitive?.content ?: "medium",
                    reason = obj["reason"]?.jsonPrimitive?.content ?: "",
                    action = obj["action"]?.jsonPrimitive?.content ?: "skip",
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            logger.warn("Claude filter failed for ${trader.name}: ${e.message}")
            return emptyList()
        }
    }

    fun getSignals(traderId: String? = null): List<TraderSignal> {
        return if (traderId != null) publishedSignals[traderId]?.toList() ?: emptyList()
        else publishedSignals.values.flatten().sortedByDescending { it.timestamp }.take(20)
    }

    fun incrementFollowers(traderId: String) {
        followerCounts.merge(traderId, 1, Int::plus)
    }

    fun getTraderStats(traderId: String): TraderStats {
        val signals = publishedSignals[traderId] ?: return TraderStats(0, 0.0, 0.0, 0)
        val won = signals.count { it.outcome == "win" }
        val total = signals.count { it.outcome != null }
        return TraderStats(
            totalSignals = signals.size,
            winRate = if (total > 0) won.toDouble() / total else 0.0,
            totalPnl = signals.sumOf { it.pnl ?: 0.0 },
            followers = followerCounts.getOrDefault(traderId, 0)
        )
    }
}

@Serializable
data class TraderProfile(
    val id: String, val name: String, val emoji: String,
    val description: String, val chain: String,
    val dataSource: String,  // "trenches" | "signal" | "social" | "token"
    val claudeFilter: String,
    val stats: TraderStats
)

@Serializable
data class TraderStats(
    val totalSignals: Int, val winRate: Double,
    val totalPnl: Double, val followers: Int
)

@Serializable
data class TraderSignal(
    val traderId: String, val traderName: String, val traderEmoji: String,
    val token: String, val confidence: String, val reason: String,
    val action: String, val timestamp: Long,
    var outcome: String? = null, var pnl: Double? = null
)
