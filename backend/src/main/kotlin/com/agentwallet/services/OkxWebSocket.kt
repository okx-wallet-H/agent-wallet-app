package com.agentwallet.services

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class OkxWebSocket(
    private val apiKey: String,
    private val secretKey: String,
    private val passphrase: String,
    private val wsUrl: String = "wss://wsdex.okx.com/ws/v6/dex"
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var reconnectAttempts = 0

    fun signalStream(chainId: String): Flow<SignalData> = flow {
        val client = HttpClient { install(WebSockets) }

        while (currentCoroutineContext().isActive) {
            try {
                client.webSocket(wsUrl) {
                    val timestamp = (System.currentTimeMillis() / 1000).toString()
                    val signString = "$timestamp" + "GET" + "/users/self/verify"
                    val mac = Mac.getInstance("HmacSHA256")
                    mac.init(SecretKeySpec(secretKey.toByteArray(), "HmacSHA256"))
                    val signature = Base64.getEncoder().encodeToString(mac.doFinal(signString.toByteArray()))

                    val authMsg = buildJsonObject {
                        put("op", "login")
                        put("args", buildJsonArray {
                            add(buildJsonObject {
                                put("apiKey", apiKey); put("passphrase", passphrase)
                                put("timestamp", timestamp); put("sign", signature)
                            })
                        })
                    }
                    send(Frame.Text(authMsg.toString()))

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
                    reconnectAttempts = 0

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val root = json.parseToJsonElement(frame.readText()).jsonObject
                            val event = root["event"]?.jsonPrimitive?.content
                            if (event == "error") {
                                if ((root["msg"]?.jsonPrimitive?.content ?: "").contains("auth"))
                                    throw OkxWSAuthException("WS auth failed")
                                continue
                            }
                            val data = root["data"]?.jsonArray ?: continue
                            data.forEach { item ->
                                val obj = item.jsonObject
                                emit(SignalData(
                                    tokenAddress = obj["tokenAddress"]?.jsonPrimitive?.content ?: "",
                                    tokenSymbol = obj["tokenSymbol"]?.jsonPrimitive?.content ?: "",
                                    chainId = chainId,
                                    marketCapUsd = obj["marketCapUsd"]?.jsonPrimitive?.content ?: "0",
                                    amountUsd = obj["amountUsd"]?.jsonPrimitive?.content ?: "0",
                                    walletType = obj["walletType"]?.jsonPrimitive?.content ?: "",
                                    triggerWallets = obj["triggerWallets"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                                    soldRatio = obj["soldRatio"]?.jsonPrimitive?.content ?: "0"
                                ))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is OkxWSAuthException) throw e
                reconnectAttempts++
                delay(minOf(1000L * (1 shl minOf(reconnectAttempts, 5)), 30000L))
            }
        }
        client.close()
    }
}

class OkxWSAuthException(message: String) : Exception(message)
