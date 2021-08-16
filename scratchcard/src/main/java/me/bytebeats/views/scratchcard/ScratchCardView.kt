package me.bytebeats.views.scratchcard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes

/**
 * Created by bytebeats on 2021/8/16 : 11:48
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

/**
 * Scratch card view
 */
class ScratchCardView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleRes: Int = 0
) : View(context, attributes, defStyleRes) {
    private var mBackgroundResource = 0
    private var mBackgroundDrawable: Drawable? = null
    private var mBackgroundRecycleableBitmapDrawable: BitmapDrawable? = null
    private var mDrawableWidth = 0
    private var mDrawableHeight = 0

    private var mForegroundResource = 0
    private var mForegroundDrawable: Drawable? = null
    private var mForegroundRecycleableBitmapDrawable: BitmapDrawable? = null


    /**
     * Sets a drawable as the content of this ImageView.
     *
     * @param drawable the Drawable to set, or `null` to clear the
     * content
     */
    fun setBackgroundImageDrawable(drawable: Drawable?) {
        if (mBackgroundDrawable !== drawable) {
            mBackgroundResource = 0
            val oldWidth: Int = mDrawableWidth
            val oldHeight: Int = mDrawableHeight
            updateBackgroundDrawable(drawable)
            if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
                requestLayout()
            }
            invalidate()
        }
    }

    /**
     * Sets a drawable as the content of this ImageView.
     *
     * This does Bitmap reading and decoding on the UI
     * thread, which can cause a latency hiccup.  If that's a concern,
     * consider using [.setImageDrawable] or
     * [.setImageBitmap] and
     * [android.graphics.BitmapFactory] instead.
     *
     * @param resId the resource identifier of the drawable
     *
     * @attr ref android.R.styleable#ImageView_src
     */
    fun setBackgroundImageResource(@DrawableRes resId: Int) {
        // The resource configuration may have changed, so we should always
        // try to load the resource even if the resId hasn't changed.
        val oldWidth: Int = mDrawableWidth
        val oldHeight: Int = mDrawableHeight
        updateBackgroundDrawable(null)
        mBackgroundResource = resId
        if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
            requestLayout()
        }
        invalidate()
    }


    /**
     * Sets a Bitmap as the content of this ImageView.
     *
     * @param bm The bitmap to set
     */
    fun setBackgroundImageBitmap(bm: Bitmap?) {
        // Hacky fix to force setImageDrawable to do a full setImageDrawable
        // instead of doing an object reference comparison
        mBackgroundDrawable = null
        mBackgroundResource = 0
        mBackgroundRecycleableBitmapDrawable = BitmapDrawable(context.resources, bm)
        setBackgroundImageDrawable(mBackgroundRecycleableBitmapDrawable)
    }

    private fun updateBackgroundDrawable(d: Drawable?) {
        if (d !== mBackgroundRecycleableBitmapDrawable && mBackgroundRecycleableBitmapDrawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mBackgroundRecycleableBitmapDrawable!!.bitmap = null
            }
        }
        var sameDrawable = false
        if (mBackgroundDrawable != null) {
            sameDrawable = mBackgroundDrawable === d
            mBackgroundDrawable!!.callback = null
            unscheduleDrawable(mBackgroundDrawable)
            if (!sameDrawable && isAttachedToWindow) {
                mBackgroundDrawable!!.setVisible(false, false)
            }
        }
        mBackgroundDrawable = d
        if (d != null) {
            d.callback = this
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                d.layoutDirection = layoutDirection
            }
            if (d.isStateful) {
                d.state = drawableState
            }
            if (!sameDrawable) {
                val visible = isAttachedToWindow && windowVisibility == VISIBLE && isShown
                d.setVisible(visible, true)
            }
            mDrawableWidth = d.intrinsicWidth
            mDrawableHeight = d.intrinsicHeight
        } else {
            mDrawableHeight = -1
            mDrawableWidth = mDrawableHeight
        }
    }
}