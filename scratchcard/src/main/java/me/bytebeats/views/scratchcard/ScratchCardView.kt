package me.bytebeats.views.scratchcard

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.get
import kotlin.math.abs
import kotlin.math.absoluteValue

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
    var mText: String? = null
        set(value) {
            field = value
            invalidate()
        }
    var mStrokeWidth: Float = 0F
        set(value) {
            field = value
            mInnerPaint.strokeWidth = field
        }


    private var mForegroundResource = 0
    private var mForegroundDrawable: Drawable? = null
    private var mForegroundBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private var mForegroundRecycleableBitmapDrawable: BitmapDrawable? = null

    private var mDrawableWidth = 0
    private var mDrawableHeight = 0

    private var mMaxWidth = Int.MAX_VALUE
    private var mMaxHeight = Int.MAX_VALUE

    var onScratchListener: OnScratchListener? = null

    private val mInnerPaint by lazy {
        Paint().apply {
            alpha = 0
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            style = Paint.Style.STROKE
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 50F
            strokeCap = Paint.Cap.ROUND
        }
    }

    private val mTextPaint by lazy {
        Paint().apply {
            alpha = 0
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            style = Paint.Style.STROKE
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 50F
            strokeCap = Paint.Cap.ROUND
        }
    }

    private val mPath by lazy {
        Path()
    }

    private val mOuterPaint by lazy {
        Paint()
    }

    private var mX = 0F
    private var mY = 0F

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        val a =
            context.obtainStyledAttributes(attributes, R.styleable.ScratchCardView, 0, defStyleRes)
        mForegroundDrawable = a.getDrawable(R.styleable.ScratchCardView_foregroundImage)
        mText = a.getString(R.styleable.ScratchCardView_text)
        mStrokeWidth = a.getDimension(R.styleable.ScratchCardView_strokeWith, 40F)
        a.recycle()
        mForegroundDrawable?.let { updateForegroundDrawable(it) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var w: Int
        var h: Int

        // Desired aspect ratio of the view's contents (not including padding)
        var desiredAspect = 0.0f

        // We are allowed to change the view's width
        var resizeWidth = false

        // We are allowed to change the view's height
        var resizeHeight = false
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        if (mForegroundDrawable == null) {
            // If no drawable, its intrinsic size is 0.
            mDrawableWidth = -1
            mDrawableHeight = -1
            h = 0
            w = h
        } else {
            w = mDrawableWidth
            h = mDrawableHeight
            if (w <= 0) w = 1
            if (h <= 0) h = 1
        }
        val pleft: Int = paddingLeft
        val pright: Int = paddingRight
        val ptop: Int = paddingTop
        val pbottom: Int = paddingBottom
        var widthSize: Int
        var heightSize: Int
        if (resizeWidth || resizeHeight) {
            /* If we get here, it means we want to resize to match the
                drawables aspect ratio, and we have the freedom to change at
                least one dimension.
            */

            // Get the max possible width given our constraints
            widthSize = resolveAdjustedSize(w + pleft + pright, mMaxWidth, widthMeasureSpec)

            // Get the max possible height given our constraints
            heightSize = resolveAdjustedSize(h + ptop + pbottom, mMaxHeight, heightMeasureSpec)
            if (desiredAspect != 0.0f) {
                // See what our actual aspect ratio is
                val actualAspect = (widthSize - pleft - pright).toFloat() /
                        (heightSize - ptop - pbottom)
                if (abs(actualAspect - desiredAspect) > 0.0000001) {
                    var done = false

                    // Try adjusting width to be proportional to height
                    if (resizeWidth) {
                        val newWidth = (desiredAspect * (heightSize - ptop - pbottom)).toInt() +
                                pleft + pright

                        // Allow the width to outgrow its original estimate if height is fixed.
                        if (!resizeHeight) {
                            widthSize = resolveAdjustedSize(newWidth, mMaxWidth, widthMeasureSpec)
                        }
                        if (newWidth <= widthSize) {
                            widthSize = newWidth
                            done = true
                        }
                    }

                    // Try adjusting height to be proportional to width
                    if (!done && resizeHeight) {
                        val newHeight = ((widthSize - pleft - pright) / desiredAspect).toInt() +
                                ptop + pbottom

                        // Allow the height to outgrow its original estimate if width is fixed.
                        if (!resizeWidth) {
                            heightSize = resolveAdjustedSize(
                                newHeight, mMaxHeight,
                                heightMeasureSpec
                            )
                        }
                        if (newHeight <= heightSize) {
                            heightSize = newHeight
                        }
                    }
                }
            }
        } else {
            /* We are either don't want to preserve the drawables aspect ratio,
               or we are not allowed to change view dimensions. Just measure in
               the normal way.
            */
            w += pleft + pright
            h += ptop + pbottom
            w = w.coerceAtLeast(suggestedMinimumWidth)
            h = h.coerceAtLeast(suggestedMinimumHeight)
            widthSize = resolveSizeAndState(w, widthMeasureSpec, 0)
            heightSize = resolveSizeAndState(h, heightMeasureSpec, 0)
        }
        setMeasuredDimension(widthSize, heightSize)
    }

    private fun resolveAdjustedSize(
        desiredSize: Int, maxSize: Int,
        measureSpec: Int
    ): Int {
        var result = desiredSize
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        when (specMode) {
            MeasureSpec.UNSPECIFIED ->                 /* Parent says we can be as big as we want. Just don't be larger
                   than max size imposed on ourselves.
                */result = desiredSize.coerceAtMost(maxSize)
            MeasureSpec.AT_MOST ->                 // Parent says we can be as big as we want, up to specSize.
                // Don't be larger than specSize, and don't be larger than
                // the max size imposed on ourselves.
                result = desiredSize.coerceAtMost(specSize).coerceAtMost(maxSize)
            MeasureSpec.EXACTLY ->                 // No choice. Do what we are told.
                result = specSize
        }
        return result
    }

    override fun onDraw(canvas: Canvas?) {
        if (mText != null) {
            canvas?.drawText(
                mText!!,
                (paddingLeft + mDrawableWidth / 2).toFloat(),
                (paddingTop + mDrawableHeight / 2).toFloat(),
                mTextPaint
            )
        }
        canvas?.drawBitmap(mForegroundBitmap!!, 0F, 0F, mOuterPaint)
        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)
        event?.let { e ->
            val curX = e.x
            val curY = e.y
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    mPath.reset()
                    mPath.moveTo(curX, curY)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (curX - mX).absoluteValue
                    val dy = (curY - mY).absoluteValue
                    if (dx >= 4 || dy >= 4) {
                        val nextX = (curX + mX) / 2
                        val nextY = (curY + mY) / 2
                        mPath.quadTo(mX, mY, nextX, nextY)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    mPath.lineTo(curX, curY)
                    mForegroundBitmap?.let {
                        val width = it.width
                        val height = it.height
                        val total = width * height
                        var visiblePixels = 0
                        for (i in 0 until width) {
                            for (j in 0 until height) {
                                if (it[i, j] == 0x00000000) {
                                    visiblePixels++
                                }
                            }
                        }
                        onScratchListener?.onScratch(this, visiblePixels.toFloat() / total * 9)
                    }
                }
            }
            mCanvas?.drawPath(mPath, mInnerPaint)
            mX = curX
            mY = curY
            invalidate()
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mForegroundBitmap?.recycle()
        mForegroundBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mForegroundBitmap!!)
        mForegroundDrawable?.let {
            it.setBounds(0, 0, mForegroundBitmap!!.width, mForegroundBitmap!!.height)
            draw(mCanvas)
        } ?: run { mCanvas?.drawARGB(0xff, 0xc0, 0xc0, 0xc0) }
        mInnerPaint.strokeWidth = mStrokeWidth
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mForegroundBitmap?.recycle()
        mForegroundBitmap = null
    }

    /**
     * Sets a drawable as the src content of this ScratchCardView.
     *
     * @param drawable the Drawable to set, or `null` to clear the
     * content
     */
    fun setForegroundImageDrawable(drawable: Drawable?) {
        if (mForegroundDrawable !== drawable) {
            mForegroundResource = 0
            val oldWidth: Int = mDrawableWidth
            val oldHeight: Int = mDrawableHeight
            updateForegroundDrawable(drawable)
            if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
                requestLayout()
            }
            invalidate()
        }
    }

    /**
     * Sets a drawable as the src content of this ScratchCardView.
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
    fun setForegroundImageResource(@DrawableRes resId: Int) {
        // The resource configuration may have changed, so we should always
        // try to load the resource even if the resId hasn't changed.
        val oldWidth: Int = mDrawableWidth
        val oldHeight: Int = mDrawableHeight
        updateForegroundDrawable(null)
        mForegroundResource = resId
        if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
            requestLayout()
        }
        invalidate()
    }


    /**
     * Sets a Bitmap as the src content of this ScratchCardView.
     *
     * @param bm The bitmap to set
     */
    fun setForegroundImageBitmap(bm: Bitmap?) {
        // Hacky fix to force setImageDrawable to do a full setImageDrawable
        // instead of doing an object reference comparison
        mForegroundDrawable = null
        mForegroundResource = 0
        mForegroundRecycleableBitmapDrawable = BitmapDrawable(context.resources, bm)
        setForegroundImageDrawable(mForegroundRecycleableBitmapDrawable)
    }

    private fun updateForegroundDrawable(d: Drawable?) {
        if (d !== mForegroundRecycleableBitmapDrawable && mForegroundRecycleableBitmapDrawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mForegroundRecycleableBitmapDrawable!!.bitmap = null
            }
        }
        var sameDrawable = false
        if (mForegroundDrawable != null) {
            sameDrawable = mForegroundDrawable === d
            mForegroundDrawable!!.callback = null
            unscheduleDrawable(mForegroundDrawable)
            if (!sameDrawable && isAttachedToWindow) {
                mForegroundDrawable!!.setVisible(false, false)
            }
        }
        mForegroundDrawable = d
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

    fun getForegroundDrawable(): Drawable? {
        if (mForegroundDrawable === mForegroundRecycleableBitmapDrawable) {
            // Consider our cached version dirty since app code now has a reference to it
            mForegroundRecycleableBitmapDrawable = null
        }
        return mForegroundDrawable
    }

    /**
     * The maximum width of this view.
     *
     * @return The maximum width of this view
     *
     * @see .setMaxWidth
     * @attr ref android.R.styleable#ImageView_maxWidth
     */
    fun getMaxWidth(): Int {
        return mMaxWidth
    }

    /**
     * An optional argument to supply a maximum width for this view. Only valid if
     * [.setAdjustViewBounds] has been set to true. To set an image to be a maximum
     * of 100 x 100 while preserving the original aspect ratio, do the following: 1) set
     * adjustViewBounds to true 2) set maxWidth and maxHeight to 100 3) set the height and width
     * layout params to WRAP_CONTENT.
     *
     *
     *
     * Note that this view could be still smaller than 100 x 100 using this approach if the original
     * image is small. To set an image to a fixed size, specify that size in the layout params and
     * then use [.setScaleType] to determine how to fit
     * the image within the bounds.
     *
     *
     * @param maxWidth maximum width for this view
     *
     * @see .getMaxWidth
     * @attr ref android.R.styleable#ImageView_maxWidth
     */
    fun setMaxWidth(maxWidth: Int) {
        mMaxWidth = maxWidth
    }

    /**
     * The maximum height of this view.
     *
     * @return The maximum height of this view
     *
     * @see .setMaxHeight
     * @attr ref android.R.styleable#ImageView_maxHeight
     */
    fun getMaxHeight(): Int {
        return mMaxHeight
    }

    /**
     * An optional argument to supply a maximum height for this view. Only valid if
     * [.setAdjustViewBounds] has been set to true. To set an image to be a
     * maximum of 100 x 100 while preserving the original aspect ratio, do the following: 1) set
     * adjustViewBounds to true 2) set maxWidth and maxHeight to 100 3) set the height and width
     * layout params to WRAP_CONTENT.
     *
     *
     *
     * Note that this view could be still smaller than 100 x 100 using this approach if the original
     * image is small. To set an image to a fixed size, specify that size in the layout params and
     * then use [.setScaleType] to determine how to fit
     * the image within the bounds.
     *
     *
     * @param maxHeight maximum height for this view
     *
     * @see .getMaxHeight
     * @attr ref android.R.styleable#ImageView_maxHeight
     */
    fun setMaxHeight(maxHeight: Int) {
        mMaxHeight = maxHeight
    }

    private fun resizeFromDrawable() {
        val d = mForegroundDrawable
        if (d != null) {
            var w = d.intrinsicWidth
            if (w < 0) w = mDrawableWidth
            var h = d.intrinsicHeight
            if (h < 0) h = mDrawableHeight
            if (w != mDrawableWidth || h != mDrawableHeight) {
                mDrawableWidth = w
                mDrawableHeight = h
                requestLayout()
            }
        }
    }

    fun setTextColor(@ColorInt textColor: Int) {
        mTextPaint.setColor(textColor)
    }

    interface OnScratchListener {
        fun onScratch(cardView: ScratchCardView?, visiblePercent: Float)
    }
}