package me.bytebeats.views.scratch

import android.view.View

/**
 * Created by bytebeats on 2021/8/24 : 15:44
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
interface OnScratchListener<T : View> {
    fun onRevealed(view: T)
    fun onScratchChanged(view: T, visiblePercent: Float)

    companion object {
        const val DEFAULT_STROKE_WIDTH = 12f
        const val DEFAULT_TOUCH_TOLERANCE = 4
        const val SCRATCH_GRADIENT_START_COLOR = 0xe5e5e5
        const val SCRATCH_GRADIENT_END_COLOR = 0xcccccc
    }
}