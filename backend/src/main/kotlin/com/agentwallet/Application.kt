package com.agentwallet

import com.agentwallet.api.*
import com.agentwallet.config.AppConfig
import com.agentwallet.plugins.*

fun main() {
    val config = AppConfig.fromEnv()

    // Initialize database (PostgreSQL + Redis + auto-create tables)
    DatabaseFactory.init(config)

    io.ktor.server.engine.embeddedServer(
        io.ktor.server.netty.Netty,
        port = config.port,
        host = config.host
    ) {
        configureSerialization()
        configureStatusPages()
        configureAuth(config)
        configureWebSockets()

        authRoutes(config)
        chatRoutes(config)
        strategyRoutes()
        portfolioRoutes()
        signalRoutes()
    }.start(wait = true)
}
