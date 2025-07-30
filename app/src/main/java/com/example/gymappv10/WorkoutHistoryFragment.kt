package com.example.gymappv10

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class WorkoutHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WorkoutHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_workout_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerViewWorkoutHistory)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            val db = WorkoutDatabase.getInstance(requireContext())
            val sessions = db.sessionDao().getAllSortedByDate()
            val grouped = sessions.groupBy { it.date }

            val finalList = mutableListOf<WorkoutHistoryItem>()
            for ((date, daySessions) in grouped) {
                finalList.add(WorkoutHistoryItem.DateHeader(date))
                for (session in daySessions) {
                    val entries = db.entryDao().getBySessionId(session.sessionId)
                    finalList.add(WorkoutHistoryItem.SessionDetail(session, entries))
                }
            }

            adapter = WorkoutHistoryAdapter(finalList)
            recyclerView.adapter = adapter
        }
    }
}
