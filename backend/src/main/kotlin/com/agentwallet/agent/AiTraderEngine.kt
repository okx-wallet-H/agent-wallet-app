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
            id = "solana-sniper", name = "Solana 新币狙击手", emoji = "🔫",
            description = "7×24 扫描 Solana 新币，过滤 Rug 盘", chain = "501",
            dataSource = "trenches",
            capabilities = listOf("trenches:scan", "trenches:dev", "token:security", "token:bundle", "token:liquidity"),
            claudeFilter = "评分维度: Dev信誉(40%) + LP锁定(25%) + 捆绑检测(20%) + 流动性(15%)",
            stats = TraderStats(0, 0.0, 0.0, 0)
        ),
        TraderProfile(
            id = "smart-money", name = "聪明钱追踪", emoji = "🧠",
            description = "跟踪链上高胜率聪明钱地址", chain = "501",
            dataSource = "signal",
            capabilities = listOf("signal:smart", "token:security", "market:trend"),
            claudeFilter = "评分维度: 信号源胜率(35%) + 金额规模(25%) + 安全检测(25%) + 趋势(15%)",
            stats = TraderStats(0, 0.0, 0.0, 0)
        ),
        TraderProfile(
            id = "kol-radar", name = "KOL 雷达", emoji = "📡",
            description = "监控 KOL 首次提及的代币", chain = "1",
            dataSource = "social",
            capabilities = listOf("social:kol", "social:sentiment", "token:security"),
            claudeFilter = "评分维度: 首次提及时间(30%) + KOL数量(25%) + 情绪(25%) + 安全(20%)",
            stats = TraderStats(0, 0.0, 0.0, 0)
        ),
        TraderProfile(
            id = "whale-watcher", name = "巨鲸动向", emoji = "🐋",
            description = "追踪巨鲸大额转账和买入", chain = "1",
            dataSource = "signal", walletType = "3",
            capabilities = listOf("signal:whale", "token:security", "market:trend"),
            claudeFilter = "评分维度: 金额规模(40%) + 历史胜率(30%) + 安全检测(20%) + 趋势(10%)",
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
        val signals = try {
            when (trader.dataSource) {
                "signal" -> signalService.getSignals(trader.chain, if (trader.id == "whale-watcher") "3" else "1")
                    .map { s -> mapOf("token" to s.tokenSymbol, "amount" to s.amountUsd, "wallets" to s.triggerWallets.size.toString(), "soldRatio" to s.soldRatio) }
                else -> emptyList()
            }
        } catch (e: Exception) {
            logger.warn("Signal fetch failed for ${trader.name}: ${e.message}")
            return emptyList()
        }

        if (signals.isEmpty()) {
            if (java.lang.System.currentTimeMillis() % 300_000 < 60_000) // log every 5 min
                logger.info("Trader [${trader.name}] fetched 0 raw signals from OKX")
            return emptyList()
        }
        logger.info("Trader [${trader.name}] fetched ${signals.size} raw signals, analyzing with Claude...")

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
    val dataSource: String,
    val walletType: String = "1",
    val model: String = "claude-sonnet-4-6",
    val scanIntervalMs: Long = 60_000,
    val capabilities: List<String> = emptyList(),  // capability IDs this trader uses
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
