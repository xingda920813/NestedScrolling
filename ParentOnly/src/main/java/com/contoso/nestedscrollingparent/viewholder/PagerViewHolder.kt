package com.contoso.nestedscrollingparent.viewholder

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.contoso.nestedscrollingparent.R
import com.contoso.nestedscrollingparent.SubPagerAdapter
import com.contoso.nestedscrollingparent.common.BaseViewHolder
import com.contoso.nestedscrollingparent.common.Holder
import com.contoso.nestedscrollingparent.model.NestedViewModel
import com.contoso.nestedscrollingparent.model.PageVO
import com.google.android.material.tabs.TabLayout

@Holder(layoutId = R.layout.item_pager)
class PagerViewHolder(itemView: View) : BaseViewHolder<List<PageVO>>(itemView) {

    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private var pagerAdapter: PagerAdapter? = null
    private var models: List<PageVO>? = null
    private var viewModel: NestedViewModel? = null

    private val observer = Observer<Int?> { height ->
        if (height != null) {
            itemView.layoutParams.height = height
            itemView.requestLayout()
        }
    }

    override fun initViews() {
        viewPager = itemView.findViewById(R.id.viewpager)
        tabLayout = itemView.findViewById(R.id.tablayout)
        tabLayout.setupWithViewPager(viewPager, true)
        viewPager.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                viewPager.requestLayout()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
        itemView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                viewModel?.innerViewContainer?.value = itemView
            }

            override fun onViewDetachedFromWindow(view: View) {
                viewModel?.innerViewContainer?.value = null
            }
        })
    }

    override fun bindView(data: List<PageVO>) {
        if (models === data) {
            return
        }
        models = data
        val context = itemView.context
        if (context is FragmentActivity) {
            pagerAdapter = SubPagerAdapter(context.supportFragmentManager, models!!)
            viewPager.adapter = pagerAdapter
            pagerAdapter!!.notifyDataSetChanged()
            viewModel = ViewModelProvider(context).get(NestedViewModel::class.java)
            viewModel!!.pagerHeight.removeObserver(observer)
            viewModel!!.pagerHeight.observe(context, observer)
            val pagerHeight = viewModel!!.pagerHeight.value
            if (pagerHeight != null) {
                itemView.layoutParams.height = pagerHeight
            }
        }
    }
}
