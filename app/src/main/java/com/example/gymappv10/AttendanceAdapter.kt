package com.example.gymappv10

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceAdapter(
    private val list: List<AttendanceRecord>
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.tvDate)
        val statusText: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_record, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val item = list[position]
        holder.dateText.text = item.date
        holder.statusText.text = when {
            item.checkInTime != null && item.checkOutTime != null -> {
                val dur = item.durationMinutes ?: 0
                "✅ ${item.checkInTime} - ${item.checkOutTime} | ${dur} min"
            }
            item.checkInTime != null -> "🟡 Checked in at ${item.checkInTime}"
            else -> "❌ Absent"
        }
    }

    override fun getItemCount() = list.size
}