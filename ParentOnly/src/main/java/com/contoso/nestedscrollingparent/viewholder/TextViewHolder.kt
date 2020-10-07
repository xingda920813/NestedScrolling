package com.contoso.nestedscrollingparent.viewholder

import android.view.View
import android.widget.TextView
import com.contoso.nestedscrollingparent.R
import com.contoso.nestedscrollingparent.common.BaseViewHolder
import com.contoso.nestedscrollingparent.common.Holder

@Holder(layoutId = R.layout.item_text)
class TextViewHolder(itemView: View) : BaseViewHolder<String>(itemView) {

    private lateinit var textView: TextView

    override fun initViews() {
        textView = itemView.findViewById(R.id.textview)
    }

    override fun bindView(data: String) {
        textView.text = data
    }
}
