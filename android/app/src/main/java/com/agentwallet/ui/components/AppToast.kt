package com.agentwallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.theme.AgentWalletTypography
import com.agentwallet.ui.theme.BackgroundSecondary
import com.agentwallet.ui.theme.Spacing
import com.agentwallet.ui.theme.TextPrimary
import com.agentwallet.ui.theme.TextSecondary
import kotlinx.coroutines.delay

enum class ToastType { SUCCESS, WARNING, ERROR }

@Composable
fun AppToast(
    message: String,
    type: ToastType,
    onDismiss: () -> Unit,
    autoDismissMs: Long = 3000L,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val visible = remember { mutableStateOf(true) }
    val icon = when (type) {
        ToastType.SUCCESS -> "✅"
        ToastType.WARNING -> "⚠️"
        ToastType.ERROR -> "❌"
    }

    if (type == ToastType.SUCCESS) {
        LaunchedEffect(Unit) { delay(autoDismissMs); visible.value = false }
    }
    LaunchedEffect(visible.value) {
        if (!visible.value) { delay(300); onDismiss() }
    }

    AnimatedVisibility(
        visible = visible.value,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Row(
            modifier = modifier.fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs)
                .clip(RoundedCornerShape(12.dp))
                .background(BackgroundSecondary)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, style = AgentWalletTypography.bodyLarge)
            Spacer(Modifier.width(Spacing.sm))
            Text(message, style = AgentWalletTypography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    actionLabel, style = AgentWalletTypography.labelMedium,
                    color = com.agentwallet.ui.theme.Accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onAction() }
                        .padding(horizontal = Spacing.sm, vertical = 2.dp)
                )
            }
            if (type != ToastType.SUCCESS) {
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "✕", style = AgentWalletTypography.bodySmall, color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { visible.value = false }
                        .padding(Spacing.xs)
                )
            }
        }
    }
}
