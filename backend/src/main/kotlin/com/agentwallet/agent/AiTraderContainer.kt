package com.agentwallet.agent

import com.agentwallet.services.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Standalone AI Trader container.
 * Each instance runs ONE trader strategy 24/7 in its own process.
 * Independent knowledge base, Claude session, and signal output.
 */
class AiTraderContainer(
    private val profile: TraderProfile,
    private val llmClient: LlmClient,
    private val signalService: OkxSignalService,
    private val socialService: OkxSocialService?,
    private val knowledgeDir: String = "/opt/trader-knowledge"
) {
    private val logger = LoggerFactory.getLogger("Trader-${profile.id}")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Per-trader knowledge base (file-based for MVP, migrate to vector DB)
    private val kbFile = File("$knowledgeDir/${profile.id}.jsonl")
    private val tradeHistory = mutableListOf<TraderTradeRecord>()
    private val publishedSignals = mutableListOf<TraderSignal>()
    private val signalQuality = ConcurrentHashMap<String, SignalQuality>()

    /**
     * Main loop — runs forever, analyzing data and publishing signals.
     */
    suspend fun run(scope: CoroutineScope) {
        val isActive = scope.isActive
        logger.info("========================================")
        logger.info("AI Trader [${profile.emoji} ${profile.name}] STARTED")
        logger.info("Strategy: ${profile.description}")
        logger.info("Chain: ${profile.chain} | Source: ${profile.dataSource}")
        logger.info("========================================")

        loadKnowledge()

        while (isActive) {
            try {
                // 1. Fetch raw data from OKX
                val rawData = fetchRawData()
                if (rawData.isEmpty()) { delay(60_000); continue }

                // 2. Claude deep analysis with historical context
                val signals = analyzeWithClaude(rawData)
                if (signals.isEmpty()) { delay(60_000); continue }

                // 3. Publish quality signals
                signals.forEach { signal ->
                    publishedSignals.add(signal)
                    logger.info("📡 SIGNAL: ${signal.token} | ${signal.confidence} | ${signal.reason}")
                    // In production: publish to Redis/pubsub for the main API
                }

                // 4. Learn from past outcomes
                learnFromHistory()

                // 5. Persist knowledge
                saveKnowledge()

            } catch (e: Exception) {
                logger.error("Trader loop error: ${e.message}", e)
            }

            delay(profile.scanIntervalMs)
        }
    }

    /** Fetch raw data based on trader's data source — each trader has unique skill set */
    private suspend fun fetchRawData(): List<Map<String, String>> {
        return try {
            when (profile.dataSource) {
                "signal" -> {
                    // Smart money / whale signals with security analysis
                    val signals = signalService.getSignals(profile.chain, profile.walletType)
                    signals.map { s ->
                        val analysis = try { signalService.getTokenAnalysis(profile.chain, s.tokenAddress) } catch (e: Exception) { null }
                        mapOf("token" to s.tokenSymbol, "amount" to s.amountUsd,
                            "wallets" to s.triggerWallets.size.toString(), "soldRatio" to s.soldRatio,
                            "marketCap" to s.marketCapUsd, "riskLevel" to (analysis?.riskControlLevel ?: "unknown"),
                            "holders" to (analysis?.holders?.toString() ?: "?"), "bundlePct" to (analysis?.bundleHoldingRatio?.toString() ?: "?"),
                            "lpLocked" to (analysis?.liquidityLocked?.toString() ?: "?"))
                    }
                }
                "trenches" -> {
                    // New token scanning with dev reputation
                    val signals = signalService.getSignals(profile.chain)
                    signals.take(10).map { s ->
                        val dev = try { signalService.getDevProfile(profile.chain, s.triggerWallets.firstOrNull() ?: "") } catch (e: Exception) { null }
                        val analysis = try { signalService.getTokenAnalysis(profile.chain, s.tokenAddress) } catch (e: Exception) { null }
                        mapOf("token" to s.tokenSymbol, "amount" to s.amountUsd,
                            "marketCap" to s.marketCapUsd, "devLaunches" to (dev?.totalLaunches?.toString() ?: "?"),
                            "devRugCount" to (dev?.rugCount?.toString() ?: "?"), "devRugHistory" to (dev?.rugHistory?.toString() ?: "?"),
                            "riskLevel" to (analysis?.riskControlLevel ?: "unknown"),
                            "bundlePct" to (analysis?.bundleHoldingRatio?.toString() ?: "?"),
                            "lpLocked" to (analysis?.liquidityLocked?.toString() ?: "?"))
                    }
                }
                "social" -> {
                    // KOL first mention + sentiment tracking
                    val signals = signalService.getSignals(profile.chain)
                    signals.take(5).mapNotNull { s ->
                        val sentiment = try { socialService?.getSentiment(s.tokenAddress, profile.chain) } catch (e: Exception) { null } ?: return@mapNotNull null
                        mapOf("token" to s.tokenSymbol, "amount" to s.amountUsd,
                            "mentions24h" to sentiment.mentionCount24h.toString(),
                            "bullishRatio" to "%.0f".format(sentiment.bullishRatio * 100),
                            "firstMentionedBy" to (sentiment.firstMentionedBy ?: "unknown"),
                            "topKols" to sentiment.topKols.joinToString(","))
                    }
                }
                else -> signalService.getSignals(profile.chain).map {
                    mapOf("token" to it.tokenSymbol, "amount" to it.amountUsd) }
            }
        } catch (e: Exception) {
            logger.warn("Data fetch failed: ${e.message}")
            emptyList()
        }
    }

    /** Claude analysis with full historical context */
    private suspend fun analyzeWithClaude(data: List<Map<String, String>>): List<TraderSignal> {
        if (data.isEmpty()) return emptyList()

        // Build context from knowledge base
        val context = buildClaudeContext(data)
        val prompt = """
${profile.claudeFilter}

## 历史战绩
近期信号: ${tradeHistory.size} 条 | 胜率: ${"%.0f".format(currentWinRate() * 100)}%
已过滤的噪音代币: ${signalQuality.filter { it.value.isNoise }.size} 个

## 当前数据
${json.encodeToString(data.take(5))}

分析并返回 JSON 数组: [{"token":"...","confidence":"high|medium|low","reason":"...","action":"buy|skip"}]
""".trimIndent()

        return try {
            val response = llmClient.chat(listOf(LlmMessage("user", prompt)), model = profile.model)
            parseClaudeResponse(response.content ?: "")
        } catch (e: Exception) {
            logger.warn("Claude analysis failed: ${e.message}")
            emptyList()
        }
    }

    private fun buildClaudeContext(data: List<Map<String, String>>): String {
        val sb = StringBuilder()
        sb.appendLine("## 已过滤噪音源")
        signalQuality.filter { it.value.isNoise }.forEach { (source, _) ->
            sb.appendLine("- $source")
        }
        sb.appendLine("\n## 最近 5 笔交易")
        tradeHistory.takeLast(5).forEach { t ->
            sb.appendLine("- ${t.token}: ${t.outcome} (${t.reason})")
        }
        return sb.toString()
    }

    private fun parseClaudeResponse(text: String): List<TraderSignal> {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']') + 1
        if (start < 0 || end <= start) return emptyList()
        return json.parseToJsonElement(text.substring(start, end)).jsonArray.map { item ->
            val obj = item.jsonObject
            TraderSignal(profile.id, profile.name, profile.emoji,
                obj["token"]?.jsonPrimitive?.content ?: "?",
                obj["confidence"]?.jsonPrimitive?.content ?: "medium",
                obj["reason"]?.jsonPrimitive?.content ?: "",
                obj["action"]?.jsonPrimitive?.content ?: "skip",
                System.currentTimeMillis())
        }
    }

    /** Learn from trade outcomes — update quality scores */
    private fun learnFromHistory() {
        tradeHistory.filter { it.outcome != "pending" }.forEach { trade ->
            val quality = signalQuality.getOrPut(trade.signalSource ?: "unknown") { SignalQuality() }
            if (trade.outcome == "win") quality.wins++
            else if (trade.outcome == "loss") quality.losses++
            else if (trade.outcome == "rug") { quality.isNoise = true; quality.losses++ }
        }
    }

    private fun currentWinRate(): Double {
        val closed = tradeHistory.filter { it.outcome != "pending" }
        if (closed.isEmpty()) return 0.0
        return closed.count { it.outcome == "win" }.toDouble() / closed.size
    }

    private fun loadKnowledge() {
        if (kbFile.exists()) {
            kbFile.readLines().forEach { line ->
                try { tradeHistory.add(json.decodeFromString<TraderTradeRecord>(line)) } catch (e: Exception) {}
            }
        }
        logger.info("Knowledge loaded: ${tradeHistory.size} trade records")
    }

    private fun saveKnowledge() {
        kbFile.parentFile.mkdirs()
        kbFile.writeText(tradeHistory.joinToString("\n") { json.encodeToString(it) })
    }

    fun getSignals() = publishedSignals.toList()
    fun getStats() = TraderStats(tradeHistory.size, currentWinRate(),
        tradeHistory.sumOf { it.pnl }, 0)
}

@kotlinx.serialization.Serializable
data class TraderTradeRecord(
    val token: String, val action: String, val amount: Double,
    val price: Double, val outcome: String, val pnl: Double,
    val signalSource: String?, val reason: String, val timestamp: Long
)

data class SignalQuality(var wins: Int = 0, var losses: Int = 0, var isNoise: Boolean = false)
