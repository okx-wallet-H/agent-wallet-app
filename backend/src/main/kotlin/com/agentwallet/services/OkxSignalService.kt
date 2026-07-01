package com.agentwallet.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

class OkxSignalService(
    val apiKey: String,
    val secretKey: String,
    val passphrase: String,
    private val baseUrl: String
) {
    private val httpClient = createOkxHttpClient()
    private val auth = OkxAuth(apiKey, secretKey, passphrase)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getSignals(chainId: String, walletType: String = "1,2,3"): List<SignalData> {
        if (apiKey.isBlank()) return emptyList()
        val body = """[{"chainId":"$chainId","walletType":"$walletType","minAmountUsd":"1000","minAddressCount":"2","limit":"20"}]"""

        val response: HttpResponse = httpClient.post("$baseUrl/api/v6/dex/market/signal/list") {
            auth.sign(this, body)
            setBody(body)
        }
        val responseText: String = response.body()
        val root = json.parseToJsonElement(responseText).jsonObject
        if (root["code"]?.jsonPrimitive?.content != "0") return emptyList()

        return root["data"]?.jsonArray?.map { item ->
            val obj = item.jsonObject
            SignalData(
                tokenAddress = obj["tokenAddress"]?.jsonPrimitive?.content ?: "",
                tokenSymbol = obj["tokenSymbol"]?.jsonPrimitive?.content ?: "",
                chainId = chainId, marketCapUsd = obj["marketCapUsd"]?.jsonPrimitive?.content ?: "0",
                amountUsd = obj["amountUsd"]?.jsonPrimitive?.content ?: "0",
                walletType = obj["walletType"]?.jsonPrimitive?.content ?: "",
                triggerWallets = obj["triggerWallets"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                soldRatio = obj["soldRatio"]?.jsonPrimitive?.content ?: "0"
            )
        } ?: emptyList()
    }

    suspend fun getTokenAnalysis(chainId: String, tokenAddress: String): TokenAnalysis {
        if (apiKey.isBlank()) return TokenAnalysis("unknown", emptyList(), 0, 0.0, 0.0, false)
        val body = """[{"chainId":"$chainId","tokenAddress":"$tokenAddress"}]"""
        val response = httpClient.post("$baseUrl/api/v6/dex/market/token/advanced-info") {
            auth.sign(this, body); setBody(body)
        }
        val text: String = response.body()
        val root = json.parseToJsonElement(text).jsonObject
        val data = root["data"]?.jsonArray?.firstOrNull()?.jsonObject
        return TokenAnalysis(
            riskControlLevel = data?.get("riskControlLevel")?.jsonPrimitive?.content ?: "unknown",
            tokenTags = data?.get("tokenTags")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            holders = data?.get("holders")?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            top10HolderRatio = data?.get("top10HolderRatio")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            bundleHoldingRatio = data?.get("bundleHoldingRatio")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            liquidityLocked = data?.get("liquidityLocked")?.jsonPrimitive?.content == "true"
        )
    }

    suspend fun getDevProfile(chainId: String, devAddress: String): DevProfile {
        if (apiKey.isBlank()) return DevProfile(0, 0, "unknown", false)
        val body = """[{"chainId":"$chainId","devAddress":"$devAddress"}]"""
        try {
            val response = httpClient.post("$baseUrl/api/v6/dex/market/trenches/dev-profile") {
                auth.sign(this, body); setBody(body)
            }
            val data = json.parseToJsonElement(response.body<String>()).jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject
            return DevProfile(
                totalLaunches = data?.get("totalLaunches")?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                rugCount = data?.get("rugCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                avgLifetime = data?.get("avgLifetime")?.jsonPrimitive?.content ?: "unknown",
                rugHistory = (data?.get("rugCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0) > 0
            )
        } catch (e: Exception) { return DevProfile(0, 0, "unknown", false) }
    }

    fun signalStream(chainId: String): Flow<SignalData> {
        if (apiKey.isBlank()) return emptyFlow()
        val ws = OkxWebSocket(apiKey, secretKey, passphrase)
        return ws.signalStream(chainId)
    }
}

data class SignalData(
    val tokenAddress: String, val tokenSymbol: String, val chainId: String,
    val marketCapUsd: String, val amountUsd: String, val walletType: String,
    val triggerWallets: List<String>, val soldRatio: String
)
data class DevProfile(val totalLaunches: Int, val rugCount: Int, val avgLifetime: String, val rugHistory: Boolean)
data class TokenAnalysis(
    val riskControlLevel: String, val tokenTags: List<String>, val holders: Int,
    val top10HolderRatio: Double, val bundleHoldingRatio: Double, val liquidityLocked: Boolean
)
