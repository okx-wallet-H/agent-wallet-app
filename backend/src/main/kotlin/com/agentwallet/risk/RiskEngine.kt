package com.agentwallet.risk

import com.agentwallet.models.RiskCheck
import com.agentwallet.models.RiskDecision
import com.agentwallet.models.TradeIntent
import com.agentwallet.plugins.RiskUsageTracker

/**
 * Pre-trade risk checks. Uses Redis for daily usage tracking (survives restarts).
 */
class RiskEngine(
    private val perTxLimit: Double = 500.0,
    private val dailyLimit: Double = 2000.0,
    private val blacklistedTokens: Set<String> = emptySet()
) {
    fun check(userId: String, intent: TradeIntent): RiskDecision {
        val checks = mutableListOf<RiskCheck>()

        // 1. Amount within per-transaction limit
        val amountOk = intent.amount <= perTxLimit
        checks.add(RiskCheck("单笔限额 ($$perTxLimit)", amountOk))

        // 2. Within daily limit (Redis-backed)
        val todayUsed = RiskUsageTracker.get(userId)
        val dailyOk = (todayUsed + intent.amount) <= dailyLimit
        checks.add(RiskCheck("日限额 ($$dailyLimit / 已用 $${"%.0f".format(todayUsed)})", dailyOk))

        // 3. Token whitelist
        val tokenOk = intent.toToken !in blacklistedTokens &&
                       intent.fromToken !in blacklistedTokens
        checks.add(RiskCheck("代币白名单", tokenOk))

        // 4. Slippage bounds
        val slippageOk = intent.slippage in 0.001..0.10
        checks.add(RiskCheck("滑点范围 (0.1%-10%)", slippageOk))

        val allPassed = checks.all { it.passed }
        val reason = if (!allPassed) {
            checks.filter { !it.passed }.joinToString("; ") { it.label }
        } else null

        // Record usage in Redis if approved
        if (allPassed) {
            RiskUsageTracker.increment(userId, intent.amount)
        }

        return RiskDecision(approved = allPassed, reason = reason, checks = checks)
    }

    fun resetDailyUsage(userId: String) {
        RiskUsageTracker.reset(userId)
    }
}
