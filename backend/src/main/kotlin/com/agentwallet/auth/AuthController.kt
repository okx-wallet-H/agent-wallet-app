package com.agentwallet.auth

import at.favre.lib.crypto.bcrypt.BCrypt
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

    // Step 1: Register — sends OKX verification code
    post("/api/auth/register") {
        val req = call.receive<RegisterRequest>()
        if (UserRepository.findByEmail(req.email) != null) {
            call.respondText(json.encodeToString(mapOf("error" to "Email already registered")),
                ContentType.Application.Json, HttpStatusCode.Conflict)
            return@post
        }
        val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
        val user = UserRepository.create(req.email, hash)

        val result = onchainos.sendOtp(user.id, req.email)
        if (!result.isOk()) {
            call.respondText(json.encodeToString(mapOf(
                "error" to "Failed to send verification code",
                "detail" to result.output.take(200)
            )), ContentType.Application.Json, HttpStatusCode.BadGateway)
            return@post
        }
        call.respondText(json.encodeToString(mapOf(
            "status" to "otp_sent", "userId" to user.id,
            "message" to "验证码已发送到 ${req.email}，请查收邮件并输入验证码"
        )), ContentType.Application.Json, HttpStatusCode.Created)
    }

    // Step 2: Verify OTP
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
                "error" to "验证码错误或已过期", "detail" to result.output.take(200)
            )), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }
        UserRepository.markVerified(user.id)
        val addrs = onchainos.getAddresses(user.id)
        val data = addrs.jsonData()?.jsonObject
        val token = jwtService.generateToken(JwtPayload(user.id, user.email))
        call.respondText(json.encodeToString(LoginResponse(
            token = token,
            user = UserInfo(user.id, user.email,
                evmAddress = data?.get("address")?.jsonPrimitive?.content ?: "",
                accountId = data?.get("accountId")?.jsonPrimitive?.content ?: "")
        )), ContentType.Application.Json, HttpStatusCode.OK)
    }

    // Login
    post("/api/auth/login") {
        val req = call.receive<LoginRequest>()
        val user = UserRepository.findByEmail(req.email) ?: run {
            call.respondText(json.encodeToString(mapOf("error" to "Invalid credentials")),
                ContentType.Application.Json, HttpStatusCode.Unauthorized)
            return@post
        }
        val verified = BCrypt.verifyer().verify(req.password.toCharArray(), user.passwordHash)
        if (!verified.verified) {
            call.respondText(json.encodeToString(mapOf("error" to "Invalid credentials")),
                ContentType.Application.Json, HttpStatusCode.Unauthorized)
            return@post
        }
        val token = jwtService.generateToken(JwtPayload(user.id, user.email))
        call.respondText(json.encodeToString(LoginResponse(
            token = token,
            user = UserInfo(user.id, user.email, evmAddress = "", accountId = "")
        )), ContentType.Application.Json, HttpStatusCode.OK)
    }
}

@Serializable data class OtpRequest(val email: String, val otp: String)
@Serializable data class LoginResponse(val token: String, val user: UserInfo)
@Serializable data class UserInfo(val id: String, val email: String, val evmAddress: String, val accountId: String)
