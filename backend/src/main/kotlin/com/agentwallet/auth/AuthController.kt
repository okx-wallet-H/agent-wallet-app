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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun Route.authRoutes(config: AppConfig) {
    val jwtService = JwtService(config)
    val onchainos = OnchainosService()

    // Send OTP to email (register or login — same flow)
    post("/api/auth/register") {
        val body = call.receive<EmailRequest>()
        val email = body.email

        if (!email.contains("@")) {
            call.respondText(json.encodeToString(mapOf("error" to "Invalid email")),
                ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }

        // Find or create user
        var user = UserRepository.findByEmail(email)
        if (user == null) {
            user = UserRepository.create(email)
        }

        // Send OKX OTP
        val result = onchainos.sendOtp(user.id, email)
        if (!result.isOk()) {
            call.respondText(json.encodeToString(mapOf(
                "error" to "Failed to send code", "detail" to result.output.take(200)
            )), ContentType.Application.Json, HttpStatusCode.BadGateway)
            return@post
        }

        call.respondText(json.encodeToString(mapOf(
            "status" to "otp_sent", "userId" to user.id,
            "message" to "验证码已发送到 $email"
        )), ContentType.Application.Json, HttpStatusCode.OK)
    }

    // Verify OTP → complete login / wallet creation
    post("/api/auth/verify-otp") {
        val req = call.receive<OtpRequest>()
        val user = UserRepository.findByEmail(req.email) ?: run {
            call.respondText(json.encodeToString(mapOf("error" to "User not found")),
                ContentType.Application.Json, HttpStatusCode.NotFound)
            return@post
        }

        val result = onchainos.verifyOtp(user.id, req.otp)
        if (!result.isOk()) {
            call.respondText(json.encodeToString(mapOf(
                "error" to "验证码错误或已过期"
            )), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }

        val newUser = !user.onchainosVerified
        if (newUser) UserRepository.markVerified(user.id)

        val addrs = onchainos.getAddresses(user.id)
        val data = addrs.jsonData()?.jsonObject
        val token = jwtService.generateToken(JwtPayload(user.id, user.email))

        call.respondText(json.encodeToString(LoginResponse(
            token = token,
            user = UserInfo(user.id, user.email,
                evmAddress = data?.get("address")?.jsonPrimitive?.content ?: "",
                accountId = data?.get("accountId")?.jsonPrimitive?.content ?: "",
                isNew = newUser)
        )), ContentType.Application.Json, HttpStatusCode.OK)
    }
}

@Serializable data class EmailRequest(val email: String)
@Serializable data class OtpRequest(val email: String, val otp: String)
@Serializable data class LoginResponse(val token: String, val user: UserInfo)
@Serializable data class UserInfo(val id: String, val email: String, val evmAddress: String, val accountId: String, val isNew: Boolean = false)
