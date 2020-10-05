package com.contoso.nestedscrolling

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewConfiguration
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class NestedScrollingRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr), NestedScrollingParent2 {

    private val scrollingParentHelper = NestedScrollingParentHelper(this)
    private val touchSlop: Int
    private val maximumVelocity: Int

    private val location = IntArray(2)
    private val childLocation = IntArray(2)
    private val webViews = HashSet<NestedScrollingTextView>()

    init {
        val viewConfig = ViewConfiguration.get(context)
        touchSlop = viewConfig.scaledTouchSlop
        maximumVelocity = viewConfig.scaledMaximumFlingVelocity
        isNestedScrollingEnabled = true
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return if (consumed != null) {
            val handled =  super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
            return dispatchScrollToWebView(dx, dy, consumed) || handled
        } else {
            super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
        }
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?, type: Int): Boolean {
        return if (consumed != null) {
            val handled = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
            return dispatchScrollToWebView(dx, dy, consumed) || handled
        } else {
            super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
        }
    }

    private fun dispatchScrollToWebView(dx: Int, dy: Int, consumed: IntArray): Boolean {
        if (dy == 0 && dx == 0) {
            return false
        }
        val webView = findTargetWebView(dy) ?: return false
        this.getLocationInWindow(location)
        webView.getLocationInWindow(childLocation)

        val distance = childLocation[1] - location[1]
        // Only scroll WebView vertically when the distance is smaller than scroll value,
        if (abs(distance) < abs(dy)) {
            return webView.consumeScroll(dx, dy - distance, consumed)
        }
        return webView.consumeScroll(dx, 0, consumed)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

    override fun getNestedScrollAxes(): Int {
        return scrollingParentHelper.nestedScrollAxes
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        scrollingParentHelper.onNestedScrollAccepted(child, target, axes, type)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        scrollingParentHelper.onStopNestedScroll(target, type)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        consumed[0] = 0
        consumed[1] = 0
        if (target is NestedScrollingTextView) {
            this.getLocationInWindow(location)
            target.getLocationInWindow(childLocation)
            if (dy > 0) {
                // Scroll to bottom
                val bottom = location[1] + height
                val childBottom = childLocation[1] + target.height
                if (childBottom > bottom) {
                    consumed[1] = minOf(childBottom - bottom, dy)
                }
            } else if (dy < 0) {
                // Scroll to top
                if (childLocation[1] < location[1]) {
                    consumed[1] = maxOf(childLocation[1] - location[1], dy)
                }
            }
            if (consumed[1] != 0) {
                scrollBy(0, consumed[1])
            }
        }
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        if (dyUnconsumed != 0 || dxUnconsumed != 0) {
            scrollBy(dxUnconsumed, dyUnconsumed)
        }
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        if (target is NestedScrollingTextView) {
            fling(velocityX.toInt(), velocityY.toInt())
            return true
        }
        return false
    }

    override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        webViews.forEach { it.adjustHeight() }
    }

    fun onWebViewDetached(webView: NestedScrollingTextView) {
        webViews.remove(webView)
    }

    fun onWebViewAttached(webView: NestedScrollingTextView) {
        webViews.add(webView)
    }

    /**
     * Find the target WebView we depends on when scrolling
     */
    private fun findTargetWebView(dy: Int): NestedScrollingTextView? {
        if (webViews.isEmpty()) {
            return null
        }
        return if (dy > 0) {
            webViews.maxByOrNull {
                it.getLocationInWindow(childLocation)
                childLocation[1]
            }
        } else {
            webViews.minByOrNull {
                it.getLocationInWindow(childLocation)
                childLocation[1]
            }
        }
    }
}