package com.agentwallet.models

import kotlinx.serialization.Serializable
import java.util.UUID

// ─── User ─────────────────────────────────────────────

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val passwordHash: String,
    val coinbaseWalletId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class LoginRequest(val email: String, val password: String)
@Serializable
data class RegisterRequest(val email: String, val password: String)
@Serializable
data class AuthResponse(val token: String, val user: User)

// ─── Strategy ─────────────────────────────────────────

@Serializable
data class Strategy(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val type: StrategyType,
    val chain: String,
    val params: StrategyParams,
    val status: StrategyStatus = StrategyStatus.RUNNING,
    val trades: Int = 0,
    val pnl: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
enum class StrategyType { SNIPER, DCA, GRID, LIMIT_ORDER }

@Serializable
enum class StrategyStatus { RUNNING, PAUSED, STOPPED }

@Serializable
data class StrategyParams(
    val maxPerTx: Double,
    val dailyLimit: Double,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val allowedTokens: List<String> = emptyList(),
    val slippage: Double = 0.03
)

// ─── Trade Intent (NL → structured) ──────────────────

@Serializable
data class TradeIntent(
    val action: TradeAction,
    val fromToken: String,
    val toToken: String,
    val amount: Double,
    val chain: String,
    val slippage: Double = 0.01
)

@Serializable
enum class TradeAction { BUY, SELL }

// ─── Chat ────────────────────────────────────────────

@Serializable
data class ChatRequest(val message: String, val conversationId: String? = null)

@Serializable
data class ChatResponse(
    val conversationId: String,
    val message: String,
    val messages: List<ChatMessage> = emptyList()
)

@Serializable
data class ChatMessage(
    val role: String,       // "user" | "agent"
    val content: String,
    val type: String = "text", // text | trade_card | signal_card | strategy_card | thinking
    val data: kotlinx.serialization.json.JsonObject? = null
)

// ─── Risk ────────────────────────────────────────────

@Serializable
data class RiskDecision(
    val approved: Boolean,
    val reason: String? = null,
    val checks: List<RiskCheck> = emptyList()
)

@Serializable
data class RiskCheck(val label: String, val passed: Boolean)

// ─── JWT ─────────────────────────────────────────────

@Serializable
data class JwtPayload(
    val userId: String,
    val email: String
)
