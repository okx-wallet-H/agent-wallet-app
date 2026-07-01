package com.agentwallet.ui.components

import androidx.compose.runtime.Composable

/**
 * All message types that can appear in the chat stream.
 */
sealed class ChatMessage {

    /** Simple text message from user or agent. */
    data class Text(
        val content: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    /** Inline trade confirmation card — user must swipe to approve. */
    data class TradeConfirmation(
        val token: String,
        val chain: String,
        val amount: String,
        val fromToken: String,
        val toAmount: String,
        val slippage: String,
        val gasEstimate: String,
        val checks: List<SecurityCheck>,
        val onConfirm: () -> Unit = {},
        val onReject: () -> Unit = {}
    ) : ChatMessage()

    /** Post-execution trade result (success or failure). */
    data class TradeResult(
        val token: String,
        val action: TradeAction,
        val amount: String,
        val price: String,
        val txHash: String,
        val status: TxStatus
    ) : ChatMessage()

    /** Real-time signal alert pushed by SignalAgent. */
    data class SignalAlert(
        val token: String,
        val chain: String,
        val amountUsd: String,
        val walletCount: Int,
        val devReputation: Boolean,
        val bundleRatio: String,
        val lpLocked: Boolean,
        val confidence: String,
        val isNew: Boolean
    ) : ChatMessage()

    /** Strategy status card showing running/stopped state. */
    data class StrategyCard(
        val name: String,
        val chain: String,
        val status: StrategyStatus,
        val runtime: String,
        val trades: Int,
        val pnl: String,
        val onPause: () -> Unit = {},
        val onEdit: () -> Unit = {},
        val onStop: () -> Unit = {}
    ) : ChatMessage()

    /** Agent reasoning steps — collapsible. */
    data class Thinking(
        val steps: List<ThinkingStep>,
        val isCollapsed: Boolean = true
    ) : ChatMessage()
}

// ─── Enums ────────────────────────────────────────────

enum class TradeAction { BUY, SELL }

enum class TxStatus { PENDING, CONFIRMED, FAILED }

enum class StrategyStatus { RUNNING, PAUSED, STOPPED }

enum class StepStatus { COMPLETED, IN_PROGRESS, PENDING }

// ─── Supporting data classes ──────────────────────────

data class SecurityCheck(
    val label: String,
    val passed: Boolean
)

data class ThinkingStep(
    val label: String,
    val status: StepStatus
)
