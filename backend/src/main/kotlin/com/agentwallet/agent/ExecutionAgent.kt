package com.agentwallet.agent

import com.agentwallet.models.TradeIntent
import com.agentwallet.risk.RiskEngine
import com.agentwallet.services.*

/**
 * Execution agent — gets swap quote → signs → broadcasts.
 * No LLM calls — deterministic logic only.
 */
class ExecutionAgent(
    private val okxDex: OkxDexService,
    private val coinbaseWallet: CoinbaseWalletService
) {
    /**
     * Execute a swap: quote → sign → broadcast.
     */
    suspend fun executeSwap(
        userId: String,
        walletId: String,
        intent: TradeIntent
    ): ExecutionResult {
        // 1. Get quote from OKX DEX
        val quote = okxDex.getSwapQuote(
            SwapQuoteParams(
                chainId = intent.chain,
                fromToken = intent.fromToken,
                toToken = intent.toToken,
                amount = intent.amount.toString(),
                slippage = intent.slippage.toString()
            )
        )

        // 2. Sign with Coinbase wallet
        val signed = coinbaseWallet.signTransaction(walletId, quote.txData)

        // 3. Broadcast
        val result = okxDex.broadcastTransaction(signed.signedPayload)

        return ExecutionResult(
            success = true,
            txHash = result.txHash,
            fromToken = quote.fromToken,
            toToken = quote.toToken,
            amount = intent.amount,
            price = quote.toTokenAmount
        )
    }
}

data class ExecutionResult(
    val success: Boolean,
    val txHash: String,
    val fromToken: String,
    val toToken: String,
    val amount: Double,
    val price: String,
    val error: String? = null
)
