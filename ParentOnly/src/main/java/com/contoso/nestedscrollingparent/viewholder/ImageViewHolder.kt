package com.contoso.nestedscrollingparent.viewholder

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import com.contoso.nestedscrollingparent.R
import com.contoso.nestedscrollingparent.common.BaseViewHolder
import com.contoso.nestedscrollingparent.common.Holder

@Holder(layoutId = R.layout.item_parent_holder)
class ImageViewHolder(itemView: View) : BaseViewHolder<Bitmap>(itemView) {

    private lateinit var imageView: ImageView
    private var bitmap: Bitmap? = null

    override fun initViews() {
        imageView = itemView.findViewById(R.id.imageview)
    }

    override fun bindView(data: Bitmap) {
        if (bitmap == data) {
            return
        }
        bitmap = data
        imageView.setImageBitmap(bitmap)
    }
}
