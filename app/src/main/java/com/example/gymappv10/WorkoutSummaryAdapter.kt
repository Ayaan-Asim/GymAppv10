package com.example.gymappv10

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WorkoutSummaryAdapter(private val summaries: List<DailyWorkoutSummary>) :
    RecyclerView.Adapter<WorkoutSummaryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val tvDate : TextView = view.findViewById<TextView>(R.id.tvDate)
        val tvDuration : TextView = view.findViewById<TextView>(R.id.tvDuration)
        val tvCalories : TextView = view.findViewById<TextView>(R.id.tvCalories)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout_summary,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = summaries[position]
        holder.tvDate.text = "• ${item.date}"
        holder.tvDuration.text = "Duration: ${item.totalDuration} min"
        holder.tvCalories.text = "Calories: ${item.totalCalories} kcal"
    }

    override fun getItemCount() = summaries.size

}