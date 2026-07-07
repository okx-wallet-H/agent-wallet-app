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
    private val publishedSignals = mutableListOf<RatedSignal>()
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
                    logger.info("📡 ${"⭐".repeat(signal.stars)} ${signal.token} | ${signal.aiOpinion}")
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

    /** Claude analysis producing star-rated signals with dimension scores */
    private suspend fun analyzeWithClaude(data: List<Map<String, String>>): List<RatedSignal> {
        if (data.isEmpty()) return emptyList()

        val prompt = """
你是「${profile.name}」（${profile.description}）。
评分维度权重: ${profile.claudeFilter}
使用的能力: ${profile.capabilities.joinToString(", ")}

## 历史战绩
总信号: ${tradeHistory.size} | 胜率: ${"%.0f".format(currentWinRate() * 100)}%

## 当前数据
${json.encodeToString(data.take(10))}

请分析并返回 JSON 数组。每个信号必须包含:
- token: 代币符号
- stars: 1-5 星评分
- confidence: high/medium/low
- dimensions: 评分维度数组 [{capabilityId, label, score(0-100), detail}]
- aiOpinion: 一句话 AI 分析观点
- action: buy/watch/skip

返回格式: [{"token":"...","stars":4,"confidence":"high","dimensions":[{"capabilityId":"trenches:dev","label":"开发者信誉","score":85,"detail":"该Dev创建过5个项目，0 Rug"}],"aiOpinion":"...","action":"buy"}]
""".trimIndent()

        return try {
            val response = llmClient.chat(listOf(LlmMessage("user", prompt)), model = profile.model)
            parseRatedResponse(response.content ?: "")
        } catch (e: Exception) {
            logger.warn("Claude analysis failed: ${e.message}")
            emptyList()
        }
    }

    private fun parseRatedResponse(text: String): List<RatedSignal> {
        val start = text.indexOf('['); val end = text.lastIndexOf(']') + 1
        if (start < 0 || end <= start) return emptyList()
        return json.parseToJsonElement(text.substring(start, end)).jsonArray.map { item ->
            val o = item.jsonObject
            val dims = o["dimensions"]?.jsonArray?.map { d -> val dobj=d.jsonObject
                SignalDimension(dobj["capabilityId"]!!.jsonPrimitive.content, dobj["label"]!!.jsonPrimitive.content, dobj["score"]!!.jsonPrimitive.content.toInt(), dobj["detail"]!!.jsonPrimitive.content)
            } ?: emptyList()
            RatedSignal(traderId=profile.id, traderName=profile.name, traderEmoji=profile.emoji, token=o["token"]?.jsonPrimitive?.content ?: "?", chain=profile.chain, stars=o["stars"]?.jsonPrimitive?.content?.toIntOrNull() ?: computeStars(dims), confidence=o["confidence"]?.jsonPrimitive?.content ?: "medium", dimensions=dims, aiOpinion=o["aiOpinion"]?.jsonPrimitive?.content ?: "", suggestedAction=o["action"]?.jsonPrimitive?.content ?: "watch")
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
