package com.contoso.nestedscrolling

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.NestedScrollingChild2
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.abs

class NestedScrollingTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr), NestedScrollingChild2 {

    private val childHelper = NestedScrollingChildHelper(this)
    private val scrollConsumed = IntArray(2)
    private val touchSlop: Int
    private val maximumFlingVelocity: Int
    private var velocityTracker: VelocityTracker? = null

    private var downY = 0
    private var lastY = 0
    private var maxScrollY = 0

    private var scrollParent: NestedScrollingRecyclerView? = null
    private val parentLocation = IntArray(2)
    private val selfLocation = IntArray(2)
    private var isScrollingVertically = false
    private var renderingHeight = 0

    private val paint: Paint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            textSize = context.resources.displayMetrics.density * 14
        }
    }

    init {
        val viewConfig = ViewConfiguration.get(context)
        touchSlop = viewConfig.scaledTouchSlop
        maximumFlingVelocity = viewConfig.scaledMaximumFlingVelocity
        isNestedScrollingEnabled = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action: Int = event.actionMasked
        val vtev = MotionEvent.obtain(event)
        vtev.setLocation(event.rawX, event.rawY)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.rawY.toInt()
                lastY = downY
                isScrollingVertically = false
                initOrResetVelocityTracker()
                velocityTracker?.addMovement(vtev)
                maxScrollY = computeVerticalScrollRange() - height
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                initVelocityTrackerIfNotExists()
                velocityTracker?.addMovement(vtev)
                val y = event.rawY.toInt()

                if (!isScrollingVertically && abs(y - downY) > touchSlop) {
                    // Start scrolling, send ACTION_CANCEL event, then TextView can dispose event handling
                    event.action = MotionEvent.ACTION_CANCEL
                    super.onTouchEvent(event)
                    isScrollingVertically = true
                }
                val dy: Int = if (isScrollingVertically) lastY - y else 0
                lastY = y
                if (dy != 0) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    childHelper.dispatchNestedPreScroll(
                        0,
                        dy,
                        scrollConsumed,
                        null,
                        ViewCompat.TYPE_TOUCH
                    )
                    val unconsumedY = dy - scrollConsumed[1]
                    scrollConsumed[1] = 0
                    consumeScroll(unconsumedY, scrollConsumed)
                    childHelper.dispatchNestedScroll(
                        0,
                        scrollConsumed[1],
                        0,
                        unconsumedY - scrollConsumed[1],
                        null,
                        ViewCompat.TYPE_TOUCH,
                        null
                    )
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isScrollingVertically) {
                    velocityTracker?.let { vt ->
                        vt.addMovement(vtev)
                        vt.computeCurrentVelocity(1000, maximumFlingVelocity.toFloat())
                        flingScroll(-vt.yVelocity.toInt())
                    }
                }
                recycleVelocityTracker()
            }
        }
        vtev.recycle()
        if (!isScrollingVertically) {
            super.onTouchEvent(event)
        }
        return true
    }

    private fun flingScroll(vy: Int) {
        if (DEBUG) {
            Log.d(TAG, "flingScroll: vy=$vy")
        }
        // Delegate fling to recycler view, this can use the same fling affect as recycler view
        if (vy != 0) {
            scrollParent?.fling(0, vy)
        }
        invalidate()
    }

    override fun scrollTo(x: Int, y: Int) {
        var toY = y
        if (toY < 0) {
            toY = 0
        }
        if (maxScrollY != 0 && toY > maxScrollY) {
            toY = maxScrollY
        }
        super.scrollTo(x, toY)
    }

    private fun initOrResetVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        } else {
            velocityTracker?.clear()
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker?.recycle()
            velocityTracker = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        var parent = parent as? ViewGroup
        while (parent != null) {
            if (parent is NestedScrollingRecyclerView) {
                scrollParent = parent
                break
            } else {
                parent = parent.parent as? ViewGroup
            }
        }
        scrollParent!!.onTextViewAttached(this)
        adjustHeight()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        childHelper.onDetachedFromWindow()
        recycleVelocityTracker()
        scrollParent?.onTextViewDetached(this)
        scrollParent = null
    }

    override fun setHeight(height: Int) {
        renderingHeight = height
        val params = layoutParams
        val parent = scrollParent
        if (params != null && parent != null) {
            super.setHeight(minOf(height, parent.measuredHeight))
        } else {
            super.setHeight(height)
        }
    }

    fun adjustHeight() {
        if (renderingHeight > 0) {
            height = renderingHeight
        }
    }

    fun consumeScroll(unconsumedY: Int, consumed: IntArray): Boolean {
        val oldScrollY = scrollY
        if (canScrollVertically(unconsumedY)) {
            if (unconsumedY != 0) {
                scrollBy(0, unconsumedY)
            }

            if (DEBUG) {
                Log.d(TAG, "consumeDy: unconsumed=$unconsumedY, consumed=${scrollY - oldScrollY}")
            }
        }
        consumed[1] += scrollY - oldScrollY
        return scrollY - oldScrollY != 0
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean = childHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int): Boolean =
        childHelper.startNestedScroll(axes)

    override fun stopNestedScroll() = childHelper.stopNestedScroll()

    override fun hasNestedScrollingParent(): Boolean =
        childHelper.hasNestedScrollingParent()

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return childHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed,
            offsetInWindow, type
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return childHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return childHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun stopNestedScroll(type: Int) {
        childHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return childHelper.hasNestedScrollingParent(type)
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return childHelper.startNestedScroll(axes, type)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (DEBUG) {
            canvas.save()
            canvas.translate(0F, scrollY.toFloat())
            val parent = scrollParent!!
            parent.getLocationInWindow(parentLocation)
            this.getLocationInWindow(selfLocation)
            val textY = context.resources.displayMetrics.density * 14
            canvas.drawText(
                "scrollRange: ${computeVerticalScrollRange()}, viewHeight: ${canvas.height}, renderingHeight=${renderingHeight}",
                0f,
                textY,
                paint
            )
            canvas.drawText(
                "scrollY = $scrollY, maxScrollY=${computeVerticalScrollRange() - height}",
                0f,
                textY + textY + 12,
                paint
            )
            canvas.restore()
        }
    }

    companion object {
        private const val TAG = "NestedScrollingTextView"
        private const val DEBUG = true
    }
}
