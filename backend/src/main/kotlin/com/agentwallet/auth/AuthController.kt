package com.agentwallet.auth

import com.agentwallet.config.AppConfig
import com.agentwallet.models.*
import com.agentwallet.services.OnchainosService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun Route.authRoutes(config: AppConfig) {
    val jwtService = JwtService(config)
    val onchainos = OnchainosService()

    // Quick login for users with active OKX session (no OTP needed)
    post("/api/auth/quick-login") {
        val body = call.receive<EmailRequest>()
        val user = UserRepository.findByEmail(body.email) ?: run {
            call.respondText(json.encodeToString(mapOf("error" to "User not found")), ContentType.Application.Json, HttpStatusCode.NotFound)
            return@post
        }
        if (!user.onchainosVerified) {
            call.respondText(json.encodeToString(mapOf("error" to "Not verified, use OTP flow")), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }
        val status = onchainos.status(user.id)
        val loggedIn = status.isOk() && status.jsonData()?.jsonObject?.get("loggedIn")?.jsonPrimitive?.content == "true"
        if (!loggedIn) {
            call.respondText(json.encodeToString(mapOf("error" to "Session expired, use OTP")), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }
        val addrs = onchainos.getAddresses(user.id)
        val addrData = addrs.jsonData()?.jsonObject
        val evmAddr = addrData?.get("evm")?.jsonArray?.firstOrNull()?.jsonObject?.get("address")?.jsonPrimitive?.content ?: ""
        val solAddr = addrData?.get("solana")?.jsonArray?.firstOrNull()?.jsonObject?.get("address")?.jsonPrimitive?.content ?: ""
        val balance = onchainos.getBalances(user.id)
        val totalUsd = balance.jsonData()?.jsonObject?.get("totalValueUsd")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
        val token = jwtService.generateToken(JwtPayload(user.id, user.email))
        call.respondText(json.encodeToString(LoginResponse(token = token, user = UserInfo(user.id, user.email, evmAddr, solAddr, "", false, totalUsd))), ContentType.Application.Json, HttpStatusCode.OK)
    }

    // Send OTP (new user or returning user)
    post("/api/auth/register") {
        val body = call.receive<EmailRequest>()
        val email = body.email
        if (!email.contains("@")) {
            call.respondText(json.encodeToString(mapOf("error" to "Invalid email")), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }
        var user = UserRepository.findByEmail(email)
        if (user == null) user = UserRepository.create(email)

        val result = onchainos.sendOtp(user.id, email)
        if (!result.isOk()) {
            call.respondText(json.encodeToString(mapOf("error" to "Failed to send code", "detail" to result.output.take(200))), ContentType.Application.Json, HttpStatusCode.BadGateway)
            return@post
        }
        call.respondText(json.encodeToString(RegisterResponse("otp_sent", user.id, "验证码已发送到 $email", !user.onchainosVerified)), ContentType.Application.Json, HttpStatusCode.OK)
    }

    // Verify OTP → complete login, return wallet info
    post("/api/auth/verify-otp") {
        val req = call.receive<OtpRequest>()
        val user = UserRepository.findByEmail(req.email) ?: run {
            call.respondText(json.encodeToString(mapOf("error" to "User not found")), ContentType.Application.Json, HttpStatusCode.NotFound)
            return@post
        }

        // Check if user already has an active OKX session
        val status = onchainos.status(user.id)
        val alreadyLoggedIn = status.isOk() && status.jsonData()?.jsonObject?.get("loggedIn")?.jsonPrimitive?.content == "true"

        if (!alreadyLoggedIn) {
            // Need to complete OTP verification
            val result = onchainos.verifyOtp(user.id, req.otp)
            if (!result.isOk()) {
                call.respondText(json.encodeToString(mapOf("error" to "验证码错误或已过期")), ContentType.Application.Json, HttpStatusCode.BadRequest)
                return@post
            }
        }

        // Mark as verified
        if (!user.onchainosVerified) UserRepository.markVerified(user.id)

        // Get wallet addresses and balance
        val addrs = onchainos.getAddresses(user.id)
        val addrData = addrs.jsonData()?.jsonObject
        val evmAddr = addrData?.get("evm")?.jsonArray?.firstOrNull()?.jsonObject?.get("address")?.jsonPrimitive?.content ?: ""
        val solAddr = addrData?.get("solana")?.jsonArray?.firstOrNull()?.jsonObject?.get("address")?.jsonPrimitive?.content ?: ""
        val accountId = addrData?.get("accountId")?.jsonPrimitive?.content ?: ""

        // Get balance
 val balance = onchainos.getBalances(user.id)
        val balData = balance.jsonData()?.jsonObject
        val totalUsd = balData?.get("totalValueUsd")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0

        val token = jwtService.generateToken(JwtPayload(user.id, user.email))
        call.respondText(json.encodeToString(LoginResponse(
            token = token,
            user = UserInfo(user.id, user.email, evmAddr, solAddr, accountId, !user.onchainosVerified, totalUsd)
        )), ContentType.Application.Json, HttpStatusCode.OK)
    }
}

@Serializable data class RegisterResponse(val status: String, val userId: String, val message: String, val isNew: Boolean)
@Serializable data class EmailRequest(val email: String)
@Serializable data class OtpRequest(val email: String, val otp: String)
@Serializable data class LoginResponse(val token: String, val user: UserInfo)
@Serializable data class UserInfo(val id: String, val email: String, val evmAddress: String, val solanaAddress: String, val accountId: String, val isNew: Boolean = false, val totalUsd: Double = 0.0)
