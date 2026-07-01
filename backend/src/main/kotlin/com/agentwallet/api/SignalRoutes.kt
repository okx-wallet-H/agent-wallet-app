package com.agentwallet.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.signalRoutes() {
    get("/api/signals") {
        call.respond(emptyList<Map<String, String>>())
    }
}
