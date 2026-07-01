package com.agentwallet.api

import com.agentwallet.agent.*
import com.agentwallet.config.AppConfig
import com.agentwallet.models.*
import com.agentwallet.services.*
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json
import java.util.UUID

fun Application.chatRoutes(config: AppConfig) {
    // ─── Wire dependencies ──────────────────────────────
    val httpClient = HttpClient()
    val okxAuth = OkxAuth(config.okxApiKey, config.okxSecretKey, config.okxPassphrase)

    // Services
    val okxDex = OkxDexService(httpClient, okxAuth, config.okxBaseUrl)
    val okxSignal = OkxSignalService(httpClient, okxAuth, config.okxBaseUrl)
    val coinbaseWallet = CoinbaseWalletService(config)

    // LLM
    val llmClient = AnthropicLlmClient(httpClient, config.anthropicApiKey)

    // Agents
    val chatAgent = ChatAgent(llmClient, config.llmModel)
    val analysisAgent = AnalysisAgent(okxSignal)
    val executionAgent = ExecutionAgent(okxDex, coinbaseWallet)

    val orchestrator = OrchestratorAgent(
        llmClient = llmClient,
        chatAgent = chatAgent,
        analysisAgent = analysisAgent,
        executionAgent = executionAgent
    )

    val json = Json { ignoreUnknownKeys = true }

    routing {
        post("/api/chat") {
            val req = call.receive<ChatRequest>()
            val userId = extractUserId(call) ?: "anonymous"
            val conversationId = req.conversationId ?: UUID.randomUUID().toString()

            val messages = orchestrator.process(userId, req.message, conversationId)

            call.respond(
                ChatResponse(
                    conversationId = conversationId,
                    message = messages.firstOrNull()?.content ?: "",
                    messages = messages
                )
            )
        }
    }
}

private fun extractUserId(call: ApplicationCall): String? {
    return call.principal<io.ktor.server.auth.JwtPrincipal>()
        ?.payload?.getClaim("userId")?.asString()
}
