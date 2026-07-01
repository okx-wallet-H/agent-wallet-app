package com.agentwallet.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4dp-based spacing grid.
 *
 *    xs    4dp    tight / inline
 *    sm    8dp    related content
 *    md   12dp    card inner padding
 *    lg   16dp    card gap / paragraph
 *    xl   24dp    major section
 *    xxl  32dp    page-level whitespace
 */
object Spacing {
    val xs  : Dp = 4.dp
    val sm  : Dp = 8.dp
    val md  : Dp = 12.dp
    val lg  : Dp = 16.dp
    val xl  : Dp = 24.dp
    val xxl : Dp = 32.dp

    val screenHorizontal : Dp = 16.dp
    val bubbleVertical   : Dp = 10.dp
    val bubbleHorizontal : Dp = 12.dp
}
