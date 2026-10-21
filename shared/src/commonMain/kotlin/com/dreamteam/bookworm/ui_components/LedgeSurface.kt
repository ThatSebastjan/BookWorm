package com.dreamteam.bookworm.ui_components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The chunky "ledge" look used across the canvas: a face sitting on a solid
 * shadow strip below it. When clickable, the face sinks onto the ledge while pressed.
 */
@Composable
fun LedgeSurface(
    color: Color,
    ledgeColor: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    role: Role = Role.Button,
    borderColor: Color? = null,
    borderWidth: Dp = 2.dp,
    ledge: Dp = 5.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed && enabled && ledge > 0.dp) ledge else 0.dp)

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Box(modifier.then(clickModifier), propagateMinConstraints = true) {
        if (ledge > 0.dp) {
            Box(Modifier.matchParentSize().padding(top = ledge).background(ledgeColor, shape))
        }
        Box(
            Modifier
                .padding(bottom = ledge)
                .offset(y = sink)
                .background(color, shape)
                .then(if (borderColor != null) Modifier.border(borderWidth, borderColor, shape) else Modifier)
                .padding(contentPadding),
            contentAlignment = contentAlignment,
        ) {
            content()
        }
    }
}
