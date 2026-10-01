package com.aipose.camera.camera

/** Keep only the connected foreground under the tap; disconnected background never drives zoom. */
object TapSubjectMask {
    const val GRID = 96
    data class Edge(val from: SubjectColor.Point, val to: SubjectColor.Point)
    data class Selection(val box: SubjectFraming.Box, val region: SubjectColor.Region, val edges: List<Edge>)
    fun select(confidence: FloatArray, width: Int, height: Int, x: Float, y: Float): Selection? {
        if (width <= 0 || height <= 0 || confidence.size != width * height || !x.isFinite() || !y.isFinite() || x !in 0f..1f || y !in 0f..1f) return null
        val n=GRID
        val mask=BooleanArray(n*n) { i ->
            val px=((i%n+.5f)*width/n).toInt().coerceAtMost(width-1)
            val py=((i/n+.5f)*height/n).toInt().coerceAtMost(height-1)
            confidence[py*width+px] >= .65f
        }
        val tx=(x*n).toInt().coerceIn(0,n-1);val ty=(y*n).toInt().coerceIn(0,n-1)
        val seed=(0 until n*n).filter { mask[it] && kotlin.math.abs(it%n-tx)<=2 && kotlin.math.abs(it/n-ty)<=2 }
            .minByOrNull { (it%n-tx)*(it%n-tx)+(it/n-ty)*(it/n-ty) } ?: return null
        val selected=BooleanArray(n*n);val queue=IntArray(n*n);var head=0;var tail=1;queue[0]=seed;selected[seed]=true
        while(head<tail) {
            val i=queue[head++];val cx=i%n;val cy=i/n
            for(j in listOf(if(cx>0) i-1 else -1,if(cx<n-1) i+1 else -1,if(cy>0) i-n else -1,if(cy<n-1) i+n else -1))
                if(j>=0 && mask[j] && !selected[j]) {selected[j]=true;queue[tail++]=j}
        }
        // Reject tiny noise and masks that claim almost the entire picture.
        if(tail<24 || tail>n*n*.88f) return null
        var left=n;var top=n;var right=0;var bottom=0
        val edges=mutableListOf<Edge>()
        fun p(a:Int,b:Int)=SubjectColor.Point(a.toFloat()/n,b.toFloat()/n)
        for(i in queue.take(tail)) {
            val a=i%n;val b=i/n;left=minOf(left,a);top=minOf(top,b);right=maxOf(right,a+1);bottom=maxOf(bottom,b+1)
            if(b==0 || !selected[i-n]) edges+=Edge(p(a,b),p(a+1,b))
            if(a==n-1 || !selected[i+1]) edges+=Edge(p(a+1,b),p(a+1,b+1))
            if(b==n-1 || !selected[i+n]) edges+=Edge(p(a+1,b+1),p(a,b+1))
            if(a==0 || !selected[i-1]) edges+=Edge(p(a,b+1),p(a,b))
        }
        val box=SubjectFraming.Box(left.toFloat()/n,top.toFloat()/n,right.toFloat()/n,bottom.toFloat()/n)
        val colorMask=BooleanArray(32*32) {i->selected[(i/32*3+1)*n+i%32*3+1]}
        if(colorMask.count {it}<3) return null
        return Selection(box,SubjectColor.Region(emptyList(),colorMask),edges)
    }
}
