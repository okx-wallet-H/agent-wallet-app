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

fun main(args: Array<String>) {
    val traderId = System.getenv("TRADER_ID")
    if (traderId != null) {
        // Run as standalone AI trader container
        val llmClient = AnthropicLlmClient(HttpClient(), System.getenv("ANTHROPIC_API_KEY") ?: "")
        val signalSvc = OkxSignalService(System.getenv("OKX_API_KEY") ?: "", System.getenv("OKX_SECRET_KEY") ?: "", System.getenv("OKX_PASSPHRASE") ?: "", "https://web3.okx.com")
        val engine = AiTraderEngine(llmClient, signalSvc, signalSvc)
        val profile = engine.traders.find { it.id == traderId } ?: run { System.err.println("Unknown trader: $traderId"); return }
        val auth = OkxAuth(System.getenv("OKX_API_KEY") ?: "", System.getenv("OKX_SECRET_KEY") ?: "", System.getenv("OKX_PASSPHRASE") ?: "")
        val socialSvc = OkxSocialService(HttpClient(), auth, "https://web3.okx.com")
        val container = AiTraderContainer(profile, llmClient, signalSvc, socialSvc)
        println("=== AI Trader: ${profile.emoji} ${profile.name} ===")
        runBlocking { container.run(this) }
        return
    }

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

    // Start AI Trader Engine
    val traderEngine = AiTraderEngine(llmClient, okxSignal, okxSignal)
    GlobalScope.launch { traderEngine.startAll(this) }

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
            traderRoutes(traderEngine)
            copyTradeRoutes(traderEngine)
        }
    }.start(wait = true)
}
