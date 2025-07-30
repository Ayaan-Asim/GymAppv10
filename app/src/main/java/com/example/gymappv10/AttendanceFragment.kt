package com.example.gymappv10

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*

class AttendanceFragment : Fragment() {

    private lateinit var btnMarkAttendance: Button
    private var hasCheckedInToday = false
    private var hasCheckedOutToday = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) checkCameraPermission()
        else Toast.makeText(context, "Location permission needed", Toast.LENGTH_SHORT).show()
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) navigateToQR()
        else Toast.makeText(context, "Camera permission needed", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadTodayStatus { inToday, outToday ->
            hasCheckedInToday = inToday
            hasCheckedOutToday = outToday
            btnMarkAttendance.text = if (!inToday) "Check In" else "Check Out"
            btnMarkAttendance.visibility = if (!outToday) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_attendance, container, false)

        btnMarkAttendance = root.findViewById(R.id.btnMarkAttendance)
        val recycler = root.findViewById<RecyclerView>(R.id.rvAttendanceHistory)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        btnMarkAttendance.setOnClickListener {
            checkLocationPermission()
        }

        loadAttendanceHistory { list ->
            recycler.adapter = AttendanceAdapter(list)
        }

        return root
    }

    private fun checkLocationPermission() {
        val ctx = requireContext()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkCameraPermission()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun checkCameraPermission() {
        val ctx = requireContext()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            navigateToQR()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun navigateToQR() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FragmentQRCode())
            .addToBackStack(null)
            .commit()
    }

    private fun loadTodayStatus(callback: (Boolean, Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val now = Date()

        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(now)
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(now)
        val week = "week_" + Calendar.getInstance().get(Calendar.WEEK_OF_MONTH)
        val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(now)

        db.collection("attendance_records")
            .document(uid).collection(year).document(monthName)
            .collection(week).document(date)
            .get()
            .addOnSuccessListener { doc ->
                val inTime = doc.getTimestamp("checkInTime")
                val outTime = doc.getTimestamp("checkOutTime")
                callback(inTime != null, outTime != null)
            }
            .addOnFailureListener {
                callback(false, false)
            }
    }

    private fun loadAttendanceHistory(callback: (List<AttendanceRecord>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val history = mutableListOf<AttendanceRecord>()

        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR).toString()
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())

        val weekTasks = (1..5).map { w ->
            db.collection("attendance_records").document(uid)
                .collection(year).document(monthName)
                .collection("week_$w").get()
        }

        Tasks.whenAllSuccess<QuerySnapshot>(weekTasks)
            .addOnSuccessListener { weekResults ->
                val dfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                for (weekSnap in weekResults) {
                    for (doc in weekSnap.documents) {
                        val date = doc.id
                        val inTS = doc.getTimestamp("checkInTime")?.toDate()
                        val outTS = doc.getTimestamp("checkOutTime")?.toDate()
                        val inStr = inTS?.let { dfTime.format(it) }
                        val outStr = outTS?.let { dfTime.format(it) }
                        val dur = if (inTS != null && outTS != null) ((outTS.time - inTS.time) / 60000) else null
                        history.add(AttendanceRecord(date, inStr, outStr, dur))
                    }
                }
                history.sortByDescending { it.date }
                callback(history)
            }
    }
}
