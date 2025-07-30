package com.example.gymappv10

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WorkoutLogBottomSheet : BottomSheetDialogFragment() {

    interface WorkoutLogListener {
        fun onWorkoutLogged()
    }

    var listener: WorkoutLogListener? = null

    private lateinit var autoCompleteTextView: MaterialAutoCompleteTextView
    private lateinit var durationInput: EditText
    private lateinit var repsInput: EditText
    private lateinit var addButton: Button
    private lateinit var doneButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WorkoutEntryAdapter

    private val tempEntries = mutableListOf<WorkoutEntry>()
    private val entryDisplayList = mutableListOf<String>()
    private lateinit var exerciseMap: Map<String, Exercise>
    private var userWeight = 70f // Hardcoded for now

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val todayDate = dateFormat.format(Date())
    private val currentSessionId = UUID.randomUUID().toString() // ✅ single session ID for all entries

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_workout_log, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        autoCompleteTextView = view.findViewById(R.id.exerciseInput)
        durationInput = view.findViewById(R.id.etDuration)
        repsInput = view.findViewById(R.id.etReps)
        addButton = view.findViewById(R.id.btnAddEntry)
        doneButton = view.findViewById(R.id.btnDone)
        recyclerView = view.findViewById(R.id.recyclerViewEntries)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = WorkoutEntryAdapter(entryDisplayList)
        recyclerView.adapter = adapter

        val db = WorkoutDatabase.getInstance(requireContext())

        lifecycleScope.launch {
            val existing = db.exerciseDao().getAll()
            if (existing.isEmpty()) {
                val defaultExercises = listOf(
                    Exercise(id = "pushups", name = "Pushups", met = 8.0, hasReps = true),
                    Exercise(id = "cycling", name = "Cycling", met = 6.0, hasReps = false),
                    Exercise(id = "treadmill", name = "Treadmill", met = 7.5, hasReps = false),
                    Exercise(id = "jumprope", name = "Jump Rope", met = 10.0, hasReps = false),
                    Exercise(id = "squats", name = "Squats", met = 5.5, hasReps = true)
                )
                db.exerciseDao().insertAll(defaultExercises)
            }

            val exercises = db.exerciseDao().getAll()
            exerciseMap = exercises.associateBy { it.name }
            val names = exercises.map { it.name }

            val autoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
            autoCompleteTextView.setAdapter(autoAdapter)
            autoCompleteTextView.threshold = 1
            autoCompleteTextView.setOnClickListener { autoCompleteTextView.showDropDown() }
            autoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) autoCompleteTextView.showDropDown()
            }
        }

        addButton.setOnClickListener {
            val name = autoCompleteTextView.text.toString()
            val duration = durationInput.text.toString().toIntOrNull()
            val reps = repsInput.text.toString().toIntOrNull()
            val exercise = exerciseMap[name]

            if (exercise == null || duration == null || duration <= 0 || (exercise.hasReps && (reps == null || reps <= 0))) {
                Toast.makeText(requireContext(), "Fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val calories = ((exercise.met * 3.5 * userWeight) / 200.0 * duration).toInt()

            val entry = WorkoutEntry(
                sessionId = currentSessionId,
                exerciseId = exercise.id,
                durationMin = duration,
                reps = if (exercise.hasReps) reps else null,
                calories = calories
            )

            tempEntries.add(entry)

            val display = if (exercise.hasReps)
                "${exercise.name}: ${duration}min, $reps reps, $calories kcal"
            else
                "${exercise.name}: ${duration}min, $calories kcal"

            entryDisplayList.add(display)
            adapter.notifyItemInserted(entryDisplayList.size - 1)

            autoCompleteTextView.text.clear()
            durationInput.text.clear()
            repsInput.text.clear()
        }

        doneButton.setOnClickListener {
            if (tempEntries.isEmpty()) {
                dismiss()
                return@setOnClickListener
            }

            val totalCals = tempEntries.sumOf { it.calories }
            val totalMin = tempEntries.sumOf { it.durationMin }
            val session = WorkoutSession(
                sessionId = currentSessionId,
                date = todayDate,
                totalDuration = totalMin,
                totalCalories = totalCals,
                timestamp = System.currentTimeMillis()
            )

            lifecycleScope.launch {
                db.sessionDao().insert(session)
                db.entryDao().insertAll(tempEntries)
                listener?.onWorkoutLogged()
                dismiss()
                Toast.makeText(requireContext(), "Workout logged!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class WorkoutEntryAdapter(private val list: List<String>) : RecyclerView.Adapter<WorkoutEntryAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tv: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tv.text = list[position]
        }

        override fun getItemCount() = list.size
    }
}
