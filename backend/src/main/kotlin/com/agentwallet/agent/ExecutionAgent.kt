package com.agentwallet.agent

import com.agentwallet.models.TradeIntent
import com.agentwallet.services.*

class ExecutionAgent(
    private val okxDex: OkxDexService,
    private val coinbaseWallet: CoinbaseWalletService
) {
    suspend fun executeSwap(userId: String, walletId: String, intent: TradeIntent): ExecutionResult {
        val quote = okxDex.getSwapQuote(
            DexSwapParams(
                chainId = intent.chain,
                fromTokenAddress = intent.fromToken,
                toTokenAddress = intent.toToken,
                amount = intent.amount.toString(),
                slippage = intent.slippage.toString()
            )
        )
        val signed = coinbaseWallet.signTransaction(
            walletId, quote.txTo, quote.txData, quote.txValue, quote.gas, quote.gasPrice
        )
        val txHash = okxDex.broadcastTransaction(signed.signedPayload, intent.chain)

        return ExecutionResult(
            success = true, txHash = txHash,
            fromToken = intent.fromToken, toToken = intent.toToken,
            amount = intent.amount, price = quote.toTokenAmount
        )
    }
}

data class ExecutionResult(
    val success: Boolean, val txHash: String,
    val fromToken: String, val toToken: String,
    val amount: Double, val price: String, val error: String? = null
)
