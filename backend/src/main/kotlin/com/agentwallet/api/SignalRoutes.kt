package com.agentwallet.api

import com.agentwallet.config.AppConfig
import com.agentwallet.services.*
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.signalRoutes(config: AppConfig) {
    val httpClient = HttpClient()
    val okxAuth = OkxAuth(config.okxApiKey, config.okxSecretKey, config.okxPassphrase)
    val signalService = OkxSignalService(config.okxApiKey, config.okxSecretKey, config.okxPassphrase, config.okxBaseUrl)

    get("/api/signals") {
        val chainId = call.request.queryParameters["chainId"] ?: "501"
        val walletType = call.request.queryParameters["walletType"] ?: "1,2,3"
        val signals = signalService.getSignals(chainId, walletType)
        call.respond(signals.map { s ->
            mapOf(
                "token" to s.tokenSymbol,
                "chain" to s.chainId,
                "amountUsd" to s.amountUsd,
                "walletCount" to s.triggerWallets.size,
                "sourceType" to when (s.walletType) { "1" -> "聪明钱"; "2" -> "KOL"; "3" -> "巨鲸"; else -> "未知" },
                "timeAgo" to "刚刚",
                "confidence" to "medium"
            )
        })
    }
}
