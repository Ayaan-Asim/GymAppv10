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

class PlanPaymentFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_plan_payment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // UI refs
        val chkPlan    = view.findViewById<CheckBox>(R.id.cbPlan)
        val chkOffer   = view.findViewById<CheckBox>(R.id.cbOffer)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmPayment)
        val tvName     = view.findViewById<TextView>(R.id.tvClassName)

        // Args
        val planName       = arguments?.getString("planName") ?: ""
        val planDuration   = arguments?.getString("planDuration") ?: ""
        val planPrice      = arguments?.getString("planPrice") ?: "0"
        val offerDuration  = arguments?.getString("planOfferDuration") ?: ""
        val offerPrice     = arguments?.getString("planOfferPrice") ?: "0"

        tvName.text = planName
        chkPlan.text  = "$planPrice for $planDuration month"
        chkOffer.text = "$offerPrice for $offerDuration months"

        // Exclusive selection
        chkPlan.setOnClickListener {
            chkOffer.isChecked = false
            chkPlan.isChecked = true
        }
        chkOffer.setOnClickListener {
            chkPlan.isChecked = false
            chkOffer.isChecked = true
        }

        btnConfirm.setOnClickListener {
            // Determine selected price & type
            val (amount, type) = if (chkOffer.isChecked) {
                offerPrice.toInt() to "Offer"
            } else {
                planPrice.toInt() to "Regular"
            }

            if (!chkOffer.isChecked && !chkPlan.isChecked) {
                Toast.makeText(requireContext(), "Please select a plan option", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showTxnDialog(planName, amount, type)
        }
    }

    private fun showTxnDialog(planName: String, amount: Int, type: String) {
        val dlgView = layoutInflater.inflate(R.layout.dialog_transaction, null)
        val spinner = dlgView.findViewById<Spinner>(R.id.spinnerMethod)
        val etTxn   = dlgView.findViewById<EditText>(R.id.etDialogTxnId)
        val btn     = dlgView.findViewById<Button>(R.id.btnDialogConfirm)

        val methods = listOf("JazzCash", "EasyPaisa")
        spinner.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, methods)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dlgView)
            .create()

        btn.setOnClickListener {
            val txnId = etTxn.text.toString().trim()
            val method = spinner.selectedItem as String

            if (txnId.isEmpty()) {
                etTxn.error = "Required"
                return@setOnClickListener
            }

            savePaymentRequest(planName, amount, type, method, txnId)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun savePaymentRequest(
        planName: String,
        amount: Int,
        type: String,
        method: String,
        txnId: String
    ) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val data = mapOf(
            "userId"       to user.uid,
            "userName"     to (user.displayName ?: user.email),
            "planName"     to planName,
            "amount"       to amount,
            "planType"     to type,
            "paymentMethod" to method,
            "txnId"        to txnId,
            "status"       to "pending",
            "timestamp"    to Timestamp.now()
        )
        db.collection("Payments")
            .document("requests")
            .collection("Membership")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Request submitted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
