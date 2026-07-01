package com.agentwallet.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * OKX DEX API client — real HTTP calls with HMAC signing.
 */
class OkxDexService(
    private val httpClient: HttpClient,
    private val auth: OkxAuth,
    private val baseUrl: String
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Get a swap quote from OKX DEX Aggregator.
     * Returns standard calldata that any wallet can sign.
     */
    suspend fun getSwapQuote(params: SwapQuoteParams): SwapQuote {
        val body = json.encodeToString(
            SwapQuoteParams.serializer(), params
        )

        val response: HttpResponse = httpClient.post("$baseUrl/api/v6/dex/aggregator/quote") {
            auth.sign(this, body)
            setBody(body)
        }

        val responseBody = response.bodyAsText()
        val jsonElement = json.parseToJsonElement(responseBody)
        val data = jsonElement.jsonObject["data"]?.jsonArray?.firstOrNull()
            ?: throw OkxApiException("No quote data in response: $responseBody")

        val code = jsonElement.jsonObject["code"]?.jsonPrimitive?.content
        if (code != "0") {
            val msg = jsonElement.jsonObject["msg"]?.jsonPrimitive?.content ?: "Unknown error"
            throw OkxApiException("OKX API error: $msg")
        }

        val tx = data.jsonObject["tx"]?.jsonObject ?: throw OkxApiException("No tx data in quote")

        return SwapQuote(
            fromToken = params.fromToken,
            toToken = params.toToken,
            toTokenAmount = data.jsonObject["toTokenAmount"]?.jsonPrimitive?.content ?: "0",
            txData = TxData(
                to = tx["to"]?.jsonPrimitive?.content ?: "",
                data = tx["data"]?.jsonPrimitive?.content ?: "",
                value = tx["value"]?.jsonPrimitive?.content ?: "0",
                gas = tx["gas"]?.jsonPrimitive?.content ?: "0",
                gasPrice = tx["gasPrice"]?.jsonPrimitive?.content ?: "0"
            ),
            router = data.jsonObject["router"]?.jsonPrimitive?.content ?: "OKX Aggregator"
        )
    }

    /**
     * Broadcast a signed transaction via OKX.
     */
    suspend fun broadcastTransaction(signedTx: String, chainId: String = "1"): BroadcastResult {
        val body = """[{"chainId":"$chainId","signedTx":"$signedTx"}]"""

        val response: HttpResponse = httpClient.post("$baseUrl/api/v6/dex/aggregator/broadcast") {
            auth.sign(this, body)
            setBody(body)
        }

        val jsonElement = json.parseToJsonElement(response.bodyAsText())
        val code = jsonElement.jsonObject["code"]?.jsonPrimitive?.content
        if (code != "0") {
            val msg = jsonElement.jsonObject["msg"]?.jsonPrimitive?.content ?: "Unknown error"
            throw OkxApiException("Broadcast failed: $msg")
        }

        val data = jsonElement.jsonObject["data"]?.jsonArray?.firstOrNull()
        val txHash = data?.jsonObject?.get("txHash")?.jsonPrimitive?.content
            ?: throw OkxApiException("No txHash in broadcast response")

        return BroadcastResult(txHash = txHash)
    }
}

class OkxApiException(message: String) : Exception(message)

@Serializable
data class SwapQuoteParams(
    val chainId: String,
    val fromTokenAddress: String,
    val toTokenAddress: String,
    val amount: String,
    val slippage: String
)

data class SwapQuote(
    val fromToken: String,
    val toToken: String,
    val toTokenAmount: String,
    val txData: TxData,
    val router: String
)

data class TxData(
    val to: String,
    val data: String,
    val value: String,
    val gas: String,
    val gasPrice: String
)

data class BroadcastResult(val txHash: String)
