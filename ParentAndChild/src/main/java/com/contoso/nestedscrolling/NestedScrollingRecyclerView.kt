package com.contoso.nestedscrolling

import android.content.Context
import android.util.AttributeSet
import android.view.View
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

    private val parentHelper = NestedScrollingParentHelper(this)

    private val location = IntArray(2)
    private val childLocation = IntArray(2)
    private val textViews = HashSet<NestedScrollingTextView>()

    init {
        isNestedScrollingEnabled = true
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ) = dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH)

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ) = if (consumed != null) {
        val handled = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
        dispatchScrollToTextView(dy, consumed) || handled
    } else {
        super.dispatchNestedPreScroll(dx, dy, null, offsetInWindow, type)
    }

    private fun dispatchScrollToTextView(dy: Int, consumed: IntArray): Boolean {
        if (dy == 0) {
            return false
        }
        val textView = findTargetTextView(dy) ?: return false
        this.getLocationInWindow(location)
        textView.getLocationInWindow(childLocation)

        val distance = childLocation[1] - location[1]
        // Only scroll TextView vertically when the distance is smaller than scroll value,
        if (abs(distance) < abs(dy)) {
            return textView.consumeScroll(dy - distance, consumed)
        }
        return textView.consumeScroll(0, consumed)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

    override fun getNestedScrollAxes(): Int {
        return parentHelper.nestedScrollAxes
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        parentHelper.onNestedScrollAccepted(child, target, axes, type)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        parentHelper.onStopNestedScroll(target, type)
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

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
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

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ) = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        textViews.forEach { it.adjustHeight() }
    }

    fun onTextViewDetached(textView: NestedScrollingTextView) {
        textViews.remove(textView)
    }

    fun onTextViewAttached(textView: NestedScrollingTextView) {
        textViews.add(textView)
    }

    /**
     * Find the target TextView we depends on when scrolling
     */
    private fun findTargetTextView(dy: Int): NestedScrollingTextView? {
        if (textViews.isEmpty()) {
            return null
        }
        return if (dy > 0) {
            textViews.maxByOrNull {
                it.getLocationInWindow(childLocation)
                childLocation[1]
            }
        } else {
            textViews.minByOrNull {
                it.getLocationInWindow(childLocation)
                childLocation[1]
            }
        }
    }
}
