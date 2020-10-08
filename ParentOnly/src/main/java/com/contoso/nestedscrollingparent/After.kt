package com.contoso.nestedscrollingparent

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.SparseArray
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.contoso.nestedscrollingparent.common.Adapter
import com.contoso.nestedscrollingparent.common.AdapterItem
import com.contoso.nestedscrollingparent.common.BaseViewHolder
import com.contoso.nestedscrollingparent.items.PageItem
import com.contoso.nestedscrollingparent.items.ParentItem
import com.contoso.nestedscrollingparent.model.NestedViewModel
import com.contoso.nestedscrollingparent.model.PageVO
import com.contoso.nestedscrollingparent.viewholder.ImageViewHolder
import com.contoso.nestedscrollingparent.viewholder.PagerViewHolder
import java.util.*

class After : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerView.Adapter<BaseViewHolder<out Any?>>
    private lateinit var viewModel: NestedViewModel
    private lateinit var container: NestedScrollingLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_after)
        container = findViewById(R.id.rootview)
        recyclerView = findViewById(R.id.recyclerview)
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.layoutManager = LinearLayoutManager(this)
        container.setOuterRecyclerView(recyclerView)
        container.setTarget(this)
        initAdapter()
        viewModel = ViewModelProvider(this).get(NestedViewModel::class.java)
        container.viewTreeObserver.addOnGlobalLayoutListener {
            val height = container.measuredHeight
            viewModel.pagerHeight.setValue(height)
        }
    }

    private fun initAdapter() {
        val viewHolders = SparseArray<Class<out BaseViewHolder<out Any?>>>()
        viewHolders.put(ViewType.TYPE_PARENT, ImageViewHolder::class.java)
        viewHolders.put(ViewType.TYPE_PAGER, PagerViewHolder::class.java)
        val ids =
            intArrayOf(R.mipmap.pic1, R.mipmap.pic2, R.mipmap.pic3, R.mipmap.pic4, R.mipmap.pic5)
        val itemList: MutableList<AdapterItem<out Any?>> = ArrayList()
        for (id in ids) {
            val bitmap = BitmapFactory.decodeResource(resources, id)
            itemList.add(ParentItem(bitmap))
        }
        val pageList: MutableList<PageVO> = ArrayList()
        for (i in 0..6) {
            pageList.add(PageVO(Color.WHITE, "tab$i"))
        }
        itemList.add(PageItem(pageList))
        adapter = Adapter(itemList, this, viewHolders)
        recyclerView.adapter = adapter
    }
}
