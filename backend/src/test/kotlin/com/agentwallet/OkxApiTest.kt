package com.agentwallet

import com.agentwallet.config.DotEnv
import com.agentwallet.services.*
import kotlinx.coroutines.runBlocking

/**
 * Quick smoke test for OKX APIs using real keys from .env.
 * Run manually: ./gradlew test --tests OkxApiTest
 */
fun main() = runBlocking {
    DotEnv.load(".env")

    val apiKey = System.getProperty("OKX_API_KEY") ?: ""
    val secretKey = System.getProperty("OKX_SECRET_KEY") ?: ""
    val passphrase = System.getProperty("OKX_PASSPHRASE") ?: ""

    if (apiKey.isBlank()) {
        println("❌ OKX_API_KEY not set. Skipping tests.")
        return@runBlocking
    }

    println("=== Testing OKX Signal API ===")
    val signalService = OkxSignalService(apiKey, secretKey, passphrase, "https://web3.okx.com")
    try {
        val signals = signalService.getSignals("501", "1") // Solana, smart money
        println("✅ Signal API: ${signals.size} signals received")
        signals.take(3).forEach { s ->
            println("  📡 ${s.tokenSymbol} — $${s.amountUsd} — ${s.triggerWallets.size} wallets — sold ${s.soldRatio}%")
        }
    } catch (e: Exception) {
        println("❌ Signal API failed: ${e.message}")
    }

    println()
    println("=== Testing OKX DEX Quote API ===")
    val dexService = OkxDexService(
        createOkxHttpClient(),
        OkxAuth(apiKey, secretKey, passphrase),
        "https://web3.okx.com"
    )
    try {
        val quote = dexService.getSwapQuote(
            DexSwapParams(
                chainId = "1",
                fromTokenAddress = "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", // ETH
                toTokenAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",   // USDC
                amount = "10000000000000000",  // 0.01 ETH in wei
                slippage = "0.03"
            )
        )
        println("✅ DEX Quote: ${quote.toTokenAmount} USDC via ${quote.router}")
        println("  txTo: ${quote.txTo.take(30)}...")
        println("  gas: ${quote.gas}")
    } catch (e: Exception) {
        println("❌ DEX Quote failed: ${e.message}")
    }

    println()
    println("=== Testing OKX WebSocket ===")
    val ws = OkxWebSocket(apiKey, secretKey, passphrase)
    try {
        var count = 0
        ws.signalStream("501").collect { signal ->
            println("📡 WS: ${signal.tokenSymbol} — $${signal.amountUsd}")
            count++
            if (count >= 3) throw kotlinx.coroutines.CancellationException("Done")
        }
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) println("✅ WebSocket: received signals")
        else println("❌ WebSocket failed: ${e.message}")
    }
}
