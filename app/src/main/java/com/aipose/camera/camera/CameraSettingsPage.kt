package com.aipose.camera.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipose.camera.ui.theme.LightUiTheme

@Composable
fun CameraSettingsPage(onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) = LightUiTheme {
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onBack) {Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回相机")}
                Text("设置",style=MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=12.dp),
                verticalArrangement=Arrangement.spacedBy(20.dp),content=content)
        }
    }
}

@Composable
fun SettingsGroup(title:String,content:@Composable ColumnScope.()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(title,Modifier.padding(start=4.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=4.dp),content=content)
        }
    }
}

@Composable
fun SettingsAction(label:String,onClick:()->Unit,expanded:Boolean=false) {
    Row(Modifier.fillMaxWidth().heightIn(min=54.dp).clickable(onClick=onClick),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
        Icon(if(expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsTimer(seconds:Int,onChange:(Int)->Unit) {
    var expanded by remember {mutableStateOf(false)}
    Row(Modifier.fillMaxWidth().heightIn(min=54.dp),verticalAlignment=Alignment.CenterVertically) {
        Text("延时拍摄",Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
        Box {
            TextButton(onClick={expanded=true}) {Text(if(seconds==0) "关闭" else "$seconds 秒");Icon(Icons.Default.ExpandMore,null,Modifier.size(18.dp))}
            DropdownMenu(expanded,onDismissRequest={expanded=false}) {
                listOf(0,2,5,10).forEach {value -> DropdownMenuItem(text={Text(if(value==0) "关闭" else "$value 秒")},onClick={expanded=false;onChange(value)})}
            }
        }
    }
}
