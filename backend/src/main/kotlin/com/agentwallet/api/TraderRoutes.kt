package com.agentwallet.api

import com.agentwallet.agent.AiTraderEngine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Serializable data class TraderListResponse(val traders: List<TraderDto>)
@Serializable data class TraderDto(val id: String, val name: String, val emoji: String, val description: String, val chain: String, val stats: TraderStatsDto)
@Serializable data class TraderStatsDto(val totalSignals: Int, val winRate: Double, val totalPnl: Double, val followers: Int)

fun Route.traderRoutes(engine: AiTraderEngine) {
    get("/api/traders") {
        val traders = engine.traders.map { t ->
            val s = engine.getTraderStats(t.id)
            TraderDto(t.id, t.name, t.emoji, t.description, t.chain, TraderStatsDto(s.totalSignals, s.winRate, s.totalPnl, s.followers))
        }
        call.respondText(json.encodeToString(TraderListResponse(traders)), ContentType.Application.Json)
    }

    get("/api/traders/{id}") {
        val id = call.parameters["id"] ?: ""
        val trader = engine.traders.find { it.id == id } ?: run {
            call.respondText("""{"error":"not found"}""", ContentType.Application.Json, HttpStatusCode.NotFound); return@get
        }
        val stats = engine.getTraderStats(id)
        val signals = engine.getSignals(id).takeLast(20)
        call.respondText(json.encodeToString(mapOf(
            "profile" to mapOf("id" to trader.id, "name" to trader.name, "emoji" to trader.emoji, "description" to trader.description, "chain" to trader.chain, "dataSource" to trader.dataSource),
            "stats" to mapOf("totalSignals" to stats.totalSignals, "winRate" to stats.winRate, "totalPnl" to stats.totalPnl, "followers" to stats.followers),
            "signals" to signals.map { mapOf("token" to it.token, "confidence" to it.confidence, "reason" to it.reason, "action" to it.action, "timestamp" to it.timestamp, "outcome" to (it.outcome ?: "pending")) }
        )), ContentType.Application.Json)
    }

    get("/api/signals") {
        val traderId = call.request.queryParameters["trader"]
        val signals = engine.getSignals(traderId)
        call.respondText(json.encodeToString(signals.map {
            mapOf("traderId" to it.traderId, "traderName" to it.traderName, "traderEmoji" to it.traderEmoji, "token" to it.token, "confidence" to it.confidence, "reason" to it.reason, "action" to it.action, "timestamp" to it.timestamp)
        }), ContentType.Application.Json)
    }
}
