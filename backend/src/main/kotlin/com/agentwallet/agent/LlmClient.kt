package com.agentwallet.agent

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Abstract LLM client — swap implementations without changing Agent code.
 */
interface LlmClient {
    suspend fun chat(
        messages: List<LlmMessage>,
        tools: List<LlmTool>? = null,
        model: String = "claude-sonnet-5"
    ): LlmResponse

    suspend fun chatStreaming(
        messages: List<LlmMessage>,
        tools: List<LlmTool>? = null,
        model: String = "claude-sonnet-5"
    ): Flow<String>
}

// ─── Message Models ────────────────────────────────────

@Serializable
data class LlmMessage(
    val role: String,      // "user" | "assistant" | "system"
    val content: String,
    val toolCalls: List<LlmToolCall>? = null
)

@Serializable
data class LlmTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

@Serializable
data class LlmToolCall(
    val id: String,
    val name: String,
    val arguments: JsonObject
)

data class LlmResponse(
    val content: String?,
    val toolCalls: List<LlmToolCall>?
)

// ─── Anthropic Implementation ─────────────────────────

class AnthropicLlmClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://api.anthropic.com"
) : LlmClient {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    override suspend fun chat(
        messages: List<LlmMessage>,
        tools: List<LlmTool>?,
        model: String
    ): LlmResponse {
        val body = buildRequestBody(messages, tools, model, stream = false)

        val response: HttpResponse = httpClient.post("$baseUrl/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(body)
        }

        return parseResponse(response.bodyAsText())
    }

    override suspend fun chatStreaming(
        messages: List<LlmMessage>,
        tools: List<LlmTool>?,
        model: String
    ): Flow<String> = flow {
        val body = buildRequestBody(messages, tools, model, stream = true)

        httpClient.post("$baseUrl/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(body)
        }.bodyAsChannel().let { channel ->
            // Parse SSE stream
            // For MVP, emit placeholder chunks
            emit("Streaming response from $model...")
        }
    }

    private fun buildRequestBody(
        messages: List<LlmMessage>,
        tools: List<LlmTool>?,
        model: String,
        stream: Boolean
    ): String {
        val msgArray = messages.map { msg ->
            buildJsonObject {
                put("role", msg.role)
                put("content", msg.content)
            }
        }

        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", 4096)
            put("messages", JsonArray(msgArray))
            if (stream) put("stream", true)
            if (tools != null) {
                put("tools", JsonArray(tools.map { tool ->
                    buildJsonObject {
                        put("name", tool.name)
                        put("description", tool.description)
                        put("input_schema", tool.inputSchema)
                    }
                }))
            }
        }

        return body.toString()
    }

    private fun parseResponse(responseBody: String): LlmResponse {
        val root = json.parseToJsonElement(responseBody).jsonObject

        val content = root["content"]?.jsonArray?.firstOrNull()?.jsonObject
        val contentType = content?.get("type")?.jsonPrimitive?.content

        return when (contentType) {
            "text" -> LlmResponse(
                content = content?.get("text")?.jsonPrimitive?.content,
                toolCalls = null
            )
            "tool_use" -> {
                val toolUses = root["content"]?.jsonArray?.filter {
                    it.jsonObject["type"]?.jsonPrimitive?.content == "tool_use"
                } ?: emptyList()

                LlmResponse(
                    content = root["stop_reason"]?.jsonPrimitive?.content,
                    toolCalls = toolUses.map { tool ->
                        LlmToolCall(
                            id = tool.jsonObject["id"]?.jsonPrimitive?.content ?: "",
                            name = tool.jsonObject["name"]?.jsonPrimitive?.content ?: "",
                            arguments = tool.jsonObject["input"]?.jsonObject ?: JsonObject(emptyMap())
                        )
                    }
                )
            }
            else -> LlmResponse(content = responseBody, toolCalls = null)
        }
    }
}

// ─── Agent Tool Definitions ───────────────────────────

/**
 * Tools exposed to the LLM for function calling.
 */
object AgentTools {
    fun createStrategy() = LlmTool(
        name = "create_strategy",
        description = "为用户创建一个新的交易策略。参数包括策略名称、区块链、单笔限额、日限额。",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject { put("type", "string"); put("description", "策略名称") })
                put("chain", buildJsonObject { put("type", "string"); put("description", "区块链，如 Ethereum, Solana") })
                put("type", buildJsonObject {
                    put("type", "string")
                    put("enum", JsonArray(listOf(JsonPrimitive("SNIPER"), JsonPrimitive("DCA"), JsonPrimitive("GRID"))))
                })
                put("maxPerTx", buildJsonObject { put("type", "number"); put("description", "单笔上限(USD)") })
                put("dailyLimit", buildJsonObject { put("type", "number"); put("description", "日限额(USD)") })
                put("stopLoss", buildJsonObject { put("type", "number"); put("description", "止损比例，如 0.5 表示 50%") })
                put("takeProfit", buildJsonObject { put("type", "number"); put("description", "止盈比例，如 2.0 表示 2x") })
            })
            put("required", JsonArray(listOf(JsonPrimitive("name"), JsonPrimitive("chain"), JsonPrimitive("maxPerTx"), JsonPrimitive("dailyLimit"))))
        }
    )

    fun executeTrade() = LlmTool(
        name = "execute_trade",
        description = "执行一笔交易。quote 已由系统从 OKX DEX 获取。",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("fromToken", buildJsonObject { put("type", "string"); put("description", "卖出的代币") })
                put("toToken", buildJsonObject { put("type", "string"); put("description", "买入的代币") })
                put("amount", buildJsonObject { put("type", "number"); put("description", "卖出数量") })
                put("chain", buildJsonObject { put("type", "string"); put("description", "区块链 ID，如 1(Ethereum) 501(Solana)") })
                put("slippage", buildJsonObject { put("type", "number"); put("description", "滑点容忍度，如 0.03 表示 3%") })
            })
            put("required", JsonArray(listOf(JsonPrimitive("fromToken"), JsonPrimitive("toToken"), JsonPrimitive("amount"))))
        }
    )

    fun querySignals() = LlmTool(
        name = "query_signals",
        description = "查询最近的聪明钱/KOL/巨鲸买入信号。",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("chainId", buildJsonObject { put("type", "string"); put("description", "区块链 ID") })
                put("walletType", buildJsonObject { put("type", "string"); put("description", "1=聪明钱, 2=KOL, 3=巨鲸, 默认全部") })
            })
            put("required", JsonArray(listOf(JsonPrimitive("chainId"))))
        }
    )

    val all = listOf(createStrategy(), executeTrade(), querySignals())
}
