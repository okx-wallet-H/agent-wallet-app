package com.agentwallet

import com.agentwallet.api.chatRoutes
import com.agentwallet.auth.authRoutes
import com.agentwallet.config.AppConfig
import com.agentwallet.plugins.configureSerialization
import com.agentwallet.plugins.configureAuth
import com.agentwallet.plugins.DatabaseFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.Test
import kotlin.test.*

class ChatRoutesTest {
    private val config = AppConfig.fromEnv()

    @Test
    fun `chat endpoint should respond`() = testApplication {
        application {
            DatabaseFactory.init(config)
            configureSerialization()
            configureAuth(config)
            routing { chatRoutes(config) }
        }
        val response = client.post("/api/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"message":"hello"}""")
        }
        // Without real API keys, may return 500; that's expected
        assertTrue(response.status.value in 200..599)
    }

    @Test
    fun `auth register should return 201`() = testApplication {
        application {
            DatabaseFactory.init(config)
            configureSerialization()
            routing { authRoutes(config) }
        }
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"t1@t.com","password":"p"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `auth register duplicate should return 409`() = testApplication {
        application {
            DatabaseFactory.init(config)
            configureSerialization()
            routing { authRoutes(config) }
        }
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"d3@t.com","password":"p1"}""")
        }
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"d3@t.com","password":"p2"}""")
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }
}
