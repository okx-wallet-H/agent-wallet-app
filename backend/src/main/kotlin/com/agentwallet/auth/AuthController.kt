package com.agentwallet.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.agentwallet.config.AppConfig
import com.agentwallet.models.*
import com.agentwallet.services.HdWalletService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.SecureRandom

fun Route.authRoutes(config: AppConfig) {
    val jwtService = JwtService(config)

    // Initialize HD wallet service
    val mnemonic = System.getenv("MASTER_MNEMONIC") ?: run {
        val new = HdWalletService.generateMnemonic()
        println("⚠️  No MASTER_MNEMONIC set. Generated: $new")
        println("    Add this to your .env file as MASTER_MNEMONIC=<value>")
        new
    }
    val encKey = System.getenv("WALLET_ENCRYPTION_KEY")
        ?.toByteArray()
        ?: SecureRandom().run { ByteArray(32).also { nextBytes(it) } }

    val walletService = HdWalletService(encKey)

    post("/api/auth/register") {
        val req = call.receive<RegisterRequest>()

        val existing = UserRepository.findByEmail(req.email)
        if (existing != null) {
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
            return@post
        }

        // Create HD wallet for this user
        val walletIndex = UserRepository.nextWalletIndex()
        val wallet = walletService.createUserWallet(walletIndex, mnemonic)

        val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
        val user = UserRepository.create(
            email = req.email,
            passwordHash = hash,
            walletIndex = walletIndex,
            evmAddress = wallet.evmAddress,
            solanaAddress = wallet.solanaAddress,
            encryptedKey = wallet.encryptedPrivateKey
        )

        val token = jwtService.generateToken(JwtPayload(user.id, user.email))
        call.respond(HttpStatusCode.Created, mapOf(
            "token" to token,
            "user" to mapOf(
                "id" to user.id,
                "email" to user.email,
                "evmAddress" to wallet.evmAddress,
                "solanaAddress" to wallet.solanaAddress,
                "walletIndex" to walletIndex
            )
        ))
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
        call.respond(mapOf(
            "token" to token,
            "user" to mapOf(
                "id" to user.id,
                "email" to user.email,
                "evmAddress" to (user.evmAddress ?: ""),
                "solanaAddress" to (user.solanaAddress ?: ""),
                "walletIndex" to (user.walletIndex ?: 0)
            )
        ))
    }
}
