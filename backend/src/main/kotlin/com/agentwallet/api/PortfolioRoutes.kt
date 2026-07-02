package com.agentwallet.api

import com.agentwallet.config.AppConfig
import com.agentwallet.plugins.UserIdPrincipal
import com.agentwallet.services.CoinbaseWalletService
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.portfolioRoutes(config: AppConfig) {
    val coinbaseWallet = CoinbaseWalletService(config)

    get("/api/portfolio") {
        val userId = call.principal<UserIdPrincipal>()?.userId ?: "anonymous"
        try {
            val balance = coinbaseWallet.getBalance("wallet_${userId.take(8)}")
            call.respond(mapOf(
                "totalUsd" to balance.totalUsd,
                "dailyPnl" to 0.0,
                "dailyPnlPercent" to 0.0,
                "tokens" to balance.tokens.map {
                    mapOf("symbol" to it.symbol, "amount" to it.amount, "usdValue" to it.usdValue, "change24h" to 0.0)
                }
            ))
        } catch (e: Exception) {
            call.respond(mapOf("totalUsd" to 0.0, "dailyPnl" to 0.0, "dailyPnlPercent" to 0.0, "tokens" to emptyList<Map<String, Any>>()))
        }
    }
}
