package com.agentwallet.api

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

/**
 * WebSocket endpoint for real-time push notifications to Android client.
 * WS /ws/notifications
 */
fun Application.webSocketRoutes() {
    routing {
        webSocket("/ws/notifications") {
            // TODO: Authenticate via token in query param
            send(Frame.Text("{\"type\":\"connected\",\"message\":\"已连接到通知服务\"}"))

            // In production, subscribe to Redis pub/sub for this user's events
            // and forward them here.

            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            // Client ping → server pong
                            if (frame.readText() == "ping") {
                                send(Frame.Text("pong"))
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                application.environment.log.error("WebSocket error", e)
            }
        }
    }
}
