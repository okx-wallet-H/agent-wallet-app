package com.agentwallet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentwallet.ui.components.AgentThinkingProcess
import com.agentwallet.ui.components.AgentThinkingIndicator
import com.agentwallet.ui.components.ChatInputBar
import com.agentwallet.ui.components.ChatMessage
import com.agentwallet.ui.components.EmptyState
import com.agentwallet.ui.components.MessageBubble
import com.agentwallet.ui.components.AgentAvatarState
import com.agentwallet.ui.components.SignalCard
import com.agentwallet.ui.components.StrategyPanel
import com.agentwallet.ui.components.TradeConfirmationCard
import com.agentwallet.ui.components.TradeResultCard
import com.agentwallet.ui.theme.Accent
import com.agentwallet.ui.theme.AgentWalletTypography
import com.agentwallet.ui.theme.BackgroundPrimary
import com.agentwallet.ui.theme.BackgroundSecondary
import com.agentwallet.ui.theme.Profit
import com.agentwallet.ui.theme.Spacing
import com.agentwallet.ui.theme.TextPrimary
import com.agentwallet.ui.theme.TextSecondary
import com.agentwallet.ui.theme.TextTertiary

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showScrollButton by remember { mutableStateOf(false) }

    // Auto-scroll when new messages arrive (unless user scrolled up)
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ─── Top bar ────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundPrimary)
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (uiState.connected) Profit else com.agentwallet.ui.theme.Loss)
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "Agent Wallet",
                style = AgentWalletTypography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (uiState.connected) "已连接" else "重连中...",
                style = AgentWalletTypography.labelMedium,
                color = TextSecondary
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(com.agentwallet.ui.theme.DividerSubtle)
        )

        // ─── Messages or empty state ────────
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.messages.isEmpty()) {
                EmptyState(onSuggestionTap = { viewModel.sendCommand(it) })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm)
                ) {
                    item { Spacer(Modifier.height(Spacing.sm)) }

                    itemsIndexed(uiState.messages) { _, message ->
                        MessageRenderer(message = message)
                    }

                    // Streaming indicator
                    if (uiState.isStreaming) {
                        item {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs)
                            ) {
                                AgentThinkingIndicator()
                            }
                        }
                    }

                    item { Spacer(Modifier.height(Spacing.xl)) }
                }
            }
        }

        // ─── Input bar ──────────────────────
        ChatInputBar(
            onSend = { viewModel.sendCommand(it) },
            enabled = !uiState.isStreaming
        )
    }
}

/**
 * Routes each ChatMessage subtype to its renderer.
 */
@Composable
fun MessageRenderer(message: ChatMessage) {
    Box(modifier = Modifier.fillMaxWidth()) {
        when (message) {
            is ChatMessage.Text ->
                MessageBubble(message = message)

            is ChatMessage.TradeConfirmation ->
                TradeConfirmationCard(
                    message = message,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)
                )

            is ChatMessage.TradeResult ->
                TradeResultCard(
                    message = message,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)
                )

            is ChatMessage.SignalAlert ->
                SignalCard(
                    message = message,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)
                )

            is ChatMessage.StrategyCard ->
                StrategyPanel(
                    message = message,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)
                )

            is ChatMessage.Thinking ->
                AgentThinkingProcess(
                    steps = message.steps,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)
                )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewChat() {
    ChatScreen()
}
