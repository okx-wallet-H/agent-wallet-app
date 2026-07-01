package com.agentwallet.agent

import com.agentwallet.models.*
import com.agentwallet.models.StrategyRepository
import com.agentwallet.models.UserRepository
import com.agentwallet.plugins.RiskUsageTracker
import com.agentwallet.risk.RiskEngine
import com.agentwallet.services.*
import kotlinx.serialization.json.*

/**
 * Main orchestrator — receives user messages, calls LLM for intent understanding,
 * and delegates function calls to the appropriate sub-agent.
 */
class OrchestratorAgent(
    private val llmClient: LlmClient,
    private val chatAgent: ChatAgent,
    private val analysisAgent: AnalysisAgent,
    private val executionAgent: ExecutionAgent
) {
    private val riskEngine = RiskEngine()

    /**
     * Process a user message through the LLM pipeline.
     */
    suspend fun process(userId: String, message: String, conversationId: String): List<ChatMessage> {
        // Build context
        val strategies = StrategyRepository.findByUserId(userId)
        val dailyUsage = RiskUsageTracker.get(userId)

        val context = UserContext(
            userId = userId,
            activeStrategies = strategies.filter { it.status == StrategyStatus.RUNNING }.map {
                ActiveStrategySummary(it.name, it.chain, it.status.name, "$%.2f".format(it.pnl))
            },
            dailyUsage = dailyUsage
        )

        val systemPrompt = chatAgent.buildSystemPrompt(context)

        // Call LLM
        val response = chatAgent.processMessage(
            systemPrompt = systemPrompt,
            conversationHistory = emptyList(), // TODO: load from ConversationRepository
            userMessage = message
        )

        // Route based on LLM response
        return when {
            // Text response → display to user
            response.content != null && response.toolCalls == null -> {
                listOf(ChatMessage("agent", response.content!!, "text"))
            }

            // Function call → execute and return result
            response.toolCalls != null -> {
                handleToolCalls(userId, response.toolCalls)
            }

            else -> listOf(ChatMessage("agent", "抱歉，我没能理解。", "text"))
        }
    }

    private suspend fun handleToolCalls(userId: String, toolCalls: List<LlmToolCall>): List<ChatMessage> {
        val results = mutableListOf<ChatMessage>()

        for (call in toolCalls) {
            when (call.name) {
                "create_strategy" -> {
                    results.addAll(handleCreateStrategy(userId, call))
                }

                "execute_trade" -> {
                    results.addAll(handleExecuteTrade(userId, call))
                }

                "query_signals" -> {
                    results.addAll(handleQuerySignals(call))
                }

                else -> {
                    results.add(ChatMessage("agent", "未知操作: ${call.name}", "text"))
                }
            }
        }

        return results
    }

    private suspend fun handleCreateStrategy(userId: String, call: LlmToolCall): List<ChatMessage> {
        val args = chatAgent.parseToolArgs(call)

        val name = args["name"] ?: "未命名策略"
        val chain = args["chain"] ?: "Ethereum"
        val maxPerTx = args["maxPerTx"]?.toDoubleOrNull() ?: 100.0
        val dailyLimit = args["dailyLimit"]?.toDoubleOrNull() ?: 500.0
        val stopLoss = args["stopLoss"]?.toDoubleOrNull()
        val takeProfit = args["takeProfit"]?.toDoubleOrNull()

        val strategy = StrategyRepository.create(
            Strategy(
                userId = userId,
                name = name,
                type = StrategyType.valueOf(args["type"] ?: "SNIPER"),
                chain = chain,
                params = StrategyParams(
                    maxPerTx = maxPerTx,
                    dailyLimit = dailyLimit,
                    stopLoss = stopLoss,
                    takeProfit = takeProfit
                )
            )
        )

        return listOf(
            ChatMessage("agent", "策略已创建并启动！", "text"),
            ChatMessage("agent", "$name · $chain", "strategy_card",
                buildJsonObject {
                    put("name", JsonPrimitive(strategy.name))
                    put("chain", JsonPrimitive(strategy.chain))
                    put("status", JsonPrimitive("RUNNING"))
                    put("runtime", JsonPrimitive("刚刚启动"))
                    put("trades", JsonPrimitive(0))
                    put("pnl", JsonPrimitive("\$0.00"))
                }
            ),
            ChatMessage("agent", "我会 7×24 监控 $chain 链上的交易机会，条件匹配时自动执行。你可以随时说「暂停策略」来暂停。", "text")
        )
    }

    private suspend fun handleExecuteTrade(userId: String, call: LlmToolCall): List<ChatMessage> {
        val args = chatAgent.parseToolArgs(call)

        val fromToken = args["fromToken"] ?: "ETH"
        val toToken = args["toToken"] ?: return listOf(ChatMessage("agent", "请指定要买入的代币", "text"))
        val amount = args["amount"]?.toDoubleOrNull() ?: return listOf(ChatMessage("agent", "请指定交易金额", "text"))
        val chain = args["chain"] ?: "1"
        val slippage = args["slippage"]?.toDoubleOrNull() ?: 0.03

        val tradeIntent = TradeIntent(
            action = TradeAction.BUY,
            fromToken = fromToken,
            toToken = toToken,
            amount = amount,
            chain = chain,
            slippage = slippage
        )

        // Risk check
        val risk = riskEngine.check(userId, tradeIntent)
        if (!risk.approved) {
            return listOf(
                ChatMessage("agent", "⚠️ 风控检查未通过：${risk.reason}", "text"),
                ChatMessage("agent", risk.checks.joinToString("\n") {
                    "${if (it.passed) "✅" else "❌"} ${it.label}"
                }, "text")
            )
        }

        // Show confirmation card
        return listOf(
            ChatMessage("agent", "📊 交易确认", "trade_confirmation",
                buildJsonObject {
                    put("token", JsonPrimitive(toToken))
                    put("chain", JsonPrimitive(chain))
                    put("amount", JsonPrimitive(amount.toString()))
                    put("fromToken", JsonPrimitive(fromToken))
                    put("slippage", JsonPrimitive(slippage.toString()))
                }
            )
        )
    }

    private suspend fun handleQuerySignals(call: LlmToolCall): List<ChatMessage> {
        val args = chatAgent.parseToolArgs(call)
        val chainId = args["chainId"] ?: "501"

        val signals = analysisAgent.getRecentSignals(chainId)
        if (signals.isEmpty()) {
            return listOf(ChatMessage("agent", "最近 $chainId 链上没有发现高质量信号。", "text"))
        }

        val messages = mutableListOf<ChatMessage>(
            ChatMessage("agent", "最近 ${signals.size} 个值得关注的信号：", "text")
        )

        signals.forEach { signal ->
            messages.add(
                ChatMessage("agent", signal.signal.tokenSymbol, "signal_card",
                    buildJsonObject {
                        put("token", JsonPrimitive(signal.signal.tokenSymbol))
                        put("chain", JsonPrimitive(signal.signal.chainId))
                        put("amountUsd", JsonPrimitive(signal.signal.amountUsd))
                        put("walletCount", JsonPrimitive(signal.signal.triggerWallets.size))
                        put("confidence", JsonPrimitive(signal.confidence))
                    }
                )
            )
        }

        return messages
    }
}
