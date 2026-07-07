package com.agentwallet.agent

import kotlinx.serialization.json.*

/**
 * Conversation agent — powered by LLM function calling.
 * Replaces the previous keyword-matching approach.
 */
class ChatAgent(
    private val llmClient: LlmClient,
    private val model: String = "claude-sonnet-5"
) {

    /**
     * Build the system prompt with user context injected.
     */
    fun buildSystemPrompt(userContext: UserContext): String = """
你是 OKX Agent Wallet 的 AI 助手，帮用户管理加密资产和交易。

当前用户信息：
- EVM 钱包地址: ${userContext.evmAddress.ifBlank { "未连接" }}
- Solana 地址: ${userContext.solanaAddress.ifBlank { "未连接" }}
- 运行中的策略: ${userContext.activeStrategies.size} 个
${userContext.activeStrategies.joinToString("\n") { "  · ${it.name} (${it.chain}) — ${it.status} — PnL: ${it.pnl}" }}
- 今日已用交易额度: ${"$%.2f".format(userContext.dailyUsage)}

你可以帮用户：
- 查看钱包地址和余额
- 创建和管理交易策略（新币狙击、定投、网格）
- 查看实时链上信号（聪明钱、KOL、巨鲸动态）
- 执行链上交易（OKX DEX 聚合最优路由）

重要规则：
1. 用户问"我的地址"、"钱包地址"、"充值地址"时，直接给出完整的 EVM 和 Solana 地址
2. 交易操作使用对应工具（query_signals / execute_trade / create_strategy）
3. 任何交易前必须通过风控检查
4. 用中文回复
""".trimIndent()

    /**
     * Send conversation to LLM with tools available.
     * Returns either a text response or function calls.
     */
    suspend fun processMessage(
        systemPrompt: String,
        conversationHistory: List<LlmMessage>,
        userMessage: String
    ): LlmResponse {
        val messages = listOf(
            LlmMessage("system", systemPrompt)
        ) + conversationHistory + listOf(
            LlmMessage("user", userMessage)
        )

        return llmClient.chat(
            messages = messages,
            tools = AgentTools.all,
            model = model
        )
    }

    /**
     * Parse a JSON tool argument into a Map for routing.
     */
    fun parseToolArgs(toolCall: LlmToolCall): Map<String, String> {
        return toolCall.arguments.entries.associate { (key, value) ->
            key to value.jsonPrimitive.content
        }
    }
}

/**
 * Context injected into the system prompt for each user.
 */
data class UserContext(
    val userId: String,
    val evmAddress: String = "",
    val solanaAddress: String = "",
    val activeStrategies: List<ActiveStrategySummary>,
    val dailyUsage: Double
)

data class ActiveStrategySummary(
    val name: String,
    val chain: String,
    val status: String,  // RUNNING / PAUSED
    val pnl: String
)
