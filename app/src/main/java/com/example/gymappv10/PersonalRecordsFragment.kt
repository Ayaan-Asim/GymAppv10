package com.example.gymappv10

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class PersonalRecordsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PRAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_personal_records, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerViewPRs)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PRAdapter()
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            val db = WorkoutDatabase.getInstance(requireContext())
            val prList = db.entryDao().getPersonalRecords()
            val exerciseMap = db.exerciseDao().getAll().associateBy { it.id }

            val displayList = prList.mapNotNull { pr ->
                val name = exerciseMap[pr.exerciseId]?.name ?: return@mapNotNull null
                "$name: ${pr.maxReps} reps"
            }

            adapter.submitList(displayList)
        }
    }
}
