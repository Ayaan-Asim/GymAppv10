package com.example.gymappv10

import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProgressFragment : Fragment(), WorkoutLogBottomSheet.WorkoutLogListener {

    override fun onWorkoutLogged() {
        refreshWorkoutSummary()
    }
    private lateinit var tvDaysInGym : TextView
    private lateinit var tvTotalTime : TextView
    private lateinit var tvCaloriesBurnt : TextView
    private lateinit var caloriesChart: LineChart
    private lateinit var lineChart: LineChart
    private lateinit var btnAddWeight: Button
    private lateinit var rvWorkoutSummary : RecyclerView
    private lateinit var dao: WeightLogDao
    private lateinit var compactCalendarView: CompactCalendarView
    private lateinit var tvStreak: TextView
    private lateinit var monthName: TextView
    private lateinit var tvLongestStreak: TextView
    private val fullDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val chartLabelFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    private var todayWeightExists = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btnViewFullLog).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WorkoutHistoryFragment())
                .addToBackStack(null)
                .commit()
        }
        refreshWorkoutSummary()
        loadMonthlyCalories()
        loadGymStats()
        loadPersonalRecords()
        caloriesChart = view.findViewById(R.id.lineChartCalories)
        setupCaloriesChart()
        loadCaloriesPerDayChart()
        val sheet = WorkoutLogBottomSheet()
        sheet.listener = this
        view.findViewById<Button>(R.id.btnLogWorkout).setOnClickListener {
            sheet.show(parentFragmentManager, "logWorkout")
        }
        tvTotalTime = view.findViewById<TextView>(R.id.tvTotalTime)
        tvDaysInGym = view.findViewById<TextView>(R.id.tvDaysInGym)
        tvCaloriesBurnt = view.findViewById<TextView>(R.id.tvCaloriesBurnt)
        rvWorkoutSummary = view.findViewById(R.id.rvWorkoutSummary)
        rvWorkoutSummary.layoutManager = LinearLayoutManager(requireContext())
        monthName = view.findViewById(R.id.monthName)
        tvLongestStreak = view.findViewById(R.id.tvLongestStreak)
        tvStreak = view.findViewById(R.id.tvCurrentStreak)
        compactCalendarView = view.findViewById(R.id.compactcalendar_view)
        lineChart = view.findViewById(R.id.weightLineChart)
        btnAddWeight = view.findViewById(R.id.btnAddWeight)
        dao = AppDatabase.getDatabase(requireContext()).weightLogDao()

        // Calendar Styling
        compactCalendarView.apply {
            setCurrentDate(Date())
            setUseThreeLetterAbbreviation(true)
            setCurrentDayBackgroundColor(Color.parseColor("#1976D2")) // 🔵 Slightly different blue
            setCurrentSelectedDayBackgroundColor(Color.parseColor("#1976D2"))
            setEventIndicatorStyle(CompactCalendarView.FILL_LARGE_INDICATOR)
            setBackgroundColor(Color.WHITE)
        }

        setupChart()
        loadAndDisplayWeightLogs()
        loadAttendanceAndMarkCalendar()

        btnAddWeight.setOnClickListener {
            showWeightInputDialog()
        }

        compactCalendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
            override fun onDayClick(dateClicked: Date) {
                val dateStr = fullDateFormat.format(dateClicked)
                Toast.makeText(requireContext(), "Clicked: $dateStr", Toast.LENGTH_SHORT).show()
                compactCalendarView.setCurrentSelectedDayBackgroundColor(Color.parseColor("#1976D2"))
            }

            override fun onMonthScroll(firstDayOfNewMonth: Date) {}
        })
    }

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            setScaleEnabled(true)
            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                textSize = 10f
                labelRotationAngle = -45f
                setAvoidFirstLastClipping(true)
            }

            axisLeft.apply {
                textSize = 12f
                granularity = 1f
                axisMinimum = 0f
            }

            legend.isEnabled = false
        }
    }

    private fun loadAndDisplayWeightLogs() {
        lifecycleScope.launch {
            val logs = dao.getAll().sortedBy { fullDateFormat.parse(it.date) }
            if (logs.isEmpty()) return@launch

            val entries = ArrayList<Entry>()
            val dateLabels = ArrayList<String>()
            val today = fullDateFormat.format(Date())
            todayWeightExists = logs.any { it.date == today }

            logs.forEachIndexed { index, log ->
                val parsedDate = fullDateFormat.parse(log.date)
                entries.add(Entry(index.toFloat(), log.weight))
                dateLabels.add(chartLabelFormat.format(parsedDate!!))
            }

            val dataSet = LineDataSet(entries, "Weight (kg)").apply {
                color = Color.parseColor("#3F51B5")
                setCircleColor(Color.BLACK)
                lineWidth = 2.5f
                circleRadius = 4f
                valueTextSize = 10f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillDrawable = getGradientDrawable()
            }

            lineChart.data = LineData(dataSet)
            lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)

            if (entries.isNotEmpty()) {
                val min = entries.minOf { it.y }
                val max = entries.maxOf { it.y }
                val margin = 5f
                lineChart.axisLeft.axisMinimum = (min - margin).coerceAtLeast(0f)
                lineChart.axisLeft.axisMaximum = max + margin
            }

            lineChart.setVisibleXRangeMaximum(7f)
            lineChart.moveViewToX(entries.size.toFloat())
            lineChart.invalidate()

            btnAddWeight.text = if (todayWeightExists) "Edit Weight" else "Add Weight"
        }
    }

    private fun getGradientDrawable(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#3F51B5"), Color.TRANSPARENT)
        )
    }

    private fun showWeightInputDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (todayWeightExists) "Edit Today's Weight" else "Enter Weight (kg)")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val weight = input.text.toString().toFloatOrNull()
                if (weight != null) {
                    val today = fullDateFormat.format(Date())
                    val log = WeightLog(today, weight)
                    lifecycleScope.launch {
                        dao.insert(log)
                        loadAndDisplayWeightLogs()
                    }
                } else {
                    Toast.makeText(requireContext(), "Invalid weight", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadAttendanceAndMarkCalendar() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR).toString()
        val month = now.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())!!
        val weeks = listOf("week_1", "week_2", "week_3", "week_4", "week_5")

        compactCalendarView.removeAllEvents()

        val todayStr = fullDateFormat.format(now.time)
        val allDates = mutableListOf<String>()
        var todayWasPresent = false
        var pendingFetches = weeks.size

        for (week in weeks) {
            db.collection("attendance_records")
                .document(uid)
                .collection(year)
                .document(month)
                .collection(week)
                .get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        val dateStr = doc.id
                        if (dateStr == todayStr) {
                            todayWasPresent = true
                            continue
                        }

                        allDates.add(dateStr)
                        val parsedDate = try { fullDateFormat.parse(dateStr) } catch (_: Exception) { null }

                        parsedDate?.let {
                            val cal = Calendar.getInstance().apply {
                                time = it
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val millis = cal.timeInMillis
                            val event = Event(Color.parseColor("#3F51B5"), millis, "Present")
                            compactCalendarView.addEvent(event)
                        }
                    }

                    pendingFetches--
                    if (pendingFetches == 0) {
                        compactCalendarView.invalidate()
                        calculateAndSetStreak(allDates, todayWasPresent)
                    }
                }
        }
    }

    private fun calculateAndSetStreak(dates: List<String>, todayIncluded: Boolean) {
        val allDates = dates.toMutableList()

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (todayIncluded) {
            val todayStr = fullDateFormat.format(todayCal.time)
            allDates.add(todayStr)
        }

        val sorted = allDates.distinct().mapNotNull {
            try { fullDateFormat.parse(it) } catch (_: Exception) { null }
        }.sorted()

        var streak = 0
        val checkCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in sorted.size - 1 downTo 0) {
            val date = sorted[i]
            val dateStr = fullDateFormat.format(date)
            val expectedStr = fullDateFormat.format(checkCal.time)

            if (dateStr == expectedStr) {
                streak++
                checkCal.add(Calendar.DATE, -1)
            } else if (date.before(checkCal.time)) {
                break
            }
        }


        var maxRun = 1
        var currentRun = 1
        for (i in 1 until sorted.size) {
            val prevCal = Calendar.getInstance().apply {
                time = sorted[i - 1]
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val currCal = Calendar.getInstance().apply {
                time = sorted[i]
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            prevCal.add(Calendar.DATE, 1)
            if (fullDateFormat.format(prevCal.time) == fullDateFormat.format(currCal.time)) {
                currentRun++
            } else {
                currentRun = 1
            }

            if (currentRun > maxRun) {
                maxRun = currentRun
            }
        }

        tvStreak.text = "$streak"
        tvLongestStreak.text = "$maxRun"
    }

    fun refreshWorkoutSummary() {
        lifecycleScope.launch {
            val db = WorkoutDatabase.getInstance(requireContext())
            val last3 = db.sessionDao().getLastThreeDaysSummary()
            if (last3.isEmpty()) {
                return@launch
            }
            val adapter = WorkoutSummaryAdapter(last3)
            rvWorkoutSummary.adapter = adapter
        }
    }
    private fun setupCaloriesChart() {
        caloriesChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            legend.isEnabled = false
        }
    }
    private fun loadCaloriesPerDayChart() {
        val dateFormatIn = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateFormatOut = SimpleDateFormat("dd MMM", Locale.getDefault())

        lifecycleScope.launch {
            val db = WorkoutDatabase.getInstance(requireContext())
            val sessions = db.sessionDao().getAllSortedByDate()

            if (sessions.isEmpty()) {
                caloriesChart.clear()
                caloriesChart.setNoDataText("No workout data")
                return@launch
            }

            val entries = sessions.mapIndexed { index, session ->
                Entry(index.toFloat(), session.totalCalories.toFloat())
            }

            val labels = sessions.map { session ->
                try {
                    val date = dateFormatIn.parse(session.date)
                    dateFormatOut.format(date ?: Date())
                } catch (e: Exception) {
                    session.date
                }
            }

            val dataSet = LineDataSet(entries, "Calories Burned").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER
                color = Color.parseColor("#FF5722")
                setCircleColor(Color.parseColor("#FF5722"))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(false)
                setDrawFilled(true)
                fillDrawable = getGradientDrawableCalories()
            }


            caloriesChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            caloriesChart.data = LineData(dataSet)
            caloriesChart.invalidate()
        }
    }
    private fun getGradientDrawableCalories(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#FF5722"), Color.TRANSPARENT)
        )
    }
    private fun loadMonthlyCalories() {
        val db = WorkoutDatabase.getInstance(requireContext())
        val calendar = Calendar.getInstance()
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1) // 01 to 12
        val year = calendar.get(Calendar.YEAR).toString()

        lifecycleScope.launch {
            val totalCals = db.sessionDao().getTotalCaloriesThisMonth(month, year) ?: 0
            tvCaloriesBurnt.text = "$totalCals"
        }
    }
    private fun loadPersonalRecords() {
        lifecycleScope.launch {
            val db = WorkoutDatabase.getInstance(requireContext())
            val prList = db.entryDao().getPersonalRecords()
            val exerciseMap = db.exerciseDao().getAll().associateBy { it.id }

            val displayList = prList.mapNotNull { pr ->
                val name = exerciseMap[pr.exerciseId]?.name ?: return@mapNotNull null
                "$name: ${pr.maxReps} reps"
            }

            val container = requireView().findViewById<LinearLayout>(R.id.personalRecordsContainer)
            container.removeAllViews()

            displayList.take(4).forEach { record ->
                val tv = TextView(requireContext()).apply {
                    text = record
                    textSize = 16f
                    setPadding(16, 8, 16, 8)
                }
                container.addView(tv)
            }

            requireView().findViewById<Button>(R.id.btnViewAllPRs).setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, PersonalRecordsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
    private fun loadGymStats() {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString()
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
        val currentWeek = calendar.get(Calendar.WEEK_OF_MONTH)
        val totalTimeDocRef = db.collection("attendance_records")
            .document(uid).collection(year).document(month)

        totalTimeDocRef.get()
            .addOnSuccessListener { doc ->
                val totalMinutes = doc.getLong("totalTimeSpentThisMonth") ?: 0
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                tvTotalTime.text = "$hours hr $minutes min"
            }
            .addOnFailureListener {
                tvTotalTime.text = "0 hr 0 min"
            }
        var totalDays = 0
        val weekRefs = (1..currentWeek).map { weekNum ->
            db.collection("attendance_records")
                .document(uid).collection(year)
                .document(month).collection("week_$weekNum")
        }

        val tasks = weekRefs.map { it.get() }

        Tasks.whenAllSuccess<QuerySnapshot>(tasks)
            .addOnSuccessListener { weekSnapshots ->
                for (weekSnap in weekSnapshots) {
                    totalDays += weekSnap.documents.size
                }
                tvDaysInGym.text = "$totalDays"
            }
            .addOnFailureListener {
                tvDaysInGym.text = "0 days"
            }
    }

}
