package com.contoso.nestedscrollingparent.common

interface AdapterItem<T> {

    val dataModel: T
    val viewType: Int
}
