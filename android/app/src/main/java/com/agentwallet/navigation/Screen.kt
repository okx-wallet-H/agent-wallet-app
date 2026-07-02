package com.agentwallet.navigation

sealed class Screen(val route: String, val label: String, val emoji: String) {
    data object Chat      : Screen("chat",      "对话", "💬")
    data object Portfolio : Screen("portfolio", "资产", "💰")
    data object Strategy  : Screen("strategy",  "策略", "📋")
    data object Signal    : Screen("signal",    "信号", "📡")
    companion object { val tabs = listOf(Chat, Portfolio, Strategy, Signal) }
}
