package com.contoso.nestedscrolling

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class After2 : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rv = NestedScrollingRecyclerView(this)
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        setContentView(rv)
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                return object :
                    RecyclerView.ViewHolder(NestedScrollingTextView(parent.context).apply {
                        textSize = 20F
                        layoutParams = RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }) {}
            }

            @SuppressLint("SetTextI18n")
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val tv = holder.itemView as NestedScrollingTextView
                val sb = StringBuilder()
                for (i in 0 until 60) {
                    sb.append(" #").append(position).append(", L").append(i).append('\n')
                }
                tv.text = sb
            }

            override fun getItemCount(): Int {
                return 2
            }
        }
    }
}
