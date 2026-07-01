package com.agentwallet.api

import com.agentwallet.models.*
import com.agentwallet.plugins.UserIdPrincipal
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun ApplicationCall.userId(): String =
    principal<UserIdPrincipal>()?.userId ?: "anonymous"

fun Route.strategyRoutes() {
    route("/api/strategies") {
        get {
            call.respond(StrategyRepository.findByUserId(call.userId()))
        }
        post {
            val req = call.receive<Strategy>()
            val created = StrategyRepository.create(req.copy(userId = call.userId()))
            call.respond(HttpStatusCode.Created, created)
        }
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            val existing = StrategyRepository.findById(id) ?: return@put call.respond(HttpStatusCode.NotFound)
            if (existing.userId != call.userId()) return@put call.respond(HttpStatusCode.Forbidden)
            val req = call.receive<Strategy>()
            val updated = StrategyRepository.update(id, existing.copy(name = req.name, status = req.status, params = req.params))
            call.respond(updated)
        }
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val existing = StrategyRepository.findById(id) ?: return@delete call.respond(HttpStatusCode.NotFound)
            if (existing.userId != call.userId()) return@delete call.respond(HttpStatusCode.Forbidden)
            StrategyRepository.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
