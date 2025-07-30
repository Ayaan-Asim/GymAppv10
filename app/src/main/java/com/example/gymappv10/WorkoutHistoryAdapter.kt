package com.example.gymappv10

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WorkoutHistoryAdapter(private val items: List<WorkoutHistoryItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_SESSION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is WorkoutHistoryItem.DateHeader -> TYPE_DATE
            is WorkoutHistoryItem.SessionDetail -> TYPE_SESSION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATE -> DateViewHolder(inflater.inflate(R.layout.item_date_header, parent, false))
            else -> SessionViewHolder(inflater.inflate(R.layout.item_session_detail, parent, false))
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is WorkoutHistoryItem.DateHeader -> (holder as DateViewHolder).bind(item)
            is WorkoutHistoryItem.SessionDetail -> (holder as SessionViewHolder).bind(item)
        }
    }

    class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDate: TextView = view.findViewById(R.id.tvDateHeader)
        fun bind(item: WorkoutHistoryItem.DateHeader) {
            tvDate.text = item.date
        }
    }

    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSummary: TextView = view.findViewById(R.id.tvSessionSummary)
        fun bind(item: WorkoutHistoryItem.SessionDetail) {
            val summary = StringBuilder()
                .append("🕒 Duration: ${item.session.totalDuration} min\n")
                .append("🔥 Calories Burned: ${item.session.totalCalories} kcal\n")
                .append("\nExercises:\n")

            item.entries.forEachIndexed { index, entry ->
                summary.append("${index + 1}. ${entry.exerciseId}: ")
                summary.append("${entry.durationMin} min")
                if (entry.reps != null) summary.append(", ${entry.reps} reps")
                summary.append(", ${entry.calories} kcal\n")
            }

            tvSummary.text = summary.toString().trim()
        }
    }

}
