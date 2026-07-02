package com.agentwallet.agent

import com.agentwallet.models.TradeAction
import com.agentwallet.models.TradeIntent
import com.agentwallet.plugins.DatabaseFactory
import com.agentwallet.risk.RiskEngine
import com.agentwallet.services.SignalData
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Autonomous trading agent — subscribes to Redis signal:new channel,
 * performs risk checks, and auto-executes trades for users with active strategies.
 */
class AutonomousAgent(
    private val executionAgent: ExecutionAgent,
    private val analysisAgent: AnalysisAgent
) {
    private val logger = LoggerFactory.getLogger(AutonomousAgent::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val riskEngine = RiskEngine()

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val redis = DatabaseFactory.redis() ?: return@launch
            logger.info("AutonomousAgent started — polling Redis signal:new")

            // Poll Redis list instead of blocking subscribe
            while (isActive) {
                try {
                    redis.resource.use { jedis ->
                        // Block with timeout — wait up to 5s for a new signal
                        val response = jedis.blpop(5, "signal:new")
                        response?.let { (_, message) ->
                            val signal = json.decodeFromString<SignalData>(message)
                            handleSignal(signal)
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("AutonomousAgent error: ${e.message}")
                    delay(1000)
                }
            }
        }
    }

    private suspend fun handleSignal(signal: SignalData) {
        // Only act on high-value signals
        val amount = signal.amountUsd.replace("$", "").replace(",", "").toDoubleOrNull() ?: return
        if (amount < 5000) return

        // Deep analyze
        val analyzed = analysisAgent.getRecentSignals(signal.chainId)
            .find { it.signal.tokenAddress == signal.tokenAddress } ?: return

        if (analyzed.confidence != "high") return

        // Execute for a demo user (in production: iterate users with active strategies)
        val intent = TradeIntent(
            action = TradeAction.BUY,
            fromToken = if (signal.chainId == "501") "SOL" else "ETH",
            toToken = signal.tokenSymbol,
            amount = 100.0,
            chain = signal.chainId,
            slippage = 0.05
        )

        val risk = riskEngine.check("autonomous", intent)
        if (!risk.approved) {
            logger.info("Autonomous trade rejected by risk engine: ${risk.reason}")
            return
        }

        val result = executionAgent.executeSwap("autonomous", "wallet_autonomous", intent)
        if (result.success) {
            logger.info("✅ Autonomous trade executed: ${result.amount} ${result.fromToken} → ${result.toToken} txHash=${result.txHash}")
        } else {
            logger.warn("❌ Autonomous trade failed: ${result.error}")
        }
    }
}
