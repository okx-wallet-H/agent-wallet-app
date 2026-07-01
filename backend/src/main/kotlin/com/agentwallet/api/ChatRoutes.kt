package com.agentwallet.api

import com.agentwallet.agent.*
import com.agentwallet.config.AppConfig
import com.agentwallet.models.*
import com.agentwallet.plugins.UserIdPrincipal
import com.agentwallet.services.*
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

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

    val orchestrator = OrchestratorAgent(llmClient, chatAgent, analysisAgent, executionAgent)

    post("/api/chat") {
        val req = call.receive<ChatRequest>()
        val userId = call.principal<UserIdPrincipal>()?.userId ?: "anonymous"
        val conversationId = req.conversationId ?: UUID.randomUUID().toString()
        val messages = orchestrator.process(userId, req.message, conversationId)
        call.respond(ChatResponse(conversationId, messages.firstOrNull()?.content ?: "", messages))
    }
}
