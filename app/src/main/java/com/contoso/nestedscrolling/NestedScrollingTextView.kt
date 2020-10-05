/*
 * Copyright (c) 2020.
 * Microsoft Corporation. All rights reserved.
 */

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

    private val scrollingChildHelper by lazy { NestedScrollingChildHelper(this) }
    private val scrollConsumed = IntArray(2)
    private val touchSlop: Int
    private val maximumFlingVelocity: Int
    private var velocityTracker: VelocityTracker? = null

    private var downX = 0
    private var lastX = 0
    private var downY = 0
    private var lastY = 0
    private var maxScrollY = 0
    private var maxScrollX = 0

    private var scrollParent: NestedScrollingRecyclerView? = null
    private val parentLocation = IntArray(2)
    private val selfLocation = IntArray(2)
    private var isScrollingVertically = false
    private var isScrollingHorizontally = false
    private var isScrollDisabled = false
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
                downX = event.rawX.toInt()
                lastX = downX
                isScrollingVertically = false
                isScrollingHorizontally = false
                isScrollDisabled = false
                initOrResetVelocityTracker()
                velocityTracker?.addMovement(vtev)
                maxScrollX = computeHorizontalScrollRange() - width
                maxScrollY = computeVerticalScrollRange() - height
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // User is doing scaling
                isScrollDisabled = true
                isScrollingVertically = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (isScrollDisabled) {
                    return super.onTouchEvent(event)
                }
                initVelocityTrackerIfNotExists()
                velocityTracker?.addMovement(vtev)
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()

                if (!isScrollingVertically && abs(y - downY) > touchSlop) {
                    if (!isScrollingHorizontally) {
                        // Start scrolling, send ACTION_CANCEL event, then WebView can dispose event handling
                        event.action = MotionEvent.ACTION_CANCEL
                        super.onTouchEvent(event)
                    }
                    isScrollingVertically = true
                }
                if (!isScrollingHorizontally && abs(x - downX) > touchSlop) {
                    if (!isScrollingVertically) {
                        // Start scrolling, send ACTION_CANCEL event, then WebView can dispose event handling
                        event.action = MotionEvent.ACTION_CANCEL
                        super.onTouchEvent(event)
                    }
                    isScrollingHorizontally = true
                }
                val dx: Int = if (isScrollingHorizontally) lastX - x else 0
                val dy: Int = if (isScrollingVertically) lastY - y else 0
                lastY = y
                lastX = x
                if (dy != 0 || dx != 0) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    scrollingChildHelper.dispatchNestedPreScroll(dx, dy, scrollConsumed, null, ViewCompat.TYPE_TOUCH)
                    val unconsumedX = dx - scrollConsumed[0]
                    val unconsumedY = dy - scrollConsumed[1]
                    scrollConsumed[0] = 0
                    scrollConsumed[1] = 0
                    consumeScroll(unconsumedX, unconsumedY, scrollConsumed)
                    scrollingChildHelper.dispatchNestedScroll(scrollConsumed[0], scrollConsumed[1], unconsumedX - scrollConsumed[0], unconsumedY - scrollConsumed[1], null, ViewCompat.TYPE_TOUCH, null)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                recycleVelocityTracker()
            }
            MotionEvent.ACTION_UP -> {
                if ((isScrollingHorizontally || isScrollingVertically) && !isScrollDisabled) {
                    velocityTracker?.let { vt ->
                        vt.addMovement(vtev)
                        vt.computeCurrentVelocity(1000, maximumFlingVelocity.toFloat())
                        flingScroll((-vt.xVelocity).toInt(), -vt.yVelocity.toInt())
                    }
                }
                recycleVelocityTracker()
            }
        }
        vtev.recycle()
        if (!isScrollingHorizontally && !isScrollingVertically) {
            super.onTouchEvent(event)
        }
        return true
    }

    private fun flingScroll(vx: Int, vy: Int) {
        if (DEBUG) {
            Log.d(TAG, "flingScroll: vx=$vx, vy=$vy")
        }
        // Delegate fling to recycler view, this can use the same fling affect as recycler view
        if (vy != 0) {
            scrollParent?.fling(0, vy)
        }
        invalidate()
    }

    override fun scrollTo(x: Int, y: Int) {
        var toX = x
        var toY = y

        if (toX < 0) {
            toX = 0
        }
        if (toY < 0) {
            toY = 0
        }

        if (maxScrollX != 0 && toX > maxScrollX) {
            toX = maxScrollX
        }
        if (maxScrollY != 0 && toY > maxScrollY) {
            toY = maxScrollY
        }

        super.scrollTo(toX, toY)
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
        check(scrollParent != null) { "Couldn't find NestedScrollingRecyclerView" }
        scrollParent?.let { scrollParent ->
            scrollParent.onWebViewAttached(this)
            adjustHeight()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scrollingChildHelper.onDetachedFromWindow()
        recycleVelocityTracker()
        scrollParent?.onWebViewDetached(this)
        scrollParent = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        renderingHeight = measuredHeight
        val parent = scrollParent
        if (parent != null) {
            setMeasuredDimension(measuredWidth, minOf(measuredHeight, parent.measuredHeight))
        }
    }

    fun adjustHeight() {
        if (renderingHeight > 0) {
            height = renderingHeight
        }
    }

    fun consumeScroll(unconsumedX: Int, unconsumedY: Int, consumed: IntArray): Boolean {
        val oldScrollX = scrollX
        val oldScrollY = scrollY
        if (!canScrollVertically(unconsumedY)) {
            if (unconsumedX != 0) {
                scrollBy(unconsumedX, 0)
            }
        } else {
            if (unconsumedX != 0 || unconsumedY != 0) {
                scrollBy(unconsumedX, unconsumedY)
            }

            if (DEBUG) {
                Log.d(TAG, "consumeDy: unconsumed=$unconsumedY, consumed=${scrollY - oldScrollY}")
            }
        }
        consumed[0] += scrollX - oldScrollX
        consumed[1] += scrollY - oldScrollY
        return scrollX - oldScrollX != 0 || scrollY - oldScrollY != 0
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        scrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean = scrollingChildHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int): Boolean = scrollingChildHelper.startNestedScroll(axes)

    override fun stopNestedScroll() = scrollingChildHelper.stopNestedScroll()

    override fun hasNestedScrollingParent(): Boolean = scrollingChildHelper.hasNestedScrollingParent()

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return scrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed,
            offsetInWindow, type
        )
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return scrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?, type: Int): Boolean {
        return scrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return scrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return scrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun stopNestedScroll(type: Int) {
        scrollingChildHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return scrollingChildHelper.hasNestedScrollingParent(type)
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return scrollingChildHelper.startNestedScroll(axes, type)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (DEBUG) {
            canvas.save()
            canvas.translate(0F, scrollY.toFloat())
            val parent = scrollParent ?: throw IllegalStateException("No scroll parent")
            parent.getLocationInWindow(parentLocation)
            this.getLocationInWindow(selfLocation)
            val textY = context.resources.displayMetrics.density * 14
            canvas.drawText(
                "scrollRange: ${computeVerticalScrollRange()}, viewHeight: ${canvas.height}, renderingHeight=${renderingHeight}",
                0f,
                textY,
                paint
            )
            canvas.drawText("scrollX=$scrollX, scrollY = $scrollY, maxScrollY=${computeVerticalScrollRange() - height}", 0f, textY + textY + 12, paint)
            canvas.restore()
        }
    }

    companion object {
        private const val TAG = "NestedScrollingTextView"
        private const val DEBUG = false
    }
}
