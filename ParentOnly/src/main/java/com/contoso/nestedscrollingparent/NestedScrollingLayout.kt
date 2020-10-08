package com.contoso.nestedscrollingparent

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.contoso.nestedscrollingparent.model.NestedViewModel

class NestedScrollingLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), NestedScrollingParent2 {

    // The outer RecyclerView, contains 5 header pictures and the ViewPager
    private lateinit var outerRecyclerView: RecyclerView

    // Actually the ViewPager
    private var innerViewContainer: View? = null

    // The inner RecyclerView under a tab, contains 20 TextViews
    private var innerRecyclerView: RecyclerView? = null

    private val parentHelper = NestedScrollingParentHelper(this)

    init {
        isNestedScrollingEnabled = true
    }

    fun setTarget(target: LifecycleOwner?) {
        val scrollViewModel = when (target) {
            is FragmentActivity -> ViewModelProvider(target).get(NestedViewModel::class.java)
            is Fragment -> ViewModelProvider(target).get(NestedViewModel::class.java)
            else -> throw IllegalArgumentException("target must be FragmentActivity or Fragment")
        }
        scrollViewModel.innerViewContainer.observe(target) { innerViewContainer = it }
        scrollViewModel.innerRecyclerView.observe(target) { innerRecyclerView = it }
    }

    fun setOuterRecyclerView(recyclerView: RecyclerView) {
        outerRecyclerView = recyclerView
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) =
        parentHelper.onNestedScrollAccepted(child, target, axes)

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) =
        parentHelper.onNestedScrollAccepted(child, target, axes, type)

    override fun getNestedScrollAxes() = parentHelper.nestedScrollAxes

    override fun onStopNestedScroll(target: View) = parentHelper.onStopNestedScroll(target)

    override fun onStopNestedScroll(target: View, type: Int) =
        parentHelper.onStopNestedScroll(target, type)

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, type: Int
    ) = Unit

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        innerViewContainer ?: return
        innerRecyclerView ?: return
        if (target === outerRecyclerView) {
            val innerContainerTop = innerViewContainer!!.top
            if (innerContainerTop == 0) {
                // The ViewPager's top has reached the top of the screen
                if (innerRecyclerView!!.canScrollVertically(dy)) {
                    // If the inner RecyclerView can scroll, delegate to it and consume the dy
                    innerRecyclerView!!.scrollBy(0, dy)
                    consumed[1] = dy
                }
                // (else) otherwise, we don't consume the dy here and
                // leave it for the outer RecyclerView to consume
            } else if (innerContainerTop < dy) {
                // Consume the scrolling distance for the inner RecyclerView part and
                // leave the innerContainerTop for the outer RecyclerView to consume
                consumed[1] = dy - innerContainerTop
            }
        }
    }
}
