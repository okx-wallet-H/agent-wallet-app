package com.agentwallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.agentwallet.navigation.AgentWalletNavHost
import com.agentwallet.ui.theme.AgentWalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AgentWalletTheme {
                AgentWalletNavHost()
            }
        }
    }
}
