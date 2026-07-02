package com.agentwallet.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.agentwallet.ui.screens.*
import com.agentwallet.ui.theme.*

@Composable
fun AgentWalletNavHost(chatViewModel: ChatViewModel = viewModel()) {
    val uiState by chatViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    if (!uiState.isLoggedIn) {
        LoginScreen(viewModel = chatViewModel, onLoginSuccess = { navController.navigate("main") { popUpTo(0) } })
    } else {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = BackgroundSecondary) {
                    Screen.tabs.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Text(screen.emoji, style = AgentWalletTypography.titleMedium) },
                            label = { Text(screen.label, style = AgentWalletTypography.labelMedium, color = if (selected) Accent else TextTertiary) },
                            selected = selected,
                            onClick = { navController.navigate(screen.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Accent.copy(alpha = 0.12f), selectedIconColor = Accent, unselectedIconColor = TextTertiary)
                        )
                    }
                }
            }
        ) { ip ->
            val m = Modifier.fillMaxSize().padding(ip)
            NavHost(navController, Screen.Chat.route, m) {
                composable(Screen.Chat.route) { ChatScreen(viewModel = chatViewModel) }
                composable(Screen.Portfolio.route) { PortfolioScreen() }
                composable(Screen.Strategy.route) { StrategyScreen() }
                composable(Screen.Signal.route) { SignalScreen() }
            }
        }
    }
}
