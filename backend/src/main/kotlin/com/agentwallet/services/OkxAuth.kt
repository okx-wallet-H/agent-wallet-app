package com.agentwallet.services

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class OkxAuth(
    private val apiKey: String,
    private val secretKey: String,
    private val passphrase: String
) {
    fun sign(builder: HttpRequestBuilder, body: String) {
        val timestamp = Instant.now().toString()
        val method = builder.method.value.uppercase()
        val params = builder.url.parameters.entries().joinToString("&") { (k, v) -> "$k=${v.joinToString(",")}" }
        val path = builder.url.encodedPath + (if (params.isNotEmpty()) "?$params" else "")
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
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)))
    }
}

fun createOkxHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15000; connectTimeoutMillis = 5000
    }
}
