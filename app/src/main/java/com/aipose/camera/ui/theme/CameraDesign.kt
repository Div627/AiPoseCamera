package com.aipose.camera.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared tokens; see /design.md. All dimensions are dp, typography uses scalable sp. */
object CameraDesign {
    val Page=20.dp;val Gap=12.dp;val Section=24.dp
    val Target=48.dp;val Icon=24.dp;val Shutter=80.dp
    val OptionRadius=12.dp;val CardRadius=20.dp;val SheetRadius=28.dp
    val Border=TextPrimary.copy(alpha=.14f)
    val BottomScrim=Brush.verticalGradient(listOf(Color.Transparent,BgDark.copy(alpha=.78f)))
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CameraSheet(title:String,onClose:()->Unit,content:@Composable ColumnScope.()->Unit) {
    ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        shape=RoundedCornerShape(topStart=CameraDesign.SheetRadius,topEnd=CameraDesign.SheetRadius),containerColor=SurfaceDark,tonalElevation=0.dp) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.62f).padding(horizontal=CameraDesign.Page).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Text(title,style=MaterialTheme.typography.titleMedium)
                IconButton(onClick=onClose,modifier=Modifier.size(CameraDesign.Target)) {Icon(Icons.Default.Close,"关闭$title",modifier=Modifier.size(CameraDesign.Icon))}
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
