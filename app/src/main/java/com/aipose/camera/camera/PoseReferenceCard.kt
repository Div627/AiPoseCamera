package com.aipose.camera.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aipose.camera.pose.GroupPoses
import com.aipose.camera.pose.HumanPoseGuide
import com.aipose.camera.ui.theme.BgDark
import com.aipose.camera.ui.theme.TextPrimary
import com.aipose.camera.ui.theme.TextSecondary

@Composable
fun PoseReferenceCard(pose: GroupPoses.Option, onChoose: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.widthIn(max = 186.dp).clip(RoundedCornerShape(16.dp)).background(BgDark.copy(alpha = .76f))
        .semantics { contentDescription = "当前姿势参考：${pose.name}，点击更换" }
        .clickable(onClick = onChoose).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        HumanPoseGuide(pose, Modifier.size(62.dp, 82.dp).clip(RoundedCornerShape(9.dp)), thumbnail = true)
        Column(Modifier.padding(start = 10.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(pose.name, color = TextPrimary, style = MaterialTheme.typography.labelLarge, maxLines = 2)
            Text("轻点换姿势", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
