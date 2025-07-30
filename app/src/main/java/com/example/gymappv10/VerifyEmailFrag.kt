package com.example.gymappv10

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VerifyEmailFrag : Fragment() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var userViewModel: SharedUserViewModel
    private var countDownTimer: CountDownTimer? = null
    private val handler = Handler()
    private var timeLeftInMillis: Long = 35000

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_verify_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel = ViewModelProvider(requireActivity())[SharedUserViewModel::class.java]

        val putUserEmailhere = view.findViewById<TextView>(R.id.userEmailhere)
        val btnResendEmail = view.findViewById<Button>(R.id.btnResend)
        val changeEmail = view.findViewById<TextView>(R.id.ChangeEmailAddress)

        putUserEmailhere.text = userViewModel.email.value ?: "No Email"

        startTimer(btnResendEmail)
        checkEmailVerification()

        btnResendEmail.setOnClickListener {
            if (btnResendEmail.isEnabled) {
                resendVerificationEmail(btnResendEmail)
            }
        }

        changeEmail.setOnClickListener {
            findNavController().navigate(R.id.action_verifyEmailFrag_to_fragment_register)
        }
    }

    private fun resendVerificationEmail(btnResendEmail: Button) {
        auth.currentUser?.sendEmailVerification()
            ?.addOnSuccessListener {
                Toast.makeText(requireContext(), "Verification email sent!", Toast.LENGTH_SHORT).show()
                timeLeftInMillis = 35000
                startTimer(btnResendEmail)
            }
            ?.addOnFailureListener { e ->
                Log.e("VerifyEmailFrag", "Failed to resend email", e)
                Toast.makeText(requireContext(), "Failed to resend email: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startTimer(btn: Button) {
        btn.isEnabled = false
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                btn.text = "Resend Email (in ${millisUntilFinished / 1000}s)"
            }

            override fun onFinish() {
                btn.text = "Resend Email"
                btn.isEnabled = true
            }
        }.start()
    }

    private fun checkEmailVerification() {
        val delay = 3000L
        handler.postDelayed(object : Runnable {
            override fun run() {
                auth.currentUser?.reload()?.addOnSuccessListener {
                    if (auth.currentUser?.isEmailVerified == true) {
                        Log.d("VerifyEmailFrag", "Email verified successfully")
                        Toast.makeText(requireContext(), "Email Verified!", Toast.LENGTH_SHORT).show()
                        saveUserDataToFirestore()
                    } else {
                        Log.d("VerifyEmailFrag", "Email not yet verified, checking again...")
                        handler.postDelayed(this, delay)
                    }
                }?.addOnFailureListener { e ->
                    Log.e("VerifyEmailFrag", "Error checking email verification", e)
                    handler.postDelayed(this, delay)
                }
            }
        }, delay)
    }

    private fun saveUserDataToFirestore() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("VerifyEmailFrag", "User ID is null")
            showError("Error: User not found")
            return
        }

        val sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Get user data from ViewModel or SharedPreferences
        val name = userViewModel.name.value ?: sharedPref.getString("tempUserName", "") ?: ""
        val email = userViewModel.email.value ?: sharedPref.getString("tempUserEmail", "") ?: ""
        val phone = userViewModel.phone.value ?: sharedPref.getString("tempUserPhone", "") ?: ""
        val year = userViewModel.year.value ?: sharedPref.getString("tempUserYear", "") ?: ""
        val month = userViewModel.month.value ?: sharedPref.getString("tempUserMonth", "") ?: ""

        val userInfo = hashMapOf(
            "name" to name,
            "email" to email,
            "phone" to phone,
            "year" to year,
            "month" to month,
            "role" to "member",
            "verified" to true,
            "createdAt" to System.currentTimeMillis()
        )

        Log.d("VerifyEmailFrag", "Saving user data to Firestore for UID: $userId")
        Log.d("VerifyEmailFrag", "User data: $userInfo")

        db.collection("users").document(userId).set(userInfo)
            .addOnSuccessListener {
                Log.d("VerifyEmailFrag", "User data saved successfully to Firestore")
                saveUserDataToSharedPreferences(userId, name, email, phone, year, month)
                navigateToMainActivity()
            }
            .addOnFailureListener { e ->
                Log.e("VerifyEmailFrag", "Failed to save user data to Firestore", e)
                showError("Failed to save user data: ${e.message}")
            }
    }

    private fun saveUserDataToSharedPreferences(userId: String, name: String, email: String, phone: String, year: String, month: String) {
        val sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("userRole", "member")
            putString("userUid", userId)
            putString("userName", name)
            putString("userEmail", email)
            putString("userPhone", phone)
            putString("userYear", year)
            putString("userMonth", month)
            putString("isLoggedIn", "yes")

            // Clean up temporary data
            remove("tempUserId")
            remove("tempUserName")
            remove("tempUserEmail")
            remove("tempUserPhone")
            remove("tempUserYear")
            remove("tempUserMonth")

            apply()
        }
        Log.d("VerifyEmailFrag", "User data saved to SharedPreferences")
    }

    private fun navigateToMainActivity() {
        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}