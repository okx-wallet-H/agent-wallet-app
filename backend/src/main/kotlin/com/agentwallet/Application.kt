package com.agentwallet

import com.agentwallet.agent.SignalAgent
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

    // Start 7×24 SignalAgent in background
    val signalService = OkxSignalService(config.okxApiKey, config.okxSecretKey, config.okxPassphrase, config.okxBaseUrl)
    val signalAgent = SignalAgent(signalService)
    GlobalScope.launch { signalAgent.start(this, "501", "1") }

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
