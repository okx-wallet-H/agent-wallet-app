package com.agentwallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentwallet.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: ChatViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    // Auto-navigate when logged in
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Accent, AccentGlow)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🧿", style = AgentWalletTypography.displayLarge)
        }

        Spacer(Modifier.height(24.dp))
        Text("Agent Wallet", style = AgentWalletTypography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "AI 驱动的加密交易助手",
            style = AgentWalletTypography.bodyLarge,
            color = TextSecondary
        )

        Spacer(Modifier.height(40.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text("邮箱") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = TextTertiary,
                focusedLabelColor = Accent,
                cursorColor = Accent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(Modifier.height(16.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = TextTertiary,
                focusedLabelColor = Accent,
                cursorColor = Accent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        // Error
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = Loss, style = AgentWalletTypography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        // Submit
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    error = "请填写邮箱和密码"
                    return@Button
                }
                loading = true
                error = null
                // Launch coroutine
                kotlinx.coroutines.MainScope().launch {
                    val err = if (isRegistering) {
                        viewModel.register(email, password)
                    } else {
                        viewModel.login(email, password)
                    }
                    loading = false
                    if (err != null) error = err
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            enabled = !loading
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = TextOnAccent,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    if (isRegistering) "注册" else "登录",
                    style = AgentWalletTypography.bodyMedium,
                    color = TextOnAccent
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = { isRegistering = !isRegistering; error = null }) {
            Text(
                if (isRegistering) "已有账号？登录" else "没有账号？注册",
                color = TextSecondary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewLogin() {
    com.agentwallet.AgentWalletApp.instance?.let {
        LoginScreen(viewModel = ChatViewModel(it), onLoginSuccess = {})
    } ?: Text("Preview requires Application context")
}
