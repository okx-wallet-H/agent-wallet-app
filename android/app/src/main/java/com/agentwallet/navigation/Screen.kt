package com.agentwallet.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * All navigation destinations in the app.
 */
sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Chat      : Screen("chat",      "对话", Icons.Filled.Chat)
    data object Portfolio : Screen("portfolio", "资产", Icons.Filled.Wallet)
    data object Strategy  : Screen("strategy",  "策略", Icons.Filled.GridView)
    data object Signal    : Screen("signal",    "信号", Icons.Filled.Notifications)

    companion object {
        val tabs = listOf(Chat, Portfolio, Strategy, Signal)
    }
}
