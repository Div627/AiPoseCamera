package com.aipose.camera.pose

/** Reviewed pixel anchors on four generated 3x2 atlases, expressed in original 512px cells.
 * Each drawing uses exactly the same transform as its landmark target; runtime cells are 384px.
 */
object PoseAtlas {
    const val centerY=.5f
    const val sizeFactor=.76f
    data class Entry(val id:String,val name:String,val category:PoseCategory,val support:PoseSupport,val centerX:Float,val tip:String,val pixels:List<Pair<Int,Int>>)
    val entries=listOf(
        Entry("natural_00","轻搭外套",PoseCategory.STANDING,PoseSupport.NONE,0.50293f,"一侧落重心，手轻搭衣边",listOf(258 to 72,219 to 126,296 to 120,206 to 188,310 to 187,200 to 255,255 to 198,233 to 259,277 to 263,249 to 341,292 to 344,252 to 423,311 to 444)),
        Entry("natural_01","停步听风",PoseCategory.STANDING,PoseSupport.NONE,0.49609f,"停稳小步，转头看向一侧",listOf(250 to 78,208 to 124,300 to 128,187 to 186,305 to 187,209 to 247,284 to 213,224 to 262,276 to 262,226 to 351,265 to 349,228 to 433,263 to 416)),
        Entry("natural_02","侧身回望",PoseCategory.STANDING,PoseSupport.NONE,0.42285f,"身体微侧，舒适范围内回望",listOf(226 to 85,203 to 129,230 to 135,220 to 198,249 to 206,240 to 253,257 to 276,188 to 251,227 to 253,177 to 356,219 to 353,164 to 459,211 to 460)),
        Entry("natural_03","交踝整理衣摆",PoseCategory.STANDING,PoseSupport.NONE,0.61328f,"交踝站稳，轻理衣摆",listOf(321 to 73,271 to 125,357 to 122,248 to 202,373 to 198,263 to 241,331 to 215,276 to 263,332 to 261,290 to 344,342 to 339,313 to 449,355 to 415)),
        Entry("natural_04","轻扶后颈",PoseCategory.STANDING,PoseSupport.NONE,0.48926f,"手轻触后颈，不耸肩",listOf(256 to 80,215 to 131,286 to 132,201 to 127,301 to 208,231 to 94,307 to 266,236 to 246,287 to 245,239 to 343,280 to 343,230 to 439,261 to 452)),
        Entry("natural_05","抱肘放松",PoseCategory.STANDING,PoseSupport.NONE,0.41699f,"轻搭手肘，前脚放松",listOf(202 to 83,166 to 134,261 to 137,172 to 196,262 to 188,188 to 272,181 to 201,186 to 250,248 to 250,182 to 351,232 to 342,192 to 443,215 to 455)),
        Entry("natural_06","侧坐轻回身",PoseCategory.SEATED,PoseSupport.CHAIR,0.47363f,"椅子坐稳，双脚自然错开",listOf(246 to 105,209 to 146,276 to 152,194 to 222,279 to 221,181 to 278,268 to 273,229 to 253,266 to 254,266 to 321,317 to 305,274 to 409,314 to 385)),
        Entry("natural_07","坐下聊两句",PoseCategory.SEATED,PoseSupport.CHAIR,0.50586f,"轻前倾，手自然垂在膝边",listOf(256 to 120,212 to 151,306 to 153,209 to 229,295 to 241,225 to 266,261 to 286,230 to 247,297 to 251,195 to 273,318 to 285,197 to 361,313 to 386)),
        Entry("natural_08","侧坐交腿",PoseCategory.SEATED,PoseSupport.CHAIR,0.5f,"坐稳椅面，再轻轻交腿",listOf(300 to 98,265 to 145,333 to 132,238 to 221,342 to 241,237 to 273,362 to 324,289 to 308,324 to 300,166 to 283,186 to 324,117 to 428,204 to 438)),
        Entry("natural_09","台阶高低腿",PoseCategory.SEATED,PoseSupport.STEPS,0.5f,"选择宽稳台阶，两脚高低错落",listOf(260 to 98,206 to 158,315 to 155,168 to 171,312 to 245,260 to 208,255 to 346,249 to 257,311 to 264,207 to 195,332 to 308,224 to 312,339 to 427)),
        Entry("natural_10","地面单膝坐",PoseCategory.SEATED,PoseSupport.GROUND,0.5f,"坐在平整地面，一手轻撑身后",listOf(311 to 106,210 to 175,364 to 176,164 to 192,392 to 278,233 to 267,431 to 366,277 to 306,333 to 324,160 to 216,327 to 346,138 to 385,205 to 408)),
        Entry("natural_11","自在盘坐",PoseCategory.SEATED,PoseSupport.GROUND,0.51875f,"地面坐稳，双腿舒适盘起",listOf(265 to 127,207 to 176,314 to 169,209 to 266,316 to 265,258 to 403,279 to 395,242 to 318,300 to 318,142 to 340,389 to 341,304 to 416,220 to 412)),
        Entry("natural_12","靠墙轻回眸",PoseCategory.LEANING,PoseSupport.WALL,0.39062f,"靠稳墙，一脚轻曲膝",listOf(207 to 76,174 to 117,226 to 116,168 to 185,242 to 173,179 to 253,250 to 199,206 to 225,241 to 225,235 to 305,252 to 305,183 to 318,252 to 425)),
        Entry("natural_13","倚栏交踝",PoseCategory.LEANING,PoseSupport.RAIL,0.5f,"使用稳固矮栏，双手轻扶",listOf(283 to 65,210 to 114,327 to 112,188 to 202,360 to 188,136 to 265,391 to 265,233 to 236,306 to 242,249 to 327,295 to 342,297 to 461,326 to 429)),
        Entry("natural_14","扶台托腮",PoseCategory.LEANING,PoseSupport.LEDGE,0.52812f,"扶稳台面，轻托脸颊",listOf(333 to 93,280 to 143,342 to 139,304 to 194,338 to 173,367 to 200,338 to 103,218 to 204,259 to 232,192 to 324,259 to 329,174 to 406,217 to 438)),
        Entry("natural_15","放松深蹲",PoseCategory.LOW,PoseSupport.GROUND,0.5375f,"双脚站稳再下蹲，手肘放松",listOf(297 to 122,220 to 183,343 to 182,193 to 260,376 to 262,277 to 326,303 to 330,251 to 307,319 to 307,199 to 330,383 to 330,230 to 392,358 to 392)),
        Entry("natural_16","低身看远处",PoseCategory.LOW,PoseSupport.GROUND,0.49414f,"双脚站稳，一手轻搭高膝",listOf(253 to 133,197 to 179,309 to 175,192 to 276,323 to 245,218 to 352,244 to 294,211 to 314,277 to 303,168 to 332,325 to 283,235 to 377,320 to 396)),
        Entry("natural_17","单膝跪地",PoseCategory.LOW,PoseSupport.GROUND,0.5f,"平软地面单膝跪，前脚踩稳",listOf(255 to 94,206 to 148,325 to 151,198 to 232,329 to 253,261 to 275,301 to 365,233 to 277,308 to 276,196 to 264,319 to 410,183 to 420,333 to 411)),
        Entry("natural_18","迎面慢步",PoseCategory.DYNAMIC,PoseSupport.NONE,0.55625f,"慢步后停稳，双臂自然摆动",listOf(277 to 76,236 to 128,322 to 128,224 to 203,341 to 212,227 to 239,352 to 265,252 to 237,312 to 237,252 to 337,297 to 341,266 to 440,288 to 421)),
        Entry("natural_19","侧向漫步",PoseCategory.DYNAMIC,PoseSupport.NONE,0.5f,"小步侧走，停稳后看镜头",listOf(283 to 84,255 to 137,288 to 141,225 to 216,294 to 218,201 to 273,326 to 264,247 to 251,283 to 249,196 to 347,304 to 344,167 to 440,343 to 437)),
        Entry("natural_20","转身提裙",PoseCategory.DYNAMIC,PoseSupport.NONE,0.5f,"轻转身停稳，不强拧颈部",listOf(253 to 75,204 to 129,276 to 122,185 to 208,292 to 198,183 to 277,322 to 257,207 to 248,271 to 243,244 to 340,253 to 341,223 to 442,248 to 420)),
        Entry("natural_21","停步理袖",PoseCategory.DYNAMIC,PoseSupport.NONE,0.50899f,"停稳脚步，轻整袖口",listOf(281 to 66,217 to 131,298 to 132,215 to 193,319 to 165,297 to 153,309 to 148,239 to 241,291 to 238,246 to 336,290 to 331,247 to 439,288 to 412)),
        Entry("natural_22","侧步扶发",PoseCategory.DYNAMIC,PoseSupport.NONE,0.5f,"小步移重心，手轻扶发",listOf(278 to 77,234 to 131,302 to 130,200 to 173,334 to 136,155 to 194,316 to 76,248 to 237,305 to 238,220 to 332,298 to 339,176 to 433,299 to 434)),
        Entry("natural_23","迈步扶外套",PoseCategory.DYNAMIC,PoseSupport.NONE,0.47303f,"停在自然迈步姿态，轻扶外套",listOf(234 to 75,197 to 131,283 to 134,189 to 189,295 to 191,216 to 166,281 to 166,210 to 254,276 to 254,232 to 338,266 to 337,246 to 435,253 to 414))
    )
    val centers=entries.map{it.centerX}
    fun points(index:Int):Landmarks {
        // Anatomical left is screen-right in a frontal reference. Mirrored matching is supported.
        val ids=if(index in listOf(2,20)) listOf(0,11,12,13,14,15,16,23,24,25,26,27,28) else listOf(0,12,11,14,13,16,15,24,23,26,25,28,27)
        return ids.zip(entries[index].pixels).associate {(id,p)->
            id to ((.5f+(p.first/512f-centers[index])*sizeFactor/.75f) to (.5f+(p.second/512f-centerY)*sizeFactor))
        }
    }
}
