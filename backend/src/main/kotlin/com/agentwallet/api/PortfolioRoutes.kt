package com.agentwallet.api

import io.ktor.server.application.*
import io.ktor.server.response.*

fun Application.portfolioRoutes() {
    routing {
        get("/api/portfolio") {
            // TODO: Call CoinbaseWalletService.getBalance()
            call.respond(mapOf(
                "totalUsd" to 12450.23,
                "dailyPnl" to 234.50,
                "dailyPnlPercent" to 1.92,
                "tokens" to listOf(
                    mapOf("symbol" to "ETH", "amount" to 2.3, "usdValue" to 7866.00, "change24h" to 1.8),
                    mapOf("symbol" to "SOL", "amount" to 150.0, "usdValue" to 2550.00, "change24h" to 3.2),
                    mapOf("symbol" to "USDC", "amount" to 5000.0, "usdValue" to 5000.00, "change24h" to 0.0)
                )
            ))
        }
    }
}
