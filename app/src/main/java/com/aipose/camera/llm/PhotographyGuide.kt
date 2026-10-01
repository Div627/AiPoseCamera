package com.aipose.camera.llm

object PhotographyGuide {
    const val ASSET="photography/v2/photography-guide.txt"
    const val VERSION="2.0"
    fun systemPrompt(resource:String):String {
        require(resource.contains("ZiYing Photography Guide v2.0")) {"摄影指导资源版本无效"}
        return resource
    }
    fun sceneContext(people:Int?,settled:Boolean,style:String,ev:Float,pose:String?):String = buildString {
        append("人数检测：${people?.toString() ?: "未知"}；稳定确认：$settled；")
        append("场景：${when {people==null->"未知";people==0->"风景/非人像";people==1->"单人";people<=4->"多人合影";else->"超出四人引导范围"}}；")
        append("当前滤镜：$style；曝光补偿EV=$ev。")
        if(people!=null && people in 1..4 && pose!=null) append("选择的姿势组合：$pose。")
        append("未上传照片；请根据已知数据提供下一步建议。")
    }
}
