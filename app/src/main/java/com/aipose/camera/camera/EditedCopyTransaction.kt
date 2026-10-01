package com.aipose.camera.camera

/** Never write, commit or delete the already-saved original, including on partial copy failure. */
object EditedCopyTransaction {
    fun <T> publish(original:T,create:()->T,write:(T)->Unit,commit:(T)->Unit,delete:(T)->Unit):T {
        val copy=create()
        require(copy!=original){"调色副本不能覆盖原片"}
        try {write(copy);commit(copy)} catch(t:Throwable) {runCatching{delete(copy)};throw t}
        return copy
    }
}
