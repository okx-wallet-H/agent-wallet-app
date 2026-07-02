package com.agentwallet.agent

import com.agentwallet.plugins.DatabaseFactory
import com.agentwallet.services.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Background agent that listens to OKX WebSocket 24/7.
 * Filters noise, publishes high-confidence signals to Redis.
 */
class SignalAgent(
    private val signalService: OkxSignalService
) {
    private val logger = LoggerFactory.getLogger(SignalAgent::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Start 7×24 signal monitoring. Runs in a background coroutine.
     * Signals flow: OKX WS → filter → Redis pub/sub → AnalysisAgent → ExecutionAgent
     */
    fun start(scope: CoroutineScope, vararg chainIds: String = arrayOf("501", "1")) {
        chainIds.forEach { chainId ->
            scope.launch(Dispatchers.IO) {
                logger.info("SignalAgent started for chain $chainId")
                var consecutiveErrors = 0

                while (isActive) {
                    try {
                        signalService.signalStream(chainId).collect { signal ->
                            consecutiveErrors = 0
                            val analyzed = quickFilter(signal)
                            if (analyzed != null) {
                                // Push to Redis list for downstream agents
                                val redis = DatabaseFactory.redis()
                                if (redis != null) {
                                    val payload = json.encodeToString(analyzed)
                                    redis.resource.use { it.lpush("signal:new", payload) }
                                }
                                logger.info("📡 High-conf signal: ${analyzed.tokenSymbol} (${analyzed.amountUsd})")
                            }
                        }
                    } catch (e: Exception) {
                        consecutiveErrors++
                        val delay = minOf(1000L * (1 shl minOf(consecutiveErrors, 5)), 30000L)
                        logger.warn("SignalAgent error (attempt $consecutiveErrors), reconnecting in ${delay}ms: ${e.message}")
                        delay(delay)
                    }
                }
            }
        }
    }

    /** Quick filter: reject low-value / already-rug-pulled signals. */
    private suspend fun quickFilter(signal: SignalData): SignalData? {
        val amount = signal.amountUsd.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
        if (amount < 1000) return null  // too small
        if (signal.soldRatio.toDoubleOrNull() ?: 100.0 > 30.0) return null  // already dumped
        return signal
    }
}
