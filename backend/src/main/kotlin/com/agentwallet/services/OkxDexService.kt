package com.agentwallet.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OkxDexService(
    private val httpClient: HttpClient,
    private val auth: OkxAuth,
    private val baseUrl: String,
    private val builderCode: String = "yf83qce657mgxsjw"
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getSwapQuote(params: DexSwapParams): DexSwapQuote {
        val body = """
            [{"chainId":"${params.chainId}","fromTokenAddress":"${params.fromTokenAddress}","toTokenAddress":"${params.toTokenAddress}","amount":"${params.amount}","slippage":"${params.slippage}"}]
        """.trimIndent()

        val response = httpClient.post("$baseUrl/api/v6/dex/aggregator/quote") {
            auth.sign(this, body); setBody(body)
            header("OK-BUILDER-CODE", builderCode)
        }

        val text: String = response.body()
        val root = json.parseToJsonElement(text).jsonObject
        val code = root["code"]?.jsonPrimitive?.content ?: "1"
        if (code != "0") throw OkxApiException("OKX error: ${root["msg"]?.jsonPrimitive?.content}")

        val data = root["data"]?.jsonArray?.firstOrNull()?.jsonObject ?: throw OkxApiException("No data")
        val tx = data["tx"]?.jsonObject ?: throw OkxApiException("No tx")

        return DexSwapQuote(
            toTokenAmount = data["toTokenAmount"]?.jsonPrimitive?.content ?: "0",
            txTo = tx["to"]?.jsonPrimitive?.content ?: "",
            txData = tx["data"]?.jsonPrimitive?.content ?: "",
            txValue = tx["value"]?.jsonPrimitive?.content ?: "0",
            gas = tx["gas"]?.jsonPrimitive?.content ?: "0",
            gasPrice = tx["gasPrice"]?.jsonPrimitive?.content ?: "0",
            router = data["router"]?.jsonPrimitive?.content ?: "OKX"
        )
    }

    suspend fun broadcastTransaction(signedTx: String, chainId: String = "1"): String {
        val body = """[{"chainId":"$chainId","signedTx":"$signedTx"}]"""
        val response = httpClient.post("$baseUrl/api/v6/dex/aggregator/broadcast") {
            auth.sign(this, body); setBody(body)
            header("OK-BUILDER-CODE", builderCode)
        }
        val text: String = response.body()
        val root = json.parseToJsonElement(text).jsonObject
        if (root["code"]?.jsonPrimitive?.content != "0")
            throw OkxApiException("Broadcast failed: ${root["msg"]?.jsonPrimitive?.content}")

        return root["data"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("txHash")?.jsonPrimitive?.content ?: throw OkxApiException("No txHash")
    }
}

class OkxApiException(message: String) : Exception(message)

@Serializable
data class DexSwapParams(
    val chainId: String, val fromTokenAddress: String,
    val toTokenAddress: String, val amount: String, val slippage: String
)

data class DexSwapQuote(
    val toTokenAmount: String, val txTo: String, val txData: String,
    val txValue: String, val gas: String, val gasPrice: String, val router: String
)
