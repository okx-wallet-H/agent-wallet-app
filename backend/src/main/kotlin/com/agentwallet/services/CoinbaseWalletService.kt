package com.agentwallet.services

import com.agentwallet.config.AppConfig
import io.ktor.client.*
import kotlinx.serialization.json.Json

class CoinbaseWalletService(
    private val config: AppConfig,
    private val httpClient: HttpClient = HttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun createWallet(userId: String): WalletInfo {
        // CDP REST API when configured, otherwise generate local IDs
        if (config.coinbaseSecretKey.isNotBlank()) {
            // TODO: Real CDP API call
        }
        return WalletInfo("cdp_${userId.take(12)}", "0x0000000000000000000000000000000000000000", "")
    }

    suspend fun signTransaction(walletId: String, txTo: String, txData: String, txValue: String, gas: String, gasPrice: String): SignedTx {
        if (config.coinbaseSecretKey.isNotBlank()) {
            // TODO: Real CDP signing
        }
        return SignedTx("0x02f8...", "0x${java.util.UUID.randomUUID().toString().replace("-", "").take(64)}")
    }

    suspend fun getBalance(walletId: String): WalletBalance {
        return WalletBalance(0.0, emptyList())
    }
}

class CoinbaseApiException(message: String) : Exception(message)

data class WalletInfo(val walletId: String, val evmAddress: String, val solanaAddress: String)
data class SignedTx(val signedPayload: String, val txHash: String)
data class WalletBalance(val totalUsd: Double, val tokens: List<TokenHolding>)
data class TokenHolding(val symbol: String, val amount: Double, val usdValue: Double)
