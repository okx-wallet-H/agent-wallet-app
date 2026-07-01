package com.agentwallet.api

import com.agentwallet.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

fun Application.strategyRoutes() {
    routing {
        route("/api/strategies") {
            // List user's strategies
            get {
                val userId = extractUserId(call)
                val list = StrategyRepository.findByUserId(userId)
                call.respond(list)
            }

            // Create a new strategy
            post {
                val req = call.receive<Strategy>()
                val userId = extractUserId(call)
                val created = StrategyRepository.create(req.copy(userId = userId))
                call.respond(HttpStatusCode.Created, created)
            }

            // Update a strategy
            put("/{id}") {
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

                val existing = StrategyRepository.findById(id)
                    ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Strategy not found"))

                if (existing.userId != extractUserId(call)) {
                    return@put call.respond(HttpStatusCode.Forbidden)
                }

                val req = call.receive<Strategy>()
                val updated = StrategyRepository.update(id, existing.copy(
                    name = req.name,
                    status = req.status,
                    params = req.params
                ))
                call.respond(updated)
            }

            // Delete a strategy
            delete("/{id}") {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val existing = StrategyRepository.findById(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound)

                if (existing.userId != extractUserId(call)) {
                    return@delete call.respond(HttpStatusCode.Forbidden)
                }

                StrategyRepository.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun extractUserId(call: ApplicationCall): String {
    return call.principal<io.ktor.server.auth.JwtPrincipal>()
        ?.payload?.getClaim("userId")?.asString() ?: "anonymous"
}
