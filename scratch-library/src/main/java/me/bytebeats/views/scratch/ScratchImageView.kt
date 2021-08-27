package me.bytebeats.views.scratch

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import androidx.annotation.MainThread
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.absoluteValue

/**
 * Created by bytebeats on 2021/8/24 : 15:54
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
class ScratchImageView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleRes: Int = 0
) : AppCompatImageView(context, attributes, defStyleRes), IScratchView {
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
    var onScratchListener: OnScratchListener<ScratchImageView>? = null
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
        val bounds = bounds().map { it.toFloat() }
        var left = bounds[0]
        var top = bounds[1]
        var right = bounds[2]
        var bottom = bounds[3]

        val w = right - left
        val h = bottom - top
        val centerX = left + w / 2
        val centerY = top + h / 2

        left = centerX - w / 2
        top = centerY - h / 2
        right = left + w
        bottom = top + h

        val paint = Paint()
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        mCanvas?.drawRect(left, top, right, bottom, paint)
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

    @MainThread
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

    override fun bounds(): IntArray = imageBounds()

    private fun imageBounds(): IntArray {
        val vw = width - paddingLeft - paddingRight
        val vh = height - paddingBottom - paddingTop
        val centerX = width / 2
        val centerY = height / 2

        val bounds = drawable.bounds

        var w = drawable.intrinsicWidth
        var h = drawable.intrinsicHeight
        if (w <= 0) {
            w = bounds.right - bounds.left
        }
        if (h <= 0) {
            h = bounds.bottom - bounds.top
        }
        if (h > vh) {
            h = vh
        }
        if (w > vw) {
            w = vw
        }

        var left = 0
        var top = 0
        when (scaleType) {
            ScaleType.FIT_START -> {
                left = paddingLeft
                top = centerY - h / 2
            }
            ScaleType.FIT_END -> {
                left = vw - paddingRight - w
                top = centerY - h / 2
            }
            ScaleType.CENTER -> {
                left = centerX - w / 2
                top = centerY - h / 2
            }
            else -> {
                left = paddingLeft
                top = paddingTop
                w = vw
                h = vh
            }
        }

        return intArrayOf(left, top, left + w, top + h)
    }

    override fun reset() {

    }

    companion object {
        private const val TAG = "ScratchImageView"
    }
}