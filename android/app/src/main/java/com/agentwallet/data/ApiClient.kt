package com.agentwallet.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * HTTP client for the Agent Wallet backend.
 */
class ApiClient(private val baseUrl: String = "http://10.0.2.2:8080") {

    private var authToken: String? = null

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            authToken?.let { header("Authorization", "Bearer $it") }
        }
    }

    fun setToken(token: String?) { authToken = token }

    // ─── Auth ──────────────────────────────────────

    suspend fun login(email: String, password: String): AuthResponseDto {
        return httpClient.post("$baseUrl/api/auth/login") {
            setBody(LoginRequest(email, password))
        }.body()
    }

    suspend fun register(email: String, password: String): AuthResponseDto {
        return httpClient.post("$baseUrl/api/auth/register") {
            setBody(RegisterRequest(email, password))
        }.body()
    }

    // ─── Chat ──────────────────────────────────────

    suspend fun sendMessage(message: String, conversationId: String? = null): ChatResponseDto {
        return httpClient.post("$baseUrl/api/chat") {
            setBody(ChatRequestDto(message, conversationId))
        }.body()
    }

    // ─── Portfolio ─────────────────────────────────

    suspend fun getPortfolio(): PortfolioDto {
        return httpClient.get("$baseUrl/api/portfolio").body()
    }

    // ─── Strategies ────────────────────────────────

    suspend fun getStrategies(): List<StrategyDto> {
        return httpClient.get("$baseUrl/api/strategies").body()
    }

    suspend fun createStrategy(strategy: StrategyDto): StrategyDto {
        return httpClient.post("$baseUrl/api/strategies") {
            setBody(strategy)
        }.body()
    }

    // ─── Signals ───────────────────────────────────

    suspend fun getSignals(): List<SignalDto> {
        return httpClient.get("$baseUrl/api/signals").body()
    }
}

// ─── DTOs ────────────────────────────────────────────────

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val email: String, val password: String)

@Serializable
data class AuthResponseDto(val token: String, val user: UserDto)

@Serializable
data class UserDto(val id: String, val email: String, val coinbaseWalletId: String? = null)

@Serializable
data class ChatRequestDto(val message: String, val conversationId: String? = null)

@Serializable
data class ChatResponseDto(
    val conversationId: String,
    val message: String,
    val messages: List<ChatMessageDto>
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
    val type: String = "text",
    val data: kotlinx.serialization.json.JsonObject? = null
)

@Serializable
data class PortfolioDto(
    val totalUsd: Double,
    val dailyPnl: Double,
    val dailyPnlPercent: Double,
    val tokens: List<TokenBalanceDto>
)

@Serializable
data class TokenBalanceDto(
    val symbol: String,
    val amount: Double,
    val usdValue: Double,
    val change24h: Double
)

@Serializable
data class StrategyDto(
    val id: String = "",
    val name: String,
    val type: String = "SNIPER",
    val chain: String,
    val params: StrategyParamsDto,
    val status: String = "RUNNING",
    val trades: Int = 0,
    val pnl: Double = 0.0
)

@Serializable
data class StrategyParamsDto(
    val maxPerTx: Double,
    val dailyLimit: Double,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val slippage: Double = 0.03
)

@Serializable
data class SignalDto(
    val token: String,
    val chain: String,
    val amountUsd: String,
    val walletCount: Int,
    val sourceType: String,
    val timeAgo: String,
    val confidence: String
)
