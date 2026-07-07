package com.agentwallet.api

import com.agentwallet.agent.AiTraderEngine
import com.agentwallet.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true }

@Serializable data class CopyTradeRequest(val traderId: String, val maxPerTx: Double, val dailyLimit: Double)
@Serializable data class CopyTradeResponse(val ok: Boolean, val strategyId: String, val message: String)

fun Route.copyTradeRoutes(engine: AiTraderEngine) {
    post("/api/copy-trade") {
        val auth = call.request.headers["Authorization"]
        val userId = auth?.removePrefix("Bearer ")?.let { try { com.auth0.jwt.JWT.decode(it).getClaim("userId").asString() } catch (e: Exception) { null } } ?: "anonymous"
        if (userId == "anonymous") { call.respondText("""{"error":"login required"}""", ContentType.Application.Json, HttpStatusCode.Unauthorized); return@post }

        val req = call.receive<CopyTradeRequest>()
        val trader = engine.traders.find { it.id == req.traderId } ?: run {
            call.respondText("""{"error":"trader not found"}""", ContentType.Application.Json, HttpStatusCode.NotFound); return@post
        }

        // Create a follow strategy for this user
        val strategy = StrategyRepository.create(Strategy(
            userId = userId, name = "跟单: ${trader.name}", type = StrategyType.SNIPER,
            chain = trader.chain, params = StrategyParams(maxPerTx = req.maxPerTx, dailyLimit = req.dailyLimit),
            status = StrategyStatus.RUNNING
        ))

        // Increment follower count
        engine.incrementFollowers(req.traderId)

        call.respondText(json.encodeToString(CopyTradeResponse(true, strategy.id, "已开始跟单「${trader.name}」！单笔限额 $${req.maxPerTx}，日限额 $${req.dailyLimit}")), ContentType.Application.Json)
    }
}
