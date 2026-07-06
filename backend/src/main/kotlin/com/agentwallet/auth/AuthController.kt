package com.agentwallet.auth
import kotlinx.serialization.json.*

import at.favre.lib.crypto.bcrypt.BCrypt
import com.agentwallet.config.AppConfig
import com.agentwallet.models.*
import com.agentwallet.services.OnchainosService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(config: AppConfig) {
    val jwtService = JwtService(config)
    val onchainos = OnchainosService()

    // Step 1: Register — sends OKX verification code to email
    post("/api/auth/register") {
        val req = call.receive<RegisterRequest>()

        if (UserRepository.findByEmail(req.email) != null) {
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
            return@post
        }

        // Create user record (not verified yet)
        val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
        val user = UserRepository.create(req.email, hash)

        // Trigger OKX email verification
        val result = onchainos.sendOtp(user.id, req.email)
        if (!result.isOk()) {
            call.respond(HttpStatusCode.BadGateway, mapOf(
                "error" to "Failed to send verification code",
                "detail" to result.output.take(200)
            ))
            return@post
        }

        call.respond(HttpStatusCode.Created, mapOf(
            "status" to "otp_sent",
            "userId" to user.id,
            "message" to "验证码已发送到 ${req.email}，请查收邮件并输入验证码"
        ))
    }

    // Step 2: Verify OTP — completes OKX wallet creation
    post("/api/auth/verify-otp") {
        val req = call.receive<OtpRequest>()

        val user = UserRepository.findByEmail(req.email)
            ?: run {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@post
            }

        val result = onchainos.verifyOtp(user.id, req.otp)
        if (!result.isOk()) {
            call.respond(HttpStatusCode.BadRequest, mapOf(
                "error" to "验证码错误或已过期",
                "detail" to result.output.take(200)
            ))
            return@post
        }

        // Mark user as verified
        UserRepository.markVerified(user.id)

        // Get wallet addresses
        val addrs = onchainos.getAddresses(user.id)
        val data = addrs.jsonData()?.jsonObject

        val token = jwtService.generateToken(JwtPayload(user.id, user.email))
        call.respond(mapOf(
            "token" to token,
            "user" to mapOf(
                "id" to user.id,
                "email" to user.email,
                "evmAddress" to (data?.get("address")?.jsonPrimitive?.content ?: "check OKX wallet"),
                "accountId" to (data?.get("accountId")?.jsonPrimitive?.content ?: "")
            )
        ))
    }

    // Login — user already has OKX wallet
    post("/api/auth/login") {
        val req = call.receive<LoginRequest>()
        val user = UserRepository.findByEmail(req.email)
            ?: run {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }

        val verified = BCrypt.verifyer().verify(req.password.toCharArray(), user.passwordHash)
        if (!verified.verified) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            return@post
        }

        // Check OKX session is still valid
        val status = onchainos.status(user.id)
        if (!status.isOk()) {
            // Re-login needed
            onchainos.sendOtp(user.id, user.email)
            call.respond(mapOf("status" to "otp_required", "message" to "需要重新验证，验证码已发送"))
            return@post
        }

        val addrs = onchainos.getAddresses(user.id)
        val data = addrs.jsonData()?.jsonObject

        val token = jwtService.generateToken(JwtPayload(user.id, user.email))
        call.respond(mapOf(
            "token" to token,
            "user" to mapOf(
                "id" to user.id,
                "email" to user.email,
                "evmAddress" to (data?.get("address")?.jsonPrimitive?.content ?: ""),
                "accountId" to (data?.get("accountId")?.jsonPrimitive?.content ?: "")
            )
        ))
    }
}

@kotlinx.serialization.Serializable
data class OtpRequest(val email: String, val otp: String)
