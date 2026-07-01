package com.agentwallet.services

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * OKX WebSocket real-time signal stream.
 * Connects to wss://wsdex.okx.com/ws/v6/dex and subscribes to the signal channel.
 */
class OkxWebSocket(
    private val apiKey: String,
    private val secretKey: String,
    private val passphrase: String,
    private val wsUrl: String = "wss://wsdex.okx.com/ws/v6/dex"
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var reconnectAttempts = 0

    /**
     * Connect to OKX WebSocket and stream signals.
     * Emits SignalData events as they arrive. Auto-reconnects on disconnect.
     */
    fun signalStream(chainId: String): Flow<SignalData> = flow {
        val client = HttpClient { install(WebSockets) }

        while (currentCoroutineContext().isActive) {
            try {
                client.webSocket(wsUrl) {
                    // 1. Authenticate
                    val timestamp = Instant.now().epochSecond.toString()
                    val signature = signAuth(timestamp)

                    val authMsg = buildJsonObject {
                        put("op", "login")
                        put("args", buildJsonArray {
                            add(buildJsonObject {
                                put("apiKey", apiKey)
                                put("passphrase", passphrase)
                                put("timestamp", timestamp)
                                put("sign", signature)
                            })
                        })
                    }
                    send(Frame.Text(authMsg.toString()))

                    // 2. Subscribe to signal channel
                    val subMsg = buildJsonObject {
                        put("op", "subscribe")
                        put("args", buildJsonArray {
                            add(buildJsonObject {
                                put("channel", "dex-market-new-signal-openapi")
                                put("chainId", chainId)
                            })
                        })
                    }
                    send(Frame.Text(subMsg.toString()))

                    reconnectAttempts = 0 // Reset on successful connection

                    // 3. Process incoming frames
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val root = json.parseToJsonElement(text).jsonObject

                            // Handle login response
                            val event = root["event"]?.jsonPrimitive?.content
                            if (event == "login" && root["code"]?.jsonPrimitive?.content == "0") {
                                continue // Login successful
                            }
                            if (event == "error") {
                                val msg = root["msg"]?.jsonPrimitive?.content ?: "Unknown WS error"
                                if (msg.contains("login") || msg.contains("auth")) {
                                    throw OkxWSAuthException("WebSocket auth failed: $msg")
                                }
                                continue
                            }

                            // Parse signal data from push
                            val data = root["data"]?.jsonArray
                            if (data != null) {
                                data.forEach { item ->
                                    val obj = item.jsonObject
                                    emit(
                                        SignalData(
                                            tokenAddress = obj["tokenAddress"]?.jsonPrimitive?.content ?: "",
                                            tokenSymbol = obj["tokenSymbol"]?.jsonPrimitive?.content ?: "",
                                            chainId = chainId,
                                            marketCapUsd = obj["marketCapUsd"]?.jsonPrimitive?.content ?: "0",
                                            amountUsd = obj["amountUsd"]?.jsonPrimitive?.content ?: "0",
                                            walletType = obj["walletType"]?.jsonPrimitive?.content ?: "",
                                            triggerWallets = obj["triggerWallets"]?.jsonArray
                                                ?.map { it.jsonPrimitive.content } ?: emptyList(),
                                            soldRatio = obj["soldRatio"]?.jsonPrimitive?.content ?: "0"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is OkxWSAuthException -> throw e // Don't reconnect on auth failure
                    else -> {
                        reconnectAttempts++
                        val delay = minOf(1000L * (1 shl minOf(reconnectAttempts, 5)), 30000L)
                        // Exponential backoff: 1s, 2s, 4s, 8s, 16s, 30s, 30s...
                        delay(delay)
                    }
                }
            }
        }

        client.close()
    }

    private fun signAuth(timestamp: String): String {
        val signString = "$timestampGET/users/self/verify"
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        return Base64.getEncoder().encodeToString(mac.doFinal(signString.toByteArray()))
    }
}

class OkxWSAuthException(message: String) : Exception(message)
