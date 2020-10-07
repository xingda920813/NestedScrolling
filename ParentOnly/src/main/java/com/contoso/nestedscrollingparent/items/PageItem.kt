package com.contoso.nestedscrollingparent.items

import com.contoso.nestedscrollingparent.ViewType
import com.contoso.nestedscrollingparent.common.AdapterItem
import com.contoso.nestedscrollingparent.model.PageVO

data class PageItem(override val dataModel: List<PageVO>) : AdapterItem<List<PageVO>> {

    override val viewType = ViewType.TYPE_PAGER
}
