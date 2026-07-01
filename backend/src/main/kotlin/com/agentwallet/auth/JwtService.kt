package com.agentwallet.auth

import com.agentwallet.config.AppConfig
import com.agentwallet.models.JwtPayload
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class JwtService(private val config: AppConfig) {

    val verifier = com.auth0.jwt.JWT
        .require(Algorithm.HMAC256(config.jwtSecret))
        .withAudience(config.jwtAudience)
        .withIssuer(config.jwtIssuer)
        .build()

    fun generateToken(payload: JwtPayload): String {
        return JWT.create()
            .withAudience(config.jwtAudience)
            .withIssuer(config.jwtIssuer)
            .withClaim("userId", payload.userId)
            .withClaim("email", payload.email)
            .withExpiresAt(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)) // 7 days
            .sign(Algorithm.HMAC256(config.jwtSecret))
    }
}
