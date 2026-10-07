package com.aipose.camera.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Shared dark action surface; secondary actions remain transparent TextButtons. */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) .97f else 1f, tween(120), label = "button press")
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 50.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        interactionSource = interaction,
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.dp, CameraDesign.Border),
        colors = ButtonDefaults.buttonColors(
            containerColor = BgDark,
            contentColor = TextPrimary,
            disabledContainerColor = BgDark.copy(alpha = .5f),
            disabledContentColor = TextSecondary.copy(alpha = .5f),
        ),
        content = content,
    )
}
