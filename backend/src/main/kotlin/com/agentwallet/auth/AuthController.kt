package com.agentwallet.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.agentwallet.config.AppConfig
import com.agentwallet.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(config: AppConfig) {
    val jwtService = JwtService(config)

    post("/api/auth/register") {
        val req = call.receive<RegisterRequest>()
        val existing = UserRepository.findByEmail(req.email)
        if (existing != null) {
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
            return@post
        }
        val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
        val user = UserRepository.create(req.email, hash)
        val token = jwtService.generateToken(JwtPayload(user.id, user.email))
        call.respond(HttpStatusCode.Created, AuthResponse(token, user.copy(passwordHash = "")))
    }

    post("/api/auth/login") {
        val req = call.receive<LoginRequest>()
        val user = UserRepository.findByEmail(req.email)
        if (user == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            return@post
        }
        val verified = BCrypt.verifyer().verify(req.password.toCharArray(), user.passwordHash)
        if (!verified.verified) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            return@post
        }
        val token = jwtService.generateToken(JwtPayload(user.id, user.email))
        call.respond(AuthResponse(token, user.copy(passwordHash = "")))
    }
}
