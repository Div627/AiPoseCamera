package com.aipose.camera.assistant

object VideoSummary {
    private val names = mapOf("Mountain" to "山地", "Sky" to "天空", "Cloud" to "云层", "Water" to "水面",
        "Beach" to "海滩", "Plant" to "植物", "Tree" to "树木", "Forest" to "树林", "Flower" to "花朵",
        "Person" to "人物", "People" to "人物", "Face" to "人脸", "Food" to "食物", "Building" to "建筑",
        "Road" to "道路", "Sunset" to "日落", "Lake" to "湖泊", "Ocean" to "海面", "Landscape" to "风景",
        "Nature" to "自然景物", "Snow" to "雪景", "Outdoor" to "户外", "Portrait" to "人像")
    fun time(ms:Long):String = String.format(java.util.Locale.getDefault(),"%.1f",ms/1000f)
    fun label(text: String): String = names[text] ?: text
    fun describe(study: VideoStudy): String {
        val labels = study.frames.filter {it.score>=.5f}.flatMap {it.labels}.groupingBy {it}.eachCount().entries.sortedByDescending {it.value}.take(5).map {label(it.key)}
        val people = study.frames.filter {it.faces > 0}
        return buildString {
            append("**这段视频 · ${study.durationMs / 1000} 秒**\n\n")
            if(labels.isNotEmpty()) append("抽样画面识别到：${labels.joinToString("、")}。\n\n")
            if(people.isNotEmpty()) append("在人像画面中检出人脸，最早约 ${people.first().timeMs / 1000} 秒，最晚约 ${people.last().timeMs / 1000} 秒。\n\n")
            append("已推荐 ${study.candidates.size} 张候选画面，可调整风格和裁剪后保存。原视频保留在本机。\n\n")
            if(!study.modelAvailable) append("部分标签或人脸分析不可用，已保留基础画面推荐。\n\n")
            append("这是抽样画面摘要，未分析对白或完整故事；选片依据基础质量和时间分布。")
        }
    }
}
