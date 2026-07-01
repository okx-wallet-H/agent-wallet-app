package com.agentwallet

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.junit.Test
import kotlin.test.*

class ChatRoutesTest {

    @Test
    fun `chat endpoint should return 200 with valid request`() = testApplication {
        application {
            // Use a minimal app config for testing
            com.agentwallet.config.AppConfig.fromEnv()
        }

        val response = client.post("/api/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"message":"你好","conversationId":null}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        val json = Json.parseToJsonElement(body).jsonObject
        assertTrue(json.containsKey("conversationId"))
        assertTrue(json.containsKey("messages"))
    }

    @Test
    fun `auth register should return 201 with valid email`() = testApplication {
        application {
            com.agentwallet.config.AppConfig.fromEnv()
        }

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@test.com","password":"password123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(json.containsKey("token"))
        assertTrue(json.containsKey("user"))
    }

    @Test
    fun `auth register duplicate email should return 409`() = testApplication {
        application {
            com.agentwallet.config.AppConfig.fromEnv()
        }

        // First registration
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"dup@test.com","password":"password123"}""")
        }

        // Duplicate
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"dup@test.com","password":"password456"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }
}
