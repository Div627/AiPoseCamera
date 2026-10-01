package com.aipose.camera.pose

import com.aipose.camera.camera.ReferenceStyle

/** Support-compatible groups: no mixing chairs, steps, walls or unsupported sitting. */
object GroupPoses {
    data class Slot(val pose:Int,val x:Float,val y:Float,val scale:Float,val mirror:Boolean=false)
    data class Option(val name:String,val tip:String,val slots:List<Slot>,val category:PoseCategory=PoseCategory.STANDING,val support:PoseSupport=PoseSupport.NONE)
    fun options(count:Int,category:PoseCategory?=null,reference:ReferenceStyle=ReferenceStyle.ALL):List<Option> {
        if(count !in 1..4) return emptyList()
        val all=if(count==1) PoseTemplate.ALL.mapIndexed {i,t->Option(t.name,t.quickTip,listOf(Slot(i,.5f,.5f,1f)),t.category,t.support)}
        else listOf(listOf(0,1,3,5),listOf(4,5,1,0),listOf(6,7,8,6),listOf(9,9,9,9),listOf(10,11,10,11),listOf(12,12,12,12),listOf(13,13,13,13),listOf(14,14,14,14),listOf(15,16,15,16),listOf(17,17,17,17),listOf(18,19,18,19),listOf(21,22,23,21)).mapIndexed {variant,poses->
            val base=PoseTemplate.ALL[poses[0]]
            val maxWidth=poses.take(count).maxOf {index->
                val p=PoseTemplate.ALL[index].points.values
                (p.maxOf{it.first}-p.minOf{it.first}+.12f).coerceAtLeast(.35f)
            }
            val scale=minOf(when(count){2->.9f;3->.75f;else->.6f},.76f/count/maxWidth)
            Option(listOf("松弛并肩","随意站姿","一起椅坐","台阶合影","地面闲坐","一起靠墙","倚栏合影","扶台合影","轻松蹲坐","单膝合影","并肩慢步","停步互动")[variant],
                "${count}人 · ${base.support.label.ifBlank{"停稳后拍摄"}}；留出间距，面部与四肢不要互相遮挡",(0 until count).map {i->
                    Slot(poses[i],.12f+.76f*(i+.5f)/count,.5f,scale)
                },base.category,base.support)
        }
        val interaction=if(count==2) listOf(
            Option("并肩聊两句","自然转向同伴，留出两人之间的小空隙",listOf(Slot(0,.30f,.50f,.80f),Slot(1,.70f,.49f,.77f)),PoseCategory.STANDING),
            Option("回身交流","一人轻回身，另一人看向同伴；停稳后拍摄",listOf(Slot(19,.30f,.5f,.60f),Slot(20,.70f,.5f,.60f,true)),PoseCategory.DYNAMIC),
            Option("坐着聊旅途","在平整地面坐稳，两人头部高低错开，自然交流",listOf(Slot(10,.29f,.46f,.46f),Slot(11,.71f,.55f,.46f)),PoseCategory.SEATED,PoseSupport.GROUND),
            Option("并肩看远方","两人留一点间距，望向同一侧景物",listOf(Slot(2,.30f,.5f,.72f),Slot(2,.70f,.5f,.72f)),PoseCategory.STANDING)
        ) else if(count==3) listOf(
            Option("旅途轻回望","中间稍靠前，两侧自然回身；保持面部互不遮挡",listOf(Slot(19,.23f,.51f,.43f),Slot(18,.50f,.47f,.46f),Slot(20,.77f,.52f,.43f,true)),PoseCategory.DYNAMIC)
        ) else if(count==4) listOf(
            Option("错落停步","四人前后稍错开，停稳后自然交流",listOf(Slot(21,.16f,.49f,.34f),Slot(22,.38f,.46f,.35f),Slot(18,.61f,.52f,.35f),Slot(23,.83f,.48f,.33f)),PoseCategory.DYNAMIC)
        ) else emptyList()
        val masculine=setOf(1,3,7,10,13,16,19,21)
        val curated=listOf(0,1,6,7,12,16)
        val ordered=if(count==1) all.sortedBy {curated.indexOf(it.slots.first().pose).let{rank->if(rank<0) 100+it.slots.first().pose else rank}} else interaction+all
        return ordered.filter{category==null || it.category==category}.sortedByDescending {o->
            when(reference){ReferenceStyle.ALL->0;ReferenceStyle.MASCULINE->o.slots.count{it.pose in masculine};ReferenceStyle.FEMININE->o.slots.count{it.pose !in masculine}}
        }
    }
    fun targets(option:Option,width:Float,height:Float):List<PoseTemplate> = option.slots.map { slot ->
        val base=PoseTemplate.ALL[slot.pose].forViewport(width,height)
        base.copy(points=base.points.mapValues {(_,p)->(slot.x+(p.first-.5f)*slot.scale*(if(slot.mirror) -1f else 1f)) to (slot.y+(p.second-.5f)*slot.scale)})
    }
}
