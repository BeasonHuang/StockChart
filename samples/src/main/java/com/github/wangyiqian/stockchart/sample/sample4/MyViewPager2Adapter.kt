package com.github.wangyiqian.stockchart.sample.sample4

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.wangyiqian.stockchart.sample.R

class MyViewPager2Adapter(
    private val itemData: List<String>,
    private val TabTitle: Array<String>,
    private val context: Context
) :
    RecyclerView.Adapter<MyViewPager2Adapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.view_pager2_recycler_view, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.recyclerView.adapter = MyRecyclerViewAdapter(itemData)
        holder.recyclerView.layoutManager = LinearLayoutManager(context)
    }

    override fun getItemCount(): Int {
        return TabTitle.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var recyclerView: RecyclerView = itemView.findViewById<RecyclerView>(R.id.recyclerView)
    }
}