package com.contoso.nestedscrolling

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Before2 : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rv = RecyclerView(this)
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        setContentView(rv)
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(TextView(parent.context).apply {
                    textSize = 20F
                    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }) {}
            }

            @SuppressLint("SetTextI18n")
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val tv = holder.itemView as TextView
                tv.text = " #$position\n".repeat(60)
            }

            override fun getItemCount(): Int {
                return 2
            }
        }
    }
}
