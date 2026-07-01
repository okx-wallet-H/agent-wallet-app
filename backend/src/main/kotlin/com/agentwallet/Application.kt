package com.agentwallet

import com.agentwallet.api.*
import com.agentwallet.auth.*
import com.agentwallet.config.*
import com.agentwallet.plugins.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

fun main() {
    DotEnv.load()  // Load .env from working directory
    val config = AppConfig.fromEnv()
    DatabaseFactory.init(config)

    embeddedServer(Netty, port = config.port, host = config.host) {
        configureSerialization()
        configureStatusPages()
        configureAuth(config)
        configureWebSockets()

        routing {
            authRoutes(config)
            chatRoutes(config)
            strategyRoutes()
            portfolioRoutes()
            signalRoutes()
        }
    }.start(wait = true)
}
