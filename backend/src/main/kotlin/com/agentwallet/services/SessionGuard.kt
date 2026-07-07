package com.agentwallet.services

import com.agentwallet.models.User
import com.agentwallet.models.UserRepository
import org.slf4j.LoggerFactory

/**
 * Session guard — ensures onchainos session is active before trading.
 * If expired, queues the trade and notifies user to re-verify.
 */
class SessionGuard(private val onchainos: OnchainosService) {
    private val logger = LoggerFactory.getLogger(SessionGuard::class.java)

    /**
     * Queue of pending trades waiting for user re-verification.
     * Key: userId, Value: trade details to execute after re-verify.
     */
    private val pendingTrades = java.util.concurrent.ConcurrentHashMap<String, PendingAction>()

    /**
     * Check if a user can trade. Returns true if session is active.
     * If not, queues the action and returns false.
     */
    suspend fun ensureSession(user: User, action: String = "trade"): SessionState {
        // Try onchainos session first
        val status = onchainos.status(user.id)
        val loggedIn = status.isOk() &&
            status.jsonData()?.jsonObject?.get("loggedIn")?.jsonPrimitive?.content == "true"

        if (loggedIn) return SessionState.ACTIVE

        // Session expired — try to refresh silently
        val refreshResult = trySilentRefresh(user)
        if (refreshResult) return SessionState.ACTIVE

        // Need manual re-verification
        logger.info("Session expired for ${user.email}, queuing $action")
        return SessionState.EXPIRED
    }

    /** Try to re-establish session without user interaction (not always possible). */
    private suspend fun trySilentRefresh(user: User): Boolean {
        // onchainos doesn't support silent refresh — sessions expire and need OTP
        return false
    }

    fun queuePending(userId: String, action: PendingAction) {
        pendingTrades[userId] = action
    }

    fun getPending(userId: String): PendingAction? = pendingTrades.remove(userId)
}

data class PendingAction(
    val type: String,       // "trade" | "copy" | "signal_execute"
    val details: String,    // Human-readable description
    val executeAfterVerify: suspend () -> Unit
)

enum class SessionState { ACTIVE, EXPIRED }
