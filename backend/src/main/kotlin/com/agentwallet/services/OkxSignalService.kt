package com.agentwallet.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

/**
 * OKX Signal / Trenches / Token API client — real HTTP calls.
 */
class OkxSignalService(
    private val httpClient: HttpClient,
    private val auth: OkxAuth,
    private val baseUrl: String
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Get recent signal list from OKX.
     * POST /api/v6/dex/market/signal/list
     */
    suspend fun getSignals(chainId: String, walletType: String = "1,2,3"): List<SignalData> {
        val body = """
            [{
                "chainId": "$chainId",
                "walletType": "$walletType",
                "minAmountUsd": "1000",
                "minAddressCount": "2",
                "limit": "20"
            }]
        """.trimIndent()

        val response: io.ktor.client.statement.HttpResponse =
            httpClient.post("$baseUrl/api/v6/dex/market/signal/list") {
                auth.sign(this, body)
                setBody(body)
            }

        val root = json.parseToJsonElement(response.bodyAsText())
        val code = root.jsonObject["code"]?.jsonPrimitive?.content
        if (code != "0") throw OkxApiException("Signal API error: ${root.jsonObject["msg"]?.jsonPrimitive?.content}")

        return root.jsonObject["data"]?.jsonArray?.map { item ->
            val obj = item.jsonObject
            SignalData(
                tokenAddress = obj["tokenAddress"]?.jsonPrimitive?.content ?: "",
                tokenSymbol = obj["tokenSymbol"]?.jsonPrimitive?.content ?: "",
                chainId = chainId,
                marketCapUsd = obj["marketCapUsd"]?.jsonPrimitive?.content ?: "0",
                amountUsd = obj["amountUsd"]?.jsonPrimitive?.content ?: "0",
                walletType = obj["walletType"]?.jsonPrimitive?.content ?: "",
                triggerWallets = obj["triggerWallets"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                soldRatio = obj["soldRatio"]?.jsonPrimitive?.content ?: "0"
            )
        } ?: emptyList()
    }

    /**
     * Get token security analysis.
     * POST /api/v6/dex/market/token/advanced-info
     */
    suspend fun getTokenAnalysis(chainId: String, tokenAddress: String): TokenAnalysis {
        val body = """[{"chainId":"$chainId","tokenAddress":"$tokenAddress"}]"""

        val response = httpClient.post("$baseUrl/api/v6/dex/market/token/advanced-info") {
            auth.sign(this, body)
            setBody(body)
        }

        val root = json.parseToJsonElement(response.bodyAsText())
        val data = root.jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject

        return TokenAnalysis(
            riskControlLevel = data?.get("riskControlLevel")?.jsonPrimitive?.content ?: "unknown",
            tokenTags = data?.get("tokenTags")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            holders = data?.get("holders")?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            top10HolderRatio = data?.get("top10HolderRatio")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            bundleHoldingRatio = data?.get("bundleHoldingRatio")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            liquidityLocked = data?.get("liquidityLocked")?.jsonPrimitive?.content == "true"
        )
    }

    /**
     * Get developer reputation profile.
     */
    suspend fun getDevProfile(chainId: String, devAddress: String): DevProfile {
        val body = """[{"chainId":"$chainId","devAddress":"$devAddress"}]"""

        try {
            val response = httpClient.post("$baseUrl/api/v6/dex/market/trenches/dev-profile") {
                auth.sign(this, body)
                setBody(body)
            }
            val root = json.parseToJsonElement(response.bodyAsText())
            val data = root.jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject

            return DevProfile(
                totalLaunches = data?.get("totalLaunches")?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                rugCount = data?.get("rugCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                avgLifetime = data?.get("avgLifetime")?.jsonPrimitive?.content ?: "unknown",
                rugHistory = (data?.get("rugCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0) > 0
            )
        } catch (e: Exception) {
            // Trenches API may not be available in all environments
            return DevProfile(0, 0, "unknown", false)
        }
    }

    /**
     * Connect to real-time signal WebSocket.
     * Returns a cold Flow that emits signals as they arrive.
     * Uses OkxWebSocket with auto-reconnect and exponential backoff.
     */
    fun signalStream(chainId: String): Flow<SignalData> {
        val ws = OkxWebSocket(
            apiKey = apiKey,
            secretKey = secretKey,
            passphrase = passphrase
        )
        return ws.signalStream(chainId)
    }
}

// ─── Data classes ─────────────────────────────────────

data class SignalData(
    val tokenAddress: String,
    val tokenSymbol: String,
    val chainId: String,
    val marketCapUsd: String,
    val amountUsd: String,
    val walletType: String,
    val triggerWallets: List<String>,
    val soldRatio: String
)

data class DevProfile(
    val totalLaunches: Int,
    val rugCount: Int,
    val avgLifetime: String,
    val rugHistory: Boolean
)

data class TokenAnalysis(
    val riskControlLevel: String,
    val tokenTags: List<String>,
    val holders: Int,
    val top10HolderRatio: Double,
    val bundleHoldingRatio: Double,
    val liquidityLocked: Boolean
)
