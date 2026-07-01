package com.agentwallet.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agentwallet.ui.screens.ChatScreen
import com.agentwallet.ui.screens.PortfolioScreen
import com.agentwallet.ui.screens.SignalScreen
import com.agentwallet.ui.screens.StrategyScreen
import com.agentwallet.ui.theme.Accent
import com.agentwallet.ui.theme.BackgroundSecondary
import com.agentwallet.ui.theme.TextOnAccent
import com.agentwallet.ui.theme.TextSecondary
import com.agentwallet.ui.theme.TextTertiary

/**
 * Root composable — Scaffold with bottom nav + NavHost for 4 tabs.
 */
@Composable
fun AgentWalletNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = BackgroundSecondary,
                tonalElevation = androidx.compose.ui.unit.Dp(0f)
            ) {
                Screen.tabs.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = {
                            // Use text emoji icons since we don't have resource bindings yet
                            Text(
                                text = when (screen) {
                                    Screen.Chat      -> "💬"
                                    Screen.Portfolio -> "💰"
                                    Screen.Strategy  -> "📋"
                                    Screen.Signal    -> "📡"
                                },
                                style = com.agentwallet.ui.theme.AgentWalletTypography.titleMedium
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                style = com.agentwallet.ui.theme.AgentWalletTypography.labelMedium,
                                color = if (selected) Accent else TextTertiary
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Accent.copy(alpha = 0.12f),
                            selectedIconColor = Accent,
                            unselectedIconColor = TextTertiary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Chat.route)      { ChatScreen() }
            composable(Screen.Portfolio.route) { PortfolioScreen() }
            composable(Screen.Strategy.route)  { StrategyScreen() }
            composable(Screen.Signal.route)    { SignalScreen() }
        }
    }
}
