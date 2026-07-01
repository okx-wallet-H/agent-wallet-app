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
你是一个加密货币交易 AI Agent。你可以帮用户：
- 创建和管理交易策略（新币狙击、定投、网格）
- 查看实时链上信号（聪明钱、KOL、巨鲸动态）
- 执行链上交易（通过 OKX DEX 最优路由）
- 查询资产组合和盈亏

当前用户信息：
- ID: ${userContext.userId}
- 运行中的策略: ${userContext.activeStrategies.size} 个
${userContext.activeStrategies.joinToString("\n") { "  · ${it.name} (${it.chain}) — ${it.status} — PnL: ${it.pnl}" }}
- 今日已用交易额度: ${"$%.2f".format(userContext.dailyUsage)}

重要安全规则：
1. 任何交易操作前必须通过风控检查（单笔限额、日限额、代币白名单）
2. 如果交易超出限额，必须明确告知用户并拒绝执行
3. 永远不要建议用户取消风控限制
4. 代币地址可以用简写，但交易确认时必须展示完整信息
5. 用中文回复
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
    val activeStrategies: List<ActiveStrategySummary>,
    val dailyUsage: Double
)

data class ActiveStrategySummary(
    val name: String,
    val chain: String,
    val status: String,  // RUNNING / PAUSED
    val pnl: String
)
