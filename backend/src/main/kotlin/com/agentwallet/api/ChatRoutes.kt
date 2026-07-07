package com.agentwallet.api

import com.agentwallet.agent.*
import com.agentwallet.config.AppConfig
import com.agentwallet.models.*
import com.agentwallet.plugins.UserIdPrincipal
import com.agentwallet.services.*
import io.ktor.client.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// In-memory store for pending trade confirmations
private val pendingTrades = ConcurrentHashMap<String, PendingTrade>()

data class PendingTrade(
    val userId: String,
    val tradeIntent: TradeIntent,
    val createdAt: Long = System.currentTimeMillis()
)

fun Route.chatRoutes(config: AppConfig) {
    val httpClient = HttpClient()
    val okxAuth = OkxAuth(config.okxApiKey, config.okxSecretKey, config.okxPassphrase)
    val okxDex = OkxDexService(httpClient, okxAuth, config.okxBaseUrl)
    val okxSignal = OkxSignalService(config.okxApiKey, config.okxSecretKey, config.okxPassphrase, config.okxBaseUrl)
    val coinbaseWallet = CoinbaseWalletService(config)

    val llmClient = AnthropicLlmClient(httpClient, config.anthropicApiKey)
    val chatAgent = ChatAgent(llmClient, config.llmModel)
    val analysisAgent = AnalysisAgent(okxSignal)
    val executionAgent = ExecutionAgent(okxDex, coinbaseWallet)

    val onchainos = com.agentwallet.services.OnchainosService()
    val orchestrator = OrchestratorAgent(llmClient, chatAgent, analysisAgent, executionAgent, onchainos)

    post("/api/chat") {
        val req = call.receive<ChatRequest>()
        // Extract userId from JWT in Authorization header
        val authHeader = call.request.headers["Authorization"]
        val userId = if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                val token = authHeader.removePrefix("Bearer ")
                val jwt = com.auth0.jwt.JWT.decode(token)
                jwt.getClaim("userId").asString() ?: "anonymous"
            } catch (e: Exception) { "anonymous" }
        } else "anonymous"
        val conversationId = req.conversationId ?: UUID.randomUUID().toString()
        val messages = orchestrator.process(userId, req.message, conversationId)

        // Store pending trade if we got a trade_confirmation
        val tradeMsg = messages.find { it.type == "trade_confirmation" && it.data != null }
        if (tradeMsg != null) {
            val data = tradeMsg.data!!
            val tradeIntent = TradeIntent(
                action = TradeAction.BUY,
                fromToken = data["fromToken"]?.jsonPrimitive?.content ?: "ETH",
                toToken = data["token"]?.jsonPrimitive?.content ?: "",
                amount = data["amount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                chain = data["chain"]?.jsonPrimitive?.content ?: "1",
                slippage = data["slippage"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.03
            )
            pendingTrades[conversationId] = PendingTrade(userId, tradeIntent)
        }

        call.respond(ChatResponse(conversationId, messages.firstOrNull()?.content ?: "", messages))
    }

    post("/api/chat/confirm") {
        val req = call.receive<ConfirmRequest>()
        val pending = pendingTrades[req.conversationId]
        if (pending == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "No pending trade"))
            return@post
        }

        if (req.action == "reject") {
            pendingTrades.remove(req.conversationId)
            call.respond(ChatResponse(req.conversationId, "交易已取消", listOf(
                ChatMessage("agent", "交易已取消", "text")
            )))
            return@post
        }

        // Execute the trade
        val result = executionAgent.executeSwap(pending.userId, "wallet_${pending.userId.take(8)}", pending.tradeIntent)
        pendingTrades.remove(req.conversationId)

        call.respond(ChatResponse(req.conversationId, "交易完成 ✅", listOf(
            ChatMessage("agent", "✅ 交易完成：${result.amount} ${result.fromToken} → ${result.price} ${result.toToken}", "text"),
            ChatMessage("agent", result.txHash, "text")
        )))
    }
}

@kotlinx.serialization.Serializable
data class ConfirmRequest(val conversationId: String, val action: String)  // "confirm" | "reject"
