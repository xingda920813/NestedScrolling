package com.contoso.nestedscrollingparent.model

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView

class NestedViewModel : ViewModel() {

    val pagerHeight = MutableLiveData<Int?>()

    val innerViewContainer = MutableLiveData<View?>()

    val innerRecyclerView = MutableLiveData<RecyclerView?>()
}
