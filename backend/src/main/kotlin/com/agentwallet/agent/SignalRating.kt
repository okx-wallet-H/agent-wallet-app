package com.agentwallet.agent

import kotlinx.serialization.Serializable

/**
 * Star-rated trading signal with screening dimensions and AI opinion.
 * 1-5 stars based on signal quality, updated by user profit/loss outcomes.
 */
@Serializable
data class RatedSignal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val traderId: String,
    val traderName: String,
    val traderEmoji: String,
    val token: String,
    val chain: String,

    // Rating
    val stars: Int,           // 1-5, calculated from dimension scores
    val confidence: String,   // "high" | "medium" | "low"

    // Screening dimensions — each dimension scored 0-100
    val dimensions: List<SignalDimension>,
    val aiOpinion: String,    // Claude's analysis summary
    val suggestedAction: String, // "buy" | "watch" | "skip"

    // Outcome tracking
    val timestamp: Long = System.currentTimeMillis(),
    var outcome: String? = null,  // "win" | "loss" | "rug" | null (pending)
    var actualPnl: Double? = null,
    var followerCount: Int = 0
)

@Serializable
data class SignalDimension(
    val capabilityId: String,   // "trenches:dev"
    val label: String,          // "开发者信誉"
    val score: Int,             // 0-100
    val detail: String          // "该Dev创建过 5 个项目，0 Rug，平均存活 7 天"
)

/**
 * Compute star rating from dimension scores.
 * 5 stars: avg >= 80
 * 4 stars: avg >= 65
 * 3 stars: avg >= 50
 * 2 stars: avg >= 35
 * 1 star:  avg < 35
 */
fun computeStars(dimensions: List<SignalDimension>): Int {
    if (dimensions.isEmpty()) return 1
    val avg = dimensions.map { it.score }.average()
    return when {
        avg >= 80 -> 5; avg >= 65 -> 4; avg >= 50 -> 3; avg >= 35 -> 2; else -> 1
    }
}

/**
 * Trader reputation — calculated from signal outcomes.
 */
@Serializable
data class TraderReputation(
    val totalSignals: Int,
    val winSignals: Int,
    val lossSignals: Int,
    val rugSignals: Int,
    val totalPnl: Double,
    val avgStars: Double,
    val followerCount: Int,
    val score: Double  // overall reputation score, 0-100
) {
    companion object {
        fun calculate(signals: List<RatedSignal>, followers: Int): TraderReputation {
            val closed = signals.filter { it.outcome != null }
            val wins = closed.count { it.outcome == "win" }
            val losses = closed.count { it.outcome == "loss" }
            val rugs = closed.count { it.outcome == "rug" }
            val totalPnl = signals.sumOf { it.actualPnl ?: 0.0 }
            val avgStars = if (signals.isNotEmpty()) signals.map { it.stars }.average() else 0.0
            val score = when {
                closed.isEmpty() -> 50.0
                else -> minOf(100.0, (wins.toDouble() / closed.size * 70) + (avgStars / 5.0 * 30))
            }
            return TraderReputation(signals.size, wins, losses, rugs, totalPnl, avgStars, followers, score)
        }
    }
}
