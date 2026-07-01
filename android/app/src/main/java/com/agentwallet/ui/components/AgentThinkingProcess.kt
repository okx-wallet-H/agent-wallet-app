package com.agentwallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agentwallet.ui.theme.Accent
import com.agentwallet.ui.theme.AccentGlow
import com.agentwallet.ui.theme.AgentWalletTypography
import com.agentwallet.ui.theme.BackgroundGlass
import com.agentwallet.ui.theme.BorderSubtle
import com.agentwallet.ui.theme.Profit
import com.agentwallet.ui.theme.Shapes
import com.agentwallet.ui.theme.Spacing
import com.agentwallet.ui.theme.TextPrimary
import com.agentwallet.ui.theme.TextSecondary
import com.agentwallet.ui.theme.TextTertiary

/**
 * Collapsible agent thinking process display.
 * Shows step-by-step reasoning with status indicators.
 */
@Composable
fun AgentThinkingProcess(
    steps: List<ThinkingStep>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val inProgress = steps.any { it.status == StepStatus.IN_PROGRESS }
    val completedCount = steps.count { it.status == StepStatus.COMPLETED }
    val totalCount = steps.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .animateContentSize()
    ) {
        // Collapsed header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.card)
                .background(BackgroundGlass)
                .clickable { expanded = !expanded }
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inProgress) {
                // Pulsing dot
                val infiniteTransition = rememberInfiniteTransition(label = "analyze_pulse")
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentGlow.copy(alpha = pulse))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Profit)
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = if (inProgress) "⚡ 分析中..." else "✅ 分析完成",
                style = AgentWalletTypography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (expanded) "收起 ▾" else "展开 ▸",
                style = AgentWalletTypography.labelMedium,
                color = TextTertiary
            )
        }

        // Expanded steps
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundGlass)
                    .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderSubtle)
                )
                Spacer(Modifier.height(Spacing.sm))

                steps.forEachIndexed { index, step ->
                    ThinkingStepRow(step, index + 1, isLast = index == steps.lastIndex)
                    if (!(index == steps.lastIndex)) {
                        Spacer(Modifier.height(Spacing.xs))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingStepRow(step: ThinkingStep, number: Int, isLast: Boolean) {
    val (statusColor, statusIcon) = when (step.status) {
        StepStatus.COMPLETED   -> Profit to "✓"
        StepStatus.IN_PROGRESS -> Accent to "●"
        StepStatus.PENDING     -> TextTertiary to "○"
    }

    val textStyle = when (step.status) {
        StepStatus.COMPLETED   -> AgentWalletTypography.bodySmall
        StepStatus.IN_PROGRESS -> AgentWalletTypography.bodyMedium
        StepStatus.PENDING     -> AgentWalletTypography.bodySmall
    }

    val textColor = when (step.status) {
        StepStatus.COMPLETED   -> TextSecondary
        StepStatus.IN_PROGRESS -> TextPrimary
        StepStatus.PENDING     -> TextTertiary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status icon
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (step.status == StepStatus.IN_PROGRESS) {
                val infiniteTransition = rememberInfiniteTransition(label = "step_pulse_${number}")
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                    label = "step_pulse"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = pulse))
                )
            } else {
                Text(statusIcon, style = AgentWalletTypography.labelMedium, color = statusColor)
            }
        }

        Spacer(Modifier.width(Spacing.sm))

        Text(
            text = "${number}. ${step.label}",
            style = textStyle,
            color = textColor
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090D)
@Composable
private fun PreviewThinking() {
    AgentThinkingProcess(
        steps = listOf(
            ThinkingStep("Token 安全分析", StepStatus.COMPLETED),
            ThinkingStep("开发者画像", StepStatus.COMPLETED),
            ThinkingStep("热度确认", StepStatus.COMPLETED),
            ThinkingStep("风控检查", StepStatus.IN_PROGRESS)
        )
    )
}
