package com.contoso.nestedscrollingparent.common

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    abstract fun initViews()
    abstract fun bindView(data: T)
}
