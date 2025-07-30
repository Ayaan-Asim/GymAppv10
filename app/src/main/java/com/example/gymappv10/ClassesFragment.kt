package com.example.gymappv10

import ClassAdapter
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ClassesFragment : Fragment() {
    private lateinit var hotAdapter: ClassAdapter
    private lateinit var filteredAdapter: ClassAdapter
    private lateinit var hotClassesRecyclerView: RecyclerView
    private lateinit var filteredClassesRecyclerView: RecyclerView
    private lateinit var btnToday: Button
    private lateinit var btnTomorrow: Button
    private lateinit var btnThisWeek: Button
    private lateinit var btnNextWeek: Button

    private val db = FirebaseFirestore.getInstance()
    private val classList = mutableListOf<ClassModel>()
    private val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_classes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val searchView = view.findViewById<SearchView>(R.id.searchView)
        searchView.queryHint = "Search Class"
        val searchEditText = searchView.findViewById<EditText>(
            androidx.appcompat.R.id.search_src_text
        )
        searchEditText.setHintTextColor(Color.GRAY)
        searchEditText.setTextColor(Color.BLACK)

        hotClassesRecyclerView = view.findViewById(R.id.hotClassesRecyclerView)
        filteredClassesRecyclerView = view.findViewById(R.id.filteredClassesRecyclerView)
        btnToday = view.findViewById(R.id.btnClassesToday)
        btnTomorrow = view.findViewById(R.id.btnClassesTomorrow)
        btnThisWeek = view.findViewById(R.id.btnClassesThisWeek)
        btnNextWeek = view.findViewById(R.id.btnClassesNextweek)

        hotAdapter = ClassAdapter(emptyList()) { showClassDialog(it) }
        filteredAdapter = ClassAdapter(emptyList()) { showClassDialog(it) }

        hotClassesRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        filteredClassesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        hotClassesRecyclerView.adapter = hotAdapter
        filteredClassesRecyclerView.adapter = filteredAdapter

        fetchAllClasses()
        setupButtonClicks()
    }

    private fun fetchAllClasses() {
        db.collection("classes")
            .get()
            .addOnSuccessListener { result ->
                classList.clear()
                for (document in result) {
                    val model = ClassModel(
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: "",
                        time = document.getString("time") ?: "",
                        instructor = document.getString("instructor") ?: "",
                        duration = document.getLong("duration")?.toString() ?: "",
                        venue = document.getString("venue") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        capacity = document.getLong("capacity")?.toInt() ?: 0,
                        isOneTime = document.getBoolean("isOneTime") ?: true,
                        dayOfWeek = document.get("dayOfWeek") as? List<String> ?: emptyList(),
                        dateAdded = document.getString("dateAdded") ?: ""
                    )
                    classList.add(model)
                }
                showHotClasses()
            }
        hotClassesRecyclerView.visibility = View.VISIBLE
        hotClassesRecyclerView.visibility = View.INVISIBLE

    }

    private fun showHotClasses() {
        hotClassesRecyclerView.visibility = View.VISIBLE
        filteredClassesRecyclerView.visibility = View.GONE
        hotAdapter.updateList(classList.take(5))
    }

    private fun setupButtonClicks() {
        btnToday.setOnClickListener {
            filterByDate(Date())
        }

        btnTomorrow.setOnClickListener {
            val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            filterByDate(calendar.time)
        }

        btnThisWeek.setOnClickListener {
            filterByWeek(0)
        }

        btnNextWeek.setOnClickListener {
            filterByWeek(1)
        }
    }

    private fun filterByDate(targetDate: Date) {
        val targetDay = dayFormatter.format(targetDate)
        val targetDayOfYear = Calendar.getInstance().apply { time = targetDate }.get(Calendar.DAY_OF_YEAR)

        val filtered = classList.filter { cls ->
            if (cls.isOneTime) {
                try {
                    val classDate = sdfDate.parse(cls.dateAdded) ?: return@filter false
                    val classDayOfYear = Calendar.getInstance().apply { time = classDate }.get(Calendar.DAY_OF_YEAR)
                    classDayOfYear == targetDayOfYear
                } catch (e: Exception) {
                    false
                }
            } else {
                cls.dayOfWeek.contains(targetDay)
            }
        }

        filteredClassesRecyclerView.visibility = View.VISIBLE
        hotClassesRecyclerView.visibility = View.GONE
        filteredAdapter.updateList(filtered)
    }

    private fun filterByWeek(weekOffset: Int) {
        val startOfWeek = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, weekOffset)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }

        val endOfWeek = Calendar.getInstance().apply {
            time = startOfWeek.time
            add(Calendar.DAY_OF_WEEK, 6)
        }

        val weekDays = mutableSetOf<String>()
        val loopCalendar = Calendar.getInstance().apply { time = startOfWeek.time }
        repeat(7) {
            weekDays.add(dayFormatter.format(loopCalendar.time))
            loopCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val filtered = classList.filter { cls ->
            if (cls.isOneTime) {
                try {
                    val classDate = sdfDate.parse(cls.dateAdded) ?: return@filter false
                    classDate >= startOfWeek.time && classDate <= endOfWeek.time
                } catch (e: Exception) {
                    false
                }
            } else {
                cls.dayOfWeek.any { weekDays.contains(it) }
            }
        }

        filteredClassesRecyclerView.visibility = View.VISIBLE
        hotClassesRecyclerView.visibility = View.GONE
        filteredAdapter.updateList(filtered)
    }

    private fun showClassDialog(classModel: ClassModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(classModel.name)

        val message = """
         Instructor: ${classModel.instructor}
         Time: ${classModel.time}
         Duration: ${classModel.duration} mins
         Venue: ${classModel.venue}
         Price: ₹${classModel.price}
         Capacity: ${classModel.capacity}
         Description: ${classModel.description}
    """.trimIndent()

        builder.setMessage(message)

        builder.setPositiveButton("Register") { dialog, _ ->
            val bundle = Bundle().apply {
                putString("className", classModel.name)
                putDouble("classPrice", classModel.price)
            }

            val paymentFragment = ClassesPaymentFragment()
            paymentFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, paymentFragment)
                .addToBackStack(null)
                .commit()

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
    }

