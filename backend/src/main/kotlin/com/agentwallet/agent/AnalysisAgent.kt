package com.agentwallet.agent

import com.agentwallet.services.OkxSignalService
import com.agentwallet.services.SignalData

/**
 * Analysis agent — aggregates OKX signal, token, trenches, and social data
 * into a buy/no-buy decision.
 */
class AnalysisAgent(private val signalService: OkxSignalService) {

    /**
     * Get recent signals, filtered through initial quality check.
     */
    suspend fun getRecentSignals(chainId: String): List<AnalyzedSignal> {
        val signals = signalService.getSignals(chainId)
        return signals
            .filter { it.soldRatio.toDoubleOrNull() ?: 100.0 < 30.0 } // filter out dumped tokens
            .map { signal -> analyzeSignal(signal, chainId) }
            .filter { it.confidence == "high" || it.confidence == "medium" }
    }

    /**
     * Deep-analyze a single signal.
     */
    private suspend fun analyzeSignal(signal: SignalData, chainId: String): AnalyzedSignal {
        // Get dev reputation
        val devAddr = signal.triggerWallets.firstOrNull() ?: return AnalyzedSignal(signal, "low", "无法获取开发者信息")

        val devProfile = signalService.getDevProfile(chainId, devAddr)
        val tokenAnalysis = signalService.getTokenAnalysis(chainId, signal.tokenAddress)

        // Scoring
        var score = 0
        val reasons = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (!devProfile.rugHistory) { score += 2; reasons.add("Dev 无 Rug 历史") }
        else { score -= 3; warnings.add("Dev 有 Rug 历史！") }

        if (tokenAnalysis.liquidityLocked) { score += 2; reasons.add("LP 已锁定") }
        else { score -= 2; warnings.add("LP 未锁定") }

        if (tokenAnalysis.bundleHoldingRatio < 0.30) { score += 2; reasons.add("捆绑比例安全") }
        else { score -= 2; warnings.add("捆绑比例过高") }

        if (tokenAnalysis.top10HolderRatio < 0.50) { score += 1; reasons.add("持有者分散") }
        else { score -= 1; warnings.add("Top 10 持有过于集中") }

        val confidence = when {
            score >= 5 -> "high"
            score >= 3 -> "medium"
            else -> "low"
        }

        return AnalyzedSignal(signal, confidence, reasons.joinToString("; "), warnings.joinToString("; "))
    }
}

data class AnalyzedSignal(
    val signal: SignalData,
    val confidence: String,
    val reasons: String,
    val warnings: String = ""
)
