package com.agentwallet.api

import com.agentwallet.config.AppConfig
import com.agentwallet.models.UserRepository
import com.agentwallet.services.OnchainosService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true }

@Serializable data class PortfolioData(val totalUsd: Double, val tokens: List<TokenData>, val evmAddress: String = "", val solanaAddress: String = "")
@Serializable data class TokenData(val symbol: String, val amount: Double, val usdValue: Double)

fun Route.portfolioRoutes(config: AppConfig) {
    val onchainos = OnchainosService()

    get("/api/portfolio") {
        val auth = call.request.headers["Authorization"]
        val userId = auth?.removePrefix("Bearer ")?.let {
            try { com.auth0.jwt.JWT.decode(it).getClaim("userId").asString() } catch (e: Exception) { null }
        } ?: "anonymous"

        val user = UserRepository.findById(userId)
        if (user == null || !user.onchainosVerified) {
            call.respondText(json.encodeToString(mapOf("error" to "wallet not connected", "totalUsd" to 0.0, "tokens" to emptyList<Any>())), io.ktor.http.ContentType.Application.Json)
            return@get
        }

        // Check session is still active
        val status = onchainos.status(userId)
        val loggedIn = status.isOk() && status.jsonData()?.jsonObject?.get("loggedIn")?.jsonPrimitive?.content == "true"
        if (!loggedIn) {
            call.respondText(json.encodeToString(mapOf("error" to "session expired, please re-verify", "totalUsd" to 0.0, "tokens" to emptyList<Any>())), io.ktor.http.ContentType.Application.Json)
            return@get
        }

        // Get addresses
        var evmAddr = ""; var solAddr = ""
        try {
            val addrs = onchainos.getAddresses(userId)
            val ad = addrs.jsonData()?.jsonObject
            evmAddr = ad?.get("evm")?.jsonArray?.firstOrNull()?.jsonObject?.get("address")?.jsonPrimitive?.content ?: ""
            solAddr = ad?.get("solana")?.jsonArray?.firstOrNull()?.jsonObject?.get("address")?.jsonPrimitive?.content ?: ""
        } catch (e: Exception) {}

        // Get balances
        var totalUsd = 0.0
        val tokens = mutableListOf<TokenData>()
        try {
            val balances = onchainos.getBalances(userId)
            val details = balances.jsonData()?.jsonObject?.get("details")?.jsonArray ?: JsonArray(emptyList())
            for (chain in details) {
                val chainObj = chain.jsonObject
                val tokenAssets = chainObj["tokenAssets"]?.jsonArray ?: continue
                for (t in tokenAssets) {
                    val tok = t.jsonObject
                    val sym = tok["customSymbol"]?.jsonPrimitive?.content ?: tok["symbol"]?.jsonPrimitive?.content ?: "?"
                    val bal = tok["balance"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val usd = tok["usdValue"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    if (bal > 0.0 || usd > 0.0) { tokens.add(TokenData(sym, bal, usd)); totalUsd += usd }
                }
            }
        } catch (e: Exception) {}

        call.respondText(json.encodeToString(PortfolioData(totalUsd, tokens, evmAddr, solAddr)), io.ktor.http.ContentType.Application.Json)
    }
}
