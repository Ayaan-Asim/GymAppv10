package com.example.gymappv10

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PRAdapter : RecyclerView.Adapter<PRAdapter.PRViewHolder>() {

    private val list = mutableListOf<String>()

    fun submitList(newList: List<String>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    class PRViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tvPRText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PRViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pr, parent, false)
        return PRViewHolder(view)
    }

    override fun onBindViewHolder(holder: PRViewHolder, position: Int) {
        holder.text.text = list[position]
    }

    override fun getItemCount() = list.size
}
