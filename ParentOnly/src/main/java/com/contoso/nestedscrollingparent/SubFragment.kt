package com.contoso.nestedscrollingparent

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.contoso.nestedscrollingparent.common.Adapter
import com.contoso.nestedscrollingparent.common.AdapterItem
import com.contoso.nestedscrollingparent.common.BaseViewHolder
import com.contoso.nestedscrollingparent.items.TextItem
import com.contoso.nestedscrollingparent.model.NestedViewModel
import com.contoso.nestedscrollingparent.viewholder.TextViewHolder
import java.util.*

class SubFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerView.Adapter<BaseViewHolder<out Any?>>
    private var viewModel: NestedViewModel? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.goods_list, container, false)
        init(view)
        return view
    }

    private fun init(view: View) {
        val color = arguments!!.getInt("color")
        recyclerView = view.findViewById(R.id.recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.setBackgroundColor(color)
        val viewHolders = SparseArray<Class<out BaseViewHolder<out Any?>>>()
        viewHolders.put(ViewType.TYPE_TEXT, TextViewHolder::class.java)
        val itemList: MutableList<AdapterItem<out Any?>> = ArrayList()
        for (i in 0..19) {
            itemList.add(TextItem("text$i"))
        }
        adapter = Adapter(itemList, view.context, viewHolders)
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        initViewModel()
        if (trackFragment() && viewModel != null) {
            viewModel!!.innerRecyclerView.value = recyclerView
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        initViewModel()
        if (!hidden && trackFragment() && viewModel != null) {
            viewModel!!.innerRecyclerView.value = recyclerView
        }
    }

    private fun initViewModel() {
        if (viewModel == null && activity != null) {
            viewModel = ViewModelProvider(activity!!).get(NestedViewModel::class.java)
        }
    }

    private fun trackFragment(): Boolean {
        if (view == null || view!!.parent !is View) {
            return false
        }
        val parent = view!!.parent as View
        if (parent is ViewPager) {
            val currentItem = parent.currentItem
            val position = if (arguments != null) {
                arguments!!.getInt("position", -1)
            } else {
                -1
            }
            return currentItem == position
        }
        return false
    }
}
