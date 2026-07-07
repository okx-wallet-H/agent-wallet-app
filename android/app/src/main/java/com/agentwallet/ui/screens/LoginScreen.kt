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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentwallet.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: ChatViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var showOtp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.isLoggedIn) { if (uiState.isLoggedIn) onLoginSuccess() }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(72.dp).background(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Accent, AccentGlow)),
            shape = RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center
        ) { Text("🧿", style = AgentWalletTypography.displayLarge) }

        Spacer(Modifier.height(24.dp))
        Text("Agent Wallet", style = AgentWalletTypography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("OKX TEE 安全钱包", style = AgentWalletTypography.bodyLarge, color = TextSecondary)
        Spacer(Modifier.height(40.dp))

        if (showOtp) {
            Text("验证码已发送到", style = AgentWalletTypography.bodyLarge, color = TextSecondary)
            Text(email, style = AgentWalletTypography.titleMedium, color = Accent)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = otp, onValueChange = { otp = it; error = null },
                label = { Text("6位验证码") }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextTertiary),
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (error != null) { Spacer(Modifier.height(8.dp)); Text(error!!, color = Loss, style = AgentWalletTypography.bodySmall) }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                if (otp.length != 6) { error = "请输入6位验证码"; return@Button }
                loading = true; error = null
                scope.launch {
                    val err = viewModel.verifyOtp(email, otp)
                    loading = false
                    if (err != null) error = err
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent), enabled = !loading
            ) { Text("验证", style = AgentWalletTypography.bodyMedium, color = TextOnAccent) }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { showOtp = false; error = null }) { Text("返回", color = TextSecondary) }

        } else {
            OutlinedTextField(
                value = email, onValueChange = { email = it; error = null },
                label = { Text("邮箱地址") }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextTertiary),
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            if (error != null) { Spacer(Modifier.height(8.dp)); Text(error!!, color = Loss, style = AgentWalletTypography.bodySmall) }
            Spacer(Modifier.height(24.dp))

            Button(onClick = {
                if (email.isBlank() || !email.contains("@")) { error = "请输入有效的邮箱地址"; return@Button }
                loading = true; error = null
                scope.launch {
                    val err = viewModel.sendOtp(email)
                    loading = false
                    if (err != null) error = err
                    else showOtp = true
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent), enabled = !loading
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = TextOnAccent, strokeWidth = 2.dp)
                else Text("获取验证码", style = AgentWalletTypography.bodyMedium, color = TextOnAccent)
            }
            Spacer(Modifier.height(8.dp))
            Text("首次使用自动创建 OKX TEE 钱包", style = AgentWalletTypography.labelMedium, color = TextTertiary)
        }
    }
}
