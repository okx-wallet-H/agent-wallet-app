package com.agentwallet.agent

/**
 * Knowledge agent — records trade outcomes, updates signal source reputation,
 * and provides RAG retrieval for future decisions.
 *
 * MVP: stub implementations. Production: pgvector + embedding model.
 */
class KnowledgeAgent {

    /**
     * Record a completed trade for future learning.
     */
    fun recordTrade(userId: String, trade: TradeRecord) {
        // TODO: Store in PostgreSQL + generate embedding for RAG
        // INSERT INTO trade_history (user_id, token, action, amount, pnl, signal_source, ...)
    }

    /**
     * Update signal source reputation after a trade outcome.
     */
    fun updateSourceReputation(sourceAddress: String, outcome: SourceOutcome) {
        // TODO: Update signal source score
        // Win: weight += 0.05, Rug: weight -= 0.20
    }

    /**
     * Query the knowledge base for relevant past experiences.
     */
    suspend fun query(userId: String, context: String): List<RelevantExperience> {
        // TODO: Vector similarity search in pgvector
        // SELECT * FROM trade_embeddings ORDER BY embedding <-> query_embedding LIMIT 5
        return emptyList()
    }
}

data class TradeRecord(
    val token: String,
    val action: String,
    val amount: Double,
    val price: Double,
    val pnl: Double?,
    val signalSource: String?,
    val outcome: String  // "profit" | "loss" | "rug"
)

data class SourceOutcome(
    val isProfit: Boolean,
    val isRug: Boolean
)

data class RelevantExperience(
    val summary: String,
    val relevanceScore: Double
)
