package com.aipose.camera.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun CameraToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val stretch = remember { Animatable(1f) }
    var previous by remember { androidx.compose.runtime.mutableStateOf(checked) }
    LaunchedEffect(checked) {
        if(previous != checked) {
            previous = checked
            stretch.animateTo(1.12f, tween(80))
            stretch.animateTo(1f, tween(140))
        }
    }
    val colors = MaterialTheme.colorScheme
    val track by animateColorAsState(if (checked) colors.primary else colors.outline, tween(180), label="switch track")
    val position by animateDpAsState(if (checked) 22.dp else 2.dp, tween(180), label="switch thumb")
    Row(Modifier.fillMaxWidth().heightIn(min=54.dp).toggleable(checked, role=Role.Switch, onValueChange=onChange),
        verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f).padding(end=16.dp), style=MaterialTheme.typography.bodyLarge)
        Box(Modifier.size(50.dp,30.dp).background(track,CircleShape), contentAlignment=Alignment.CenterStart) {
            Box(Modifier.offset(x=position).size(26.dp).graphicsLayer {scaleX=stretch.value;scaleY=1f/stretch.value}.background(colors.surface,CircleShape))
        }
    }
}
