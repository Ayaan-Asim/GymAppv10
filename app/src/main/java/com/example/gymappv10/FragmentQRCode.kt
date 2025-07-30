package com.example.gymappv10

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class FragmentQRCode : Fragment() {

    private lateinit var previewView: PreviewView
    private var scanned = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        previewView = PreviewView(requireContext())
        return previewView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startCamera()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        showToast("Camera starting...")
        val provider = ProcessCameraProvider.getInstance(requireContext())
        provider.addListener({
            val camProvider = provider.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imgProxy ->
                if (scanned) {
                    imgProxy.close()
                    return@setAnalyzer
                }
                val media = imgProxy.image
                if (media != null) {
                    val image = InputImage.fromMediaImage(media, imgProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { codes ->
                            val code = codes.firstOrNull()?.rawValue
                            if (code != null) {
                                scanned = true
                                showToast("QR scanned: $code")
                                handleCode(code)
                            } else {
                                showToast("No QR code detected")
                            }
                        }
                        .addOnFailureListener {
                            showToast("QR scan error: ${it.message}")
                        }
                        .addOnCompleteListener { imgProxy.close() }
                } else {
                    showToast("Image is null")
                    imgProxy.close()
                }
            }

            camProvider.unbindAll()
            camProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            showToast("Camera ready")
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun handleCode(code: String) {
        showToast("Checking QR code...")
        val db = FirebaseFirestore.getInstance()
        db.collection("qr_code").document("current_code").get()
            .addOnSuccessListener { doc ->
                val validCode = doc.getString("code")
                if (validCode == null) {
                    showToast("QR code in DB is null")
                    return@addOnSuccessListener
                }
                if (code == validCode) {
                    showToast("QR matched. Saving attendance...")
                    saveAttendance()
                } else {
                    showToast("Invalid QR code")
                }
            }
            .addOnFailureListener {
                showToast("QR fetch error: ${it.message}")
            }
    }

    private fun saveAttendance() {
        showToast("Saving attendance...")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            showToast("User not logged in")
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("name") ?: "UnknownUser"

                val now = Date()
                val cal = Calendar.getInstance().apply { time = now }
                val year = cal.get(Calendar.YEAR).toString()
                val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(now)
                val fullDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(now)
                val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
                val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
                val week = "week_${cal.get(Calendar.WEEK_OF_MONTH)}"
                val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)

                val attendanceHistoryDoc = db.collection("attendance_history")
                    .document(monthName).collection(fullDate).document(userName)

                val monthMetaDoc = db.collection("attendance_history").document(monthName)

                val userAttendance = db.collection("attendance_records").document(uid)
                    .collection(year).document(monthName).collection(week).document(fullDate)

                val totalTimeMonthlyDoc = db.collection("attendance_records").document(uid)
                    .collection(year).document(monthName) // 👈 for total time

                val vennYear = db.collection("reports_summary").document(year)
                    .collection("general").document("attendance")

                val vennMonth = db.collection("reports_summary").document(year)
                    .collection(monthName).document("general").collection("attendance").document("attendance")

                val vennWeek = db.collection("reports_summary").document(year)
                    .collection(monthName).document(week)
                    .collection("general").document("attendance")

                val memberSummary = db.collection("reports_summary").document(year)
                    .collection(monthName).document("member_summary").collection("members").document(uid)

                db.runTransaction { txn ->
                    val snap = txn.get(userAttendance)
                    val memberSnap = txn.get(memberSummary)

                    if (snap.exists()) {
                        // 🕒 CHECKOUT → Update checkOutTime & calculate time spent
                        txn.update(userAttendance, "checkOutTime", now)

                        snap.getDate("checkInTime")?.let { checkInTime ->
                            val durationMillis = now.time - checkInTime.time
                            val minutesSpent = TimeUnit.MILLISECONDS.toMinutes(durationMillis)

                            txn.set(
                                totalTimeMonthlyDoc,
                                mapOf("totalTimeSpentThisMonth" to FieldValue.increment(minutesSpent)),
                                SetOptions.merge()
                            )
                        }
                    } else {

                        txn.set(userAttendance, mapOf("checkInTime" to now))

                        listOf(vennYear, vennMonth, vennWeek).forEach { docRef ->
                            txn.set(docRef, mapOf("totalCheckins" to FieldValue.increment(1)), SetOptions.merge())
                            txn.set(docRef, mapOf(
                                "dailyCounts.counts.$isoDate.checkinCount" to FieldValue.increment(1)
                            ), SetOptions.merge())
                        }

                        if (!memberSnap.exists()) {
                            txn.set(memberSummary, mapOf(
                                "totalDaysPresent" to 1,
                                "lastAttendanceDate" to now,
                                "checkinDays" to mapOf(dayOfWeek to 1)
                            ))
                            txn.set(vennMonth, mapOf("activeMembers" to FieldValue.increment(1)), SetOptions.merge())
                        } else {
                            val curr = memberSnap.getLong("totalDaysPresent") ?: 0
                            val checkinDays = memberSnap.get("checkinDays") as? Map<*, *> ?: emptyMap<String, Long>()
                            val currentDayCount = (checkinDays[dayOfWeek] as? Long) ?: 0
                            txn.update(memberSummary, mapOf(
                                "totalDaysPresent" to curr + 1,
                                "lastAttendanceDate" to now,
                                "checkinDays.$dayOfWeek" to currentDayCount + 1
                            ))
                        }

                        txn.set(attendanceHistoryDoc, mapOf("time" to timeNow))
                        txn.set(monthMetaDoc, mapOf(
                            "dateList" to FieldValue.arrayUnion(fullDate)
                        ), SetOptions.merge())
                    }
                }.addOnSuccessListener {
                    showToast("Attendance saved successfully")
                    parentFragmentManager.popBackStack()
                }.addOnFailureListener {
                    showToast("Transaction failed: ${it.message}")
                }
            }
            .addOnFailureListener {
                showToast("User fetch failed: ${it.message}")
            }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
