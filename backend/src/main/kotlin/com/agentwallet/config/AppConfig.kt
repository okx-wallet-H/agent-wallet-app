package com.agentwallet.config

data class AppConfig(
    val host: String,
    val port: Int,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtRealm: String,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val redisHost: String,
    val redisPort: Int,
    val okxApiKey: String,
    val okxSecretKey: String,
    val okxPassphrase: String,
    val okxBaseUrl: String,
    val coinbaseApiKey: String,
    val coinbaseSecretKey: String,
    val anthropicApiKey: String,
    val llmModel: String
) {
    /** Detect if we're in production mode (all external services configured). */
    fun isProduction(): Boolean {
        return dbUrl.contains("postgresql") && !anthropicApiKey.isBlank()
    }

    companion object {
        fun fromEnv(): AppConfig = AppConfig(
            host       = env("HOST", "0.0.0.0"),
            port       = env("PORT", "8080").toInt(),
            jwtSecret  = env("JWT_SECRET", "dev-secret-change-in-production-32chars"),
            jwtIssuer  = env("JWT_ISSUER", "agent-wallet"),
            jwtAudience = env("JWT_AUDIENCE", "agent-wallet-users"),
            jwtRealm   = env("JWT_REALM", "Agent Wallet"),
            dbUrl      = env("DB_URL", "jdbc:h2:mem:agentwallet"),
            dbUser     = env("DB_USER", "sa"),
            dbPassword = env("DB_PASSWORD", ""),
            redisHost  = env("REDIS_HOST", "localhost"),
            redisPort  = env("REDIS_PORT", "6379").toInt(),
            okxApiKey     = env("OKX_API_KEY", ""),
            okxSecretKey  = env("OKX_SECRET_KEY", ""),
            okxPassphrase = env("OKX_PASSPHRASE", ""),
            okxBaseUrl    = env("OKX_BASE_URL", "https://web3.okx.com"),
            coinbaseApiKey    = env("COINBASE_API_KEY", ""),
            coinbaseSecretKey = env("COINBASE_SECRET_KEY", ""),
            anthropicApiKey   = env("ANTHROPIC_API_KEY", ""),
            llmModel          = env("LLM_MODEL", "claude-sonnet-5")
        )

        private fun env(key: String, default: String): String =
            System.getenv(key) ?: default
    }
}
