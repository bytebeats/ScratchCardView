package me.bytebeats.views.scratch

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.absoluteValue

/**
 * Created by bytebeats on 2021/8/24 : 15:54
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
class ScratchTextView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleRes: Int = 0
) : AppCompatTextView(context, attributes, defStyleRes), IScratchView {
    private var mScratchBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mErasePath by lazy { Path() }
    private val mTouchPath by lazy { Path() }
    private val mBitmapPaint by lazy { Paint(Paint.DITHER_FLAG) }
    private val mErasePaint by lazy {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            color = (0xFFFF0000).toInt()
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.BEVEL
            strokeCap = Paint.Cap.ROUND
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            strokeWidth = OnScratchListener.DEFAULT_STROKE_WIDTH
        }
    }
    private val mGradientPaint by lazy { Paint() }
    private var mForegroundDrawable: BitmapDrawable? = null
    var onScratchListener: OnScratchListener<ScratchTextView>? = null
    private var mScratchPercent: Float = 0.0F

    private var mGradientStartColor = OnScratchListener.SCRATCH_GRADIENT_START_COLOR
    private var mGradientEndColor = OnScratchListener.SCRATCH_GRADIENT_END_COLOR
    var mStrokeWidth: Float = 0f
        set(value) {
            field = value
            mErasePaint.strokeWidth = field
        }

    private var mX = 0.0F
    private var mY = 0.0F

    private var mBackgroundHandler: Handler? = null
    private val mHandlerThread by lazy {
        object : HandlerThread("$TAG-background") {
            override fun onLooperPrepared() {
                mBackgroundHandler = object : Handler(looper) {
                    override fun handleMessage(msg: Message) {
                        super.handleMessage(msg)
                        if (msg.what == IScratchView.MSG_TOUCH_MOVE) {
                            scratchOnBackground()
                        }
                    }
                }
            }
        }
    }

    init {
        val a = context.obtainStyledAttributes(attributes, R.styleable.ScratchView)
        mForegroundDrawable = a.getDrawable(R.styleable.ScratchView_foreground) as BitmapDrawable?
        mForegroundDrawable?.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        mGradientStartColor = a.getColor(
            R.styleable.ScratchView_gradientStartColor,
            OnScratchListener.SCRATCH_GRADIENT_START_COLOR
        )
        mGradientEndColor = a.getColor(
            R.styleable.ScratchView_gradientEndColor,
            OnScratchListener.SCRATCH_GRADIENT_END_COLOR
        )
        mStrokeWidth = a.getDimension(
            R.styleable.ScratchView_stroke,
            OnScratchListener.DEFAULT_STROKE_WIDTH
        )
        text = a.getString(R.styleable.ScratchView_revealedText)
        a.recycle()
        mHandlerThread.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mScratchBitmap?.recycle()
        mScratchBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mScratchBitmap!!)
        val rect = Rect(0, 0, mScratchBitmap!!.width, mScratchBitmap!!.height)
        mForegroundDrawable?.let {
            it.bounds = rect
            mGradientPaint.shader = LinearGradient(
                0F,
                0F,
                0F,
                height.toFloat(),
                mGradientStartColor,
                mGradientEndColor,
                Shader.TileMode.MIRROR
            )
            mCanvas?.drawRect(rect, mGradientPaint)
            mForegroundDrawable?.draw(mCanvas!!)
        } ?: run { mCanvas?.drawColor(OnScratchListener.SCRATCH_GRADIENT_START_COLOR) }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(mScratchBitmap!!, 0F, 0F, mBitmapPaint)
        canvas?.drawPath(mErasePath, mErasePaint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mX = event.x
                mY = event.y
                mErasePath.reset()
                mErasePath.moveTo(mX, mY)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                val dx = (mX - x).absoluteValue
                val dy = (mY - y).absoluteValue
                if (dx > OnScratchListener.DEFAULT_TOUCH_TOLERANCE || dy > OnScratchListener.DEFAULT_TOUCH_TOLERANCE) {
                    mErasePath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                    mX = x
                    mY = y
                    drawPath()
                }
                mTouchPath.reset()
                mTouchPath.addCircle(mX, mY, 30F, Path.Direction.CW)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                drawPath()
                invalidate()
            }

        }
        return true
    }

    private fun drawPath() {
        mErasePath.lineTo(mX, mY)
        mCanvas?.drawPath(mErasePath, mErasePaint)
        mTouchPath.reset()
        mErasePath.reset()
        mErasePath.moveTo(mX, mY)
        checkRevealed()
    }

    private fun checkRevealed() {
        if (!isRevealed() && onScratchListener != null) {
            if (mBackgroundHandler?.hasMessages(IScratchView.MSG_TOUCH_MOVE) == true) {
                return
            }
            mBackgroundHandler?.sendEmptyMessage(IScratchView.MSG_TOUCH_MOVE)
        }
    }

    override fun reveal() {
        val bounds = textBounds(1.5f).map { it.toFloat() }
        val paint = Paint()
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        mCanvas?.drawRect(bounds[0], bounds[1], bounds[2], bounds[3], paint)
        checkRevealed()
        invalidate()
    }

    private fun scratchOnBackground() {
        val bounds = bounds()
        val left = bounds[0]
        val top = bounds[1]
        val w = bounds[2] - left
        val h = bounds[3] - top
        val croppedBitmap = Bitmap.createBitmap(mScratchBitmap!!, left, top, w, h)
        val revealedPercent = BitmapHelper.transparentPixelPercent(croppedBitmap)
        post { callbackOnUIThread(revealedPercent) }
    }

    private fun callbackOnUIThread(scratchedPercent: Float) {
        if (!isRevealed()) {
            val old = mScratchPercent
            mScratchPercent = scratchedPercent
            if (old != mScratchPercent) {
                onScratchListener?.onScratchChanged(this, scratchedPercent)
            }
            if (isRevealed()) {
                onScratchListener?.onRevealed(this)
            }
        }
    }

    override fun isRevealed(): Boolean = mScratchPercent > 0.95F

    override fun bounds(): IntArray = textBounds()

    private fun textBounds(scale: Float = 1.0F): IntArray {
        val centerX = width / 2
        val centerY = height / 2
        val dimens = textDimens()
        var w = dimens[0]
        var h = dimens[1]
        val lines = lineCount
        w /= lines
        h *= lines
        h = if (h > height) {
            height - paddingBottom - paddingTop
        } else {
            (h * scale).toInt()
        }
        w = if (w > width) {
            width - paddingLeft - paddingRight
        } else {
            (w * scale).toInt()
        }

        val left = if (gravity and Gravity.LEFT == Gravity.LEFT) {
            paddingLeft
        } else if (gravity and Gravity.RIGHT == Gravity.RIGHT) {
            width - paddingRight - w
        } else if (gravity and Gravity.CENTER_HORIZONTAL == Gravity.CENTER_HORIZONTAL) {
            centerX - w / 2
        } else {
            0
        }

        val top = if (gravity and Gravity.TOP == Gravity.TOP) {
            paddingTop
        } else if (gravity and Gravity.BOTTOM == Gravity.BOTTOM) {
            height - paddingBottom - h
        } else if (gravity and Gravity.CENTER_VERTICAL == Gravity.CENTER_VERTICAL) {
            centerY - h / 2
        } else {
            0
        }
        return intArrayOf(left, top, left + w, top + h)
    }

    private fun textDimens(): IntArray {
        val bounds = Rect()
        paint.getTextBounds(text.toString(), 0, text.length, bounds)
        val w = bounds.left + bounds.width()
        val h = bounds.bottom + bounds.height()
        return intArrayOf(w, h)
    }

    override fun reset() {

    }

    companion object {
        private const val TAG = "ScratchTextView"
    }
}