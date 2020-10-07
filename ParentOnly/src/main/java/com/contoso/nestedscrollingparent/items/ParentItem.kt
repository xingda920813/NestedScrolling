package com.contoso.nestedscrollingparent.items

import android.graphics.Bitmap
import com.contoso.nestedscrollingparent.ViewType
import com.contoso.nestedscrollingparent.common.AdapterItem

data class ParentItem(override val dataModel: Bitmap) : AdapterItem<Bitmap> {

    override val viewType = ViewType.TYPE_PARENT
}
