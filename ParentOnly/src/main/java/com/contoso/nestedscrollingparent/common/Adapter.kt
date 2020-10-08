package com.contoso.nestedscrollingparent.common

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.lang.reflect.Constructor

class Adapter(
    private val itemList: List<AdapterItem<out Any?>>,
    context: Context,
    private val viewHolders: SparseArray<Class<out BaseViewHolder<out Any?>>>
) : RecyclerView.Adapter<BaseViewHolder<out Any?>>() {

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): BaseViewHolder<out Any?> {
        val clazz = viewHolders[viewType]
        val annotation = clazz.getAnnotation(Holder::class.java)!!
        val itemView = inflater.inflate(annotation.layoutId, viewGroup, false)
        val viewHolder = clazz.getCtor().newInstance(itemView)
        viewHolder.initViews()
        return viewHolder
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(baseViewHolder: BaseViewHolder<out Any?>, position: Int) {
        (baseViewHolder as BaseViewHolder<Any?>).bindView(itemList[position].dataModel)
    }

    override fun getItemViewType(position: Int): Int {
        return itemList[position].viewType
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    private companion object {

        private val ctorCache = HashMap<Class<out BaseViewHolder<out Any?>>,
                Constructor<out BaseViewHolder<out Any?>>>()

        private fun Class<out BaseViewHolder<out Any?>>.getCtor() =
            ctorCache.getOrPut(this) { getConstructor(View::class.java) }
    }
}
