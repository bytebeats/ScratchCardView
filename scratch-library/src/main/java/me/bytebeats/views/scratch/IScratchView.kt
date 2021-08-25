package me.bytebeats.views.scratch

/**
 * Created by bytebeats on 2021/8/25 : 11:27
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
interface IScratchView {

    fun reveal()
    fun reset()
    fun bounds(): IntArray
    fun isRevealed(): Boolean

    companion object {
        internal const val MSG_TOUCH_MOVE = 0x1001
    }
}