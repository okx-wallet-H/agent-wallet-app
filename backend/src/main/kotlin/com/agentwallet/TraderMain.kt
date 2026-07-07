package com.agentwallet

import com.agentwallet.agent.*
import com.agentwallet.services.*
import io.ktor.client.*
import kotlinx.coroutines.*

/**
 * Standalone entry point for running a single AI Trader in its own container.
 * Usage: java -jar trader.jar <trader-id>
 *
 * Each trader = independent container, independent knowledge base, independent Claude session.
 */
fun main(args: Array<String>) {
    val traderId = args.firstOrNull() ?: "solana-sniper"

    // Build the specific trader profile
    val profile = AiTraderEngine(
        AnthropicLlmClient(HttpClient(), System.getenv("ANTHROPIC_API_KEY") ?: ""),
        OkxSignalService(
            System.getenv("OKX_API_KEY") ?: "", System.getenv("OKX_SECRET_KEY") ?: "",
            System.getenv("OKX_PASSPHRASE") ?: "", System.getenv("OKX_BASE_URL") ?: "https://web3.okx.com"
        ),
        OkxSignalService(System.getenv("OKX_API_KEY") ?: "", System.getenv("OKX_SECRET_KEY") ?: "",
            System.getenv("OKX_PASSPHRASE") ?: "", System.getenv("OKX_BASE_URL") ?: "https://web3.okx.com")
    ).traders.find { it.id == traderId } ?: run {
        println("Unknown trader: $traderId")
        println("Available: solana-sniper, smart-money, kol-radar, whale-watcher")
        return
    }

    val llmClient = AnthropicLlmClient(HttpClient(), System.getenv("ANTHROPIC_API_KEY") ?: "")
    val signalService = OkxSignalService(
        System.getenv("OKX_API_KEY") ?: "", System.getenv("OKX_SECRET_KEY") ?: "",
        System.getenv("OKX_PASSPHRASE") ?: "", System.getenv("OKX_BASE_URL") ?: "https://web3.okx.com"
    )

    val trader = AiTraderContainer(profile, llmClient, signalService)

    println("=== AI Trader Container ===")
    println("ID:      ${profile.id}")
    println("Name:    ${profile.name}")
    println("Chain:   ${profile.chain}")
    println("Source:  ${profile.dataSource}")
    println("===========================")

    runBlocking { trader.run(this) }
}
