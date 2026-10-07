package com.aipose.camera.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aipose.camera.ui.theme.BgDark
import com.aipose.camera.ui.theme.SurfaceDark
import com.aipose.camera.ui.theme.TextPrimary
import com.aipose.camera.ui.theme.TextSecondary

@Composable
fun AssistantComposer(
    value: String,
    onValueChange: (String) -> Unit,
    canSend: Boolean,
    replying: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onVoice: () -> Unit,
    voiceEnabled: Boolean,
) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp).padding(vertical = 11.dp, horizontal = 2.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                decorationBox = { field ->
                    Box { if (value.isEmpty()) Text("说说想拍什么", color = MaterialTheme.colorScheme.onSurfaceVariant); field() }
                },
            )
            IconButton(onClick=onVoice,enabled=voiceEnabled,modifier=Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Mic,"语音输入",tint=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = if (replying) onStop else onSend,
                enabled = replying || canSend,
                modifier = Modifier.size(48.dp).background(BgDark, CircleShape),
            ) {
                Icon(if (replying) Icons.Default.Stop else Icons.Default.ArrowUpward,
                    if (replying) "停止回复" else "发送", tint = if (replying || canSend) TextPrimary else TextSecondary.copy(alpha = .4f))
            }
        }
    }
}
