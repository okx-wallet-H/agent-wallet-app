package com.agentwallet.services

import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Wraps the onchainos CLI for multi-user OKX Agentic Wallet management.
 *
 * Each user gets their own session directory ($sessionsDir/$userId).
 * The CLI's session tokens (accessToken/refreshToken) are stored per-directory,
 * providing complete multi-user wallet isolation.
 *
 * All wallets are created inside OKX's TEE — private keys never touch our server.
 */
class OnchainosService(
    private val sessionsDir: String = "/opt/onchainos-sessions",
    private val cliPath: String = "onchainos"
) {
    private val logger = LoggerFactory.getLogger(OnchainosService::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ─── Auth Flow ──────────────────────────────────────

    /**
     * Step 1: Send verification code to user's email.
     * Returns: {"ok": true, "data": {}} on success.
     */
    fun sendOtp(userId: String, email: String): OnchainosResult {
        val home = sessionHome(userId)
        return exec(home, "wallet", "login", email, "--locale", "zh_CN")
    }

    /**
     * Step 2: Verify OTP code. OKX auto-creates the wallet on first login.
     * Returns accountId, accountName, isNew on success.
     */
    fun verifyOtp(userId: String, otp: String): OnchainosResult {
        val home = sessionHome(userId)
        return exec(home, "wallet", "verify", otp)
    }

    // ─── Wallet Operations ──────────────────────────────

    /** Get wallet status (loggedIn, accountCount, etc). */
    fun status(userId: String): OnchainosResult {
        val home = sessionHome(userId)
        return exec(home, "wallet", "status")
    }

    /** Get wallet addresses (EVM + Solana). JSON by default. */
    fun getAddresses(userId: String): OnchainosResult {
        val home = sessionHome(userId)
        return exec(home, "wallet", "addresses")
    }

    /** Get wallet balances. */
    fun getBalances(userId: String): OnchainosResult {
        val home = sessionHome(userId)
        return exec(home, "wallet", "balance")
    }

    // ─── CLI Execution ──────────────────────────────────

    private fun sessionHome(userId: String): File {
        val dir = File(sessionsDir, userId)
        dir.mkdirs()
        return dir
    }

    private fun exec(homeDir: File, vararg args: String): OnchainosResult {
        val cmd = listOf(cliPath) + args.toList()
        val pb = java.lang.ProcessBuilder(cmd)
        pb.directory(homeDir)
        pb.environment()["HOME"] = homeDir.absolutePath
        pb.environment()["PATH"] = System.getenv("PATH") ?: "/usr/local/bin:/usr/bin:/bin"
        pb.redirectErrorStream(true)

        val process = pb.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        return OnchainosResult(exitCode, output)
    }
}

data class OnchainosResult(
    val exitCode: Int,
    val output: String
) {
    fun isOk(): Boolean = exitCode == 0
    fun isConfirming(): Boolean = exitCode == 2
    fun jsonData(): JsonElement? = try {
        Json.parseToJsonElement(output).jsonObject["data"]
    } catch (e: Exception) { null }
}
