package com.agentwallet.plugins

import com.agentwallet.auth.JwtService
import com.agentwallet.config.AppConfig
import com.auth0.jwt.JWT
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth(config: AppConfig) {
    val jwtService = JwtService(config)

    install(Authentication) {
        jwt("auth-jwt") {
            realm = config.jwtRealm
            verifier(jwtService.verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val email = credential.payload.getClaim("email").asString()
                if (userId != null && email != null) UserIdPrincipal(userId, email) else null
            }
        }
    }
}
