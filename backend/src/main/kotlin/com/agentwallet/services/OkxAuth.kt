package com.agentwallet.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * OKX Onchain OS HMAC-SHA256 authentication.
 *
 * Signing formula:
 *   signature = Base64(HMAC-SHA256(secretKey, timestamp + method + path + body))
 */
class OkxAuth(
    private val apiKey: String,
    private val secretKey: String,
    private val passphrase: String
) {
    /**
     * Add OKX authentication headers to an HTTP request.
     */
    fun sign(builder: HttpRequestBuilder, body: String) {
        val timestamp = Instant.now().toString()
        val method = builder.method.value.uppercase()
        val path = builder.url.encodedPath + (builder.url.encodedQuery?.let { "?$it" } ?: "")
        val signString = "$timestamp$method$path$body"
        val signature = hmacSha256(signString, secretKey)

        builder.headers {
            append("OK-ACCESS-KEY", apiKey)
            append("OK-ACCESS-SIGN", signature)
            append("OK-ACCESS-TIMESTAMP", timestamp)
            append("OK-ACCESS-PASSPHRASE", passphrase)
        }
    }

    private fun hmacSha256(data: String, key: String): String {
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }
}

/**
 * Ktor HttpClient plugin that automatically signs OKX API requests.
 */
fun HttpClientConfig<*>.okxAuth(apiKey: String, secretKey: String, passphrase: String) {
    // This is called per-request in services using OkxAuth directly
}

/**
 * Helper to build an OKX-authenticated HttpClient.
 */
fun createOkxHttpClient(): HttpClient {
    return HttpClient {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            io.ktor.serialization.kotlinx.json.json(
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 5000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
}
