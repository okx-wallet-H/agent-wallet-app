package com.agentwallet.api

import com.agentwallet.config.AppConfig
import com.agentwallet.models.UserRepository
import com.agentwallet.plugins.UserIdPrincipal
import com.agentwallet.services.OnchainosService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true }

fun Route.portfolioRoutes(config: AppConfig) {
    val onchainos = OnchainosService()

    get("/api/portfolio") {
        val userId = call.principal<UserIdPrincipal>()?.userId ?: "anonymous"
        val user = UserRepository.findById(userId)

        if (user == null || !user.onchainosVerified) {
            call.respondText(json.encodeToString(mapOf("totalUsd" to 0.0, "tokens" to emptyList<Any>())),
                io.ktor.http.ContentType.Application.Json)
            return@get
        }

        val balances = onchainos.getBalances(userId)
        val data = balances.jsonData()?.jsonObject
        val details = data?.get("details")?.jsonArray ?: JsonArray(emptyList())

        val tokens = mutableListOf<Map<String, Any?>>()
        var totalUsd = 0.0

        for (chain in details) {
            val chainObj = chain.jsonObject
            val tokenAssets = chainObj["tokenAssets"]?.jsonArray ?: continue
            for (t in tokenAssets) {
                val tok = t.jsonObject
                val sym = tok["customSymbol"]?.jsonPrimitive?.content
                    ?: tok["symbol"]?.jsonPrimitive?.content ?: "?"
                val bal = tok["balance"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                val usd = tok["usdValue"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                if (bal > 0.0 || usd > 0.0) {
                    tokens.add(mapOf("symbol" to sym, "amount" to bal, "usdValue" to usd))
                    totalUsd += usd
                }
            }
        }

        call.respondText(json.encodeToString(mapOf("totalUsd" to totalUsd, "tokens" to tokens)),
            io.ktor.http.ContentType.Application.Json)
    }
}
