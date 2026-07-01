package com.agentwallet.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*

/**
 * OKX Social / Sentiment API client.
 */
class OkxSocialService(
    private val httpClient: HttpClient,
    private val auth: OkxAuth,
    private val baseUrl: String
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Get social sentiment for a token.
     */
    suspend fun getSentiment(tokenAddress: String, chainId: String): SentimentData {
        val body = """[{"chainId":"$chainId","tokenAddress":"$tokenAddress"}]"""

        try {
            val response = httpClient.post("$baseUrl/api/v6/dex/market/social/sentiment") {
                auth.sign(this, body)
                setBody(body)
            }

            val root = json.parseToJsonElement(response.bodyAsText())
            val data = root.jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject

            return SentimentData(
                mentionCount24h = data?.get("mentionCount24h")?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                bullishRatio = data?.get("bullishRatio")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                topKols = data?.get("topKols")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                firstMentionedBy = data?.get("firstMentionedBy")?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            return SentimentData(0, 0.0, emptyList(), null)
        }
    }
}

data class SentimentData(
    val mentionCount24h: Int,
    val bullishRatio: Double,
    val topKols: List<String>,
    val firstMentionedBy: String?
)
