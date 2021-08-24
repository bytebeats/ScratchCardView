package me.bytebeats.views.scratch

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.get
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


    private var mForegroundDrawable: Drawable? = null
    private var mForegroundBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null

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
        val a =
            context.obtainStyledAttributes(attributes, R.styleable.ScratchCardView)
        mForegroundDrawable = a.getDrawable(R.styleable.ScratchCardView_foregroundDrawable)
        mText = a.getString(R.styleable.ScratchCardView_text)
        mStrokeWidth = a.getDimension(R.styleable.ScratchCardView_strokeWith, 40F)
        a.recycle()
    }

    override fun onDraw(canvas: Canvas?) {
//        if (mText != null) {
//            canvas?.drawText(
//                mText!!,
//                (paddingLeft + mDrawableWidth / 2).toFloat(),
//                (paddingTop + mDrawableHeight / 2).toFloat(),
//                mTextPaint
//            )
//        }
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


    fun setTextColor(@ColorInt textColor: Int) {
        mTextPaint.color = textColor
    }

    interface OnScratchListener {
        fun onScratch(cardView: ScratchCardView?, visiblePercent: Float)
    }
}