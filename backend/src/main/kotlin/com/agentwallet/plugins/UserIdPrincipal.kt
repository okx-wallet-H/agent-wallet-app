package com.agentwallet.plugins

import io.ktor.server.auth.Principal

data class UserIdPrincipal(val userId: String, val email: String) : Principal
