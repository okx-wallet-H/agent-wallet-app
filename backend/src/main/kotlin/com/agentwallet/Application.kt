package com.agentwallet

import com.agentwallet.agent.*
import com.agentwallet.api.*
import com.agentwallet.auth.*
import com.agentwallet.config.*
import com.agentwallet.plugins.*
import com.agentwallet.services.*
import io.ktor.client.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*

fun main() {
    val config = AppConfig.fromEnv()
    DotEnv.load()
    DatabaseFactory.init(config)

    // Wire services
    val httpClient = HttpClient()
    val okxAuth = OkxAuth(config.okxApiKey, config.okxSecretKey, config.okxPassphrase)
    val okxDex = OkxDexService(httpClient, okxAuth, config.okxBaseUrl)
    val okxSignal = OkxSignalService(config.okxApiKey, config.okxSecretKey, config.okxPassphrase, config.okxBaseUrl)
    val coinbaseWallet = CoinbaseWalletService(config)
    val llmClient = AnthropicLlmClient(httpClient, config.anthropicApiKey)

    // Start 7×24 agents
    val signalAgent = SignalAgent(okxSignal)
    val autonomousAgent = AutonomousAgent(
        ExecutionAgent(okxDex, coinbaseWallet),
        AnalysisAgent(okxSignal)
    )
    GlobalScope.launch { signalAgent.start(this, "501", "1") }
    GlobalScope.launch { autonomousAgent.start(this) }

    embeddedServer(Netty, port = config.port, host = config.host) {
        configureSerialization()
        configureStatusPages()
        configureAuth(config)
        configureWebSockets()
        routing {
            authRoutes(config)
            chatRoutes(config)
            strategyRoutes()
            portfolioRoutes(config)
            signalRoutes(config)
        }
    }.start(wait = true)
}
