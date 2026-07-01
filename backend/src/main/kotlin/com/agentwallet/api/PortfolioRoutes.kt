package com.agentwallet.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.portfolioRoutes() {
    get("/api/portfolio") {
        call.respond(mapOf(
            "totalUsd" to 0.0, "dailyPnl" to 0.0, "dailyPnlPercent" to 0.0,
            "tokens" to emptyList<Map<String, Any>>()
        ))
    }
}
