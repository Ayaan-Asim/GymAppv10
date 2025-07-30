package com.example.gymappv10

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClassesPaymentFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var className: String? = null
    private var classPrice: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_classes_payment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        className = arguments?.getString("className")
        classPrice = arguments?.getDouble("classPrice") ?: 0.0

        val tvClassName = view.findViewById<TextView>(R.id.tvClassName)
        val tvClassPrice = view.findViewById<TextView>(R.id.tvClassPrice)
        val btnConfirmPayment = view.findViewById<Button>(R.id.btnConfirmPayment)

        tvClassName.text = "Class Name: $className"
        tvClassPrice.text = "Class Price: Rs. ${classPrice.toInt()}"

        btnConfirmPayment.setOnClickListener {
            showTxnDialog()
        }
    }

    private fun showTxnDialog() {
        val dlgView = layoutInflater.inflate(R.layout.dialog_transaction_classes, null)
        val spinner = dlgView.findViewById<Spinner>(R.id.spinnerMethod)
        val etTxnId = dlgView.findViewById<EditText>(R.id.etDialogTxnId)
        val btnDialog = dlgView.findViewById<Button>(R.id.btnDialogConfirm)

        val methods = listOf("JazzCash", "EasyPaisa")
        spinner.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, methods)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dlgView)
            .create()

        btnDialog.setOnClickListener {
            val txnId = etTxnId.text.toString().trim()
            val method = spinner.selectedItem as String

            if (txnId.isEmpty()) {
                etTxnId.error = "Enter Transaction ID"
                return@setOnClickListener
            }

            uploadClassPayment(txnId, method)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun uploadClassPayment(txnId: String, method: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val data = mapOf(
            "userId" to user.uid,
            "userName" to (user.displayName ?: user.email ?: ""),
            "planName" to className,
            "amount" to classPrice.toInt(),
            "planType" to "Regular",
            "paymentMethod" to method,
            "txnId" to txnId,
            "status" to "pending",
            "timestamp" to Timestamp.now()
        )

        db.collection("Payments")
            .document("requests")
            .collection("Classes")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Payment request sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
