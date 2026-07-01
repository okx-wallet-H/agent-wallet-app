package com.agentwallet.api

import io.ktor.server.application.*
import io.ktor.server.response.*

fun Application.signalRoutes() {
    routing {
        get("/api/signals") {
            // TODO: Call OkxSignalService.getSignals()
            call.respond(listOf(
                mapOf(
                    "token" to "\$NEWPUMP",
                    "chain" to "Solana",
                    "amountUsd" to "\$12,500",
                    "walletCount" to 3,
                    "sourceType" to "聪明钱",
                    "timeAgo" to "刚刚",
                    "confidence" to "高"
                ),
                mapOf(
                    "token" to "\$MIDCOIN",
                    "chain" to "Ethereum",
                    "amountUsd" to "\$45,200",
                    "walletCount" to 1,
                    "sourceType" to "巨鲸",
                    "timeAgo" to "5 分钟前",
                    "confidence" to "中"
                )
            ))
        }
    }
}
