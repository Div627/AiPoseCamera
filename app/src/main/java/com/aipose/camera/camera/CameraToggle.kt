package com.aipose.camera.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aipose.camera.ui.theme.BgDark
import com.aipose.camera.ui.theme.TextPrimary

@Composable
fun CameraToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,Modifier.weight(1f).padding(end=12.dp),style=MaterialTheme.typography.bodyLarge)
        Switch(checked=checked,onCheckedChange=onChange,modifier=Modifier.semantics{contentDescription=label},
            colors=SwitchDefaults.colors(checkedTrackColor=TextPrimary,checkedThumbColor=BgDark))
    }
}
