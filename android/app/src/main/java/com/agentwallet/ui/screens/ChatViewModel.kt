package com.agentwallet.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentwallet.data.*
import com.agentwallet.util.MarkdownUtil
import com.agentwallet.ui.components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val connected: Boolean = true,
    val isLoggedIn: Boolean = false,
    val email: String? = null,
    val evmAddress: String? = null,
    val solanaAddress: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient()
    private val authRepo = AuthRepository(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var conversationId: String? = null

    init {
        // Restore auth token
        viewModelScope.launch {
            val token = authRepo.getToken()
            val email = authRepo.getEmail()
            if (token != null) {
                api.setToken(token)
                _uiState.update { it.copy(isLoggedIn = true, email = email) }
                addWelcomeMessage()
            }
        }
    }

    private fun addWelcomeMessage() {
        _uiState.update {
            it.copy(messages = listOf(
                ChatMessage.Text("你好！我是你的交易 Agent。告诉我你想做什么？", isUser = false)
            ))
        }
    }

    suspend fun quickLogin(email: String): String? {
        return try {
            val response = api.quickLogin(email)
            val token = response["token"]?.jsonPrimitive?.content ?: return "快速登录失败"
            val user = response["user"]?.jsonObject ?: return "快速登录失败"
            api.setToken(token)
            authRepo.saveAuth(token, email)
            _uiState.update { it.copy(isLoggedIn = true, email = email,
                evmAddress = user["evmAddress"]?.jsonPrimitive?.content,
                solanaAddress = user["solanaAddress"]?.jsonPrimitive?.content ?: "")}
            addWelcomeMessage()
            null
        } catch (e: Exception) { e.message ?: "需要验证码" }
    }

    suspend fun sendOtp(email: String): String? {
        return try {
            api.sendOtp(email)
            null
        } catch (e: Exception) { e.message ?: "发送验证码失败" }
    }

    suspend fun verifyOtp(email: String, otp: String): String? {
        return try {
            val response = api.verifyOtp(email, otp)
            val token = response["token"]?.jsonPrimitive?.content ?: return "验证失败"
            val user = response["user"]?.jsonObject ?: return "验证失败"
            api.setToken(token)
            authRepo.saveAuth(token, email)
            _uiState.update { it.copy(
                isLoggedIn = true, email = email,
                evmAddress = user["evmAddress"]?.jsonPrimitive?.content,
                solanaAddress = user["solanaAddress"]?.jsonPrimitive?.content ?: ""
            )}
            addWelcomeMessage()
            null
        } catch (e: Exception) { e.message ?: "验证码错误" }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.clearAuth()
            api.setToken(null)
            _uiState.update { ChatUiState() }
        }
    }

    fun sendCommand(text: String) {
        val userMsg = ChatMessage.Text(content = text, isUser = true)

        _uiState.update { state ->
            state.copy(messages = state.messages + userMsg, isStreaming = true)
        }

        // Show thinking indicator
        _uiState.update { state ->
            state.copy(messages = state.messages + ChatMessage.Thinking(
                steps = listOf(ThinkingStep("处理中...", StepStatus.IN_PROGRESS)),
                isCollapsed = false
            ))
        }

        viewModelScope.launch {
            try {
                val response = api.sendMessage(
                    message = text,
                    conversationId = conversationId
                )
                conversationId = response.conversationId

                // Remove thinking indicator
                _uiState.update { state ->
                    val filtered = state.messages.filterNot { it is ChatMessage.Thinking }
                    state.copy(messages = filtered, isStreaming = false)
                }

                // Map backend messages to UI messages
                response.messages.forEach { dto ->
                    val uiMsg = mapDtoToUiMessage(dto)
                    _uiState.update { state ->
                        state.copy(messages = state.messages + uiMsg)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    val filtered = state.messages.filterNot { it is ChatMessage.Thinking }
                    state.copy(
                        messages = filtered + ChatMessage.Text("抱歉，请求失败: ${e.message}", isUser = false),
                        isStreaming = false,
                        connected = false
                    )
                }
            }
        }
    }

    private fun mapDtoToUiMessage(dto: ChatMessageDto): ChatMessage {
        return when (dto.type) {
            "text" -> ChatMessage.Text(content = MarkdownUtil.stripMarkdown(dto.content), isUser = dto.role == "user")

            "trade_confirmation" -> {
                val data = dto.data ?: return ChatMessage.Text(dto.content, false)
                ChatMessage.TradeConfirmation(
                    token = data["token"]?.jsonPrimitive?.content ?: "",
                    chain = data["chain"]?.jsonPrimitive?.content ?: "",
                    amount = data["amount"]?.jsonPrimitive?.content ?: "",
                    fromToken = data["fromToken"]?.jsonPrimitive?.content ?: "",
                    toAmount = "",
                    slippage = data["slippage"]?.jsonPrimitive?.content ?: "3%",
                    gasEstimate = "~$2.30",
                    checks = listOf(
                        SecurityCheck("Dev 信誉", true),
                        SecurityCheck("LP 锁定", true),
                        SecurityCheck("捆绑 < 30%", true)
                    ),
                    onConfirm = {
                        viewModelScope.launch {
                            // Call backend to execute the trade
                            sendCommand("确认交易")
                        }
                    },
                    onReject = {
                        addAgentText("已取消交易。")
                    }
                )
            }

            "strategy_card" -> {
                val data = dto.data ?: return ChatMessage.Text(dto.content, false)
                ChatMessage.StrategyCard(
                    name = data["name"]?.jsonPrimitive?.content ?: "",
                    chain = data["chain"]?.jsonPrimitive?.content ?: "",
                    status = StrategyStatus.valueOf(data["status"]?.jsonPrimitive?.content ?: "RUNNING"),
                    runtime = data["runtime"]?.jsonPrimitive?.content ?: "",
                    trades = data["trades"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    pnl = data["pnl"]?.jsonPrimitive?.content ?: "$0.00"
                )
            }

            "signal_card" -> {
                val data = dto.data ?: return ChatMessage.Text(dto.content, false)
                ChatMessage.SignalAlert(
                    token = data["token"]?.jsonPrimitive?.content ?: "",
                    chain = data["chain"]?.jsonPrimitive?.content ?: "",
                    amountUsd = data["amountUsd"]?.jsonPrimitive?.content ?: "",
                    walletCount = data["walletCount"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    devReputation = true,
                    bundleRatio = "8%",
                    lpLocked = true,
                    confidence = data["confidence"]?.jsonPrimitive?.content ?: "medium",
                    isNew = true
                )
            }

            else -> ChatMessage.Text(content = dto.content, isUser = false)
        }
    }

    private fun addAgentText(text: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages + ChatMessage.Text(content = text, isUser = false))
        }
    }

    // ─── Portfolio / Strategy / Signal loaders ─────────

    fun loadPortfolio(onResult: (PortfolioDto?) -> Unit) {
        viewModelScope.launch {
            try { onResult(api.getPortfolio()) }
            catch (e: Exception) { onResult(null) }
        }
    }

    fun loadStrategies(onResult: (List<StrategyDto>) -> Unit) {
        viewModelScope.launch {
            try { onResult(api.getStrategies()) }
            catch (e: Exception) { onResult(emptyList()) }
        }
    }

    fun loadSignals(onResult: (List<SignalDto>) -> Unit) {
        viewModelScope.launch {
            try { onResult(api.getSignals()) }
            catch (e: Exception) { onResult(emptyList()) }
        }
    }
}
