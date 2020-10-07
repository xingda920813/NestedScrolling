package com.contoso.nestedscrollingparent.items

import com.contoso.nestedscrollingparent.ViewType
import com.contoso.nestedscrollingparent.common.AdapterItem

data class TextItem(override val dataModel: String) : AdapterItem<String> {

    override val viewType = ViewType.TYPE_TEXT
}
