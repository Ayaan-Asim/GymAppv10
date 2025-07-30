package com.example.gymappv10

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences
    private lateinit var userViewModel: SharedUserViewModel

    private var selectedYear: String? = null
    private var selectedMonth: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase and other components
        initializeComponents()

        val yearAutoComplete: AutoCompleteTextView = view.findViewById(R.id.autoCompleteTextViewYear)
        val monthAutoComplete: AutoCompleteTextView = view.findViewById(R.id.autoCompleteTextViewMonth)
        val userName = view.findViewById<EditText>(R.id.userName)
        val userEmail = view.findViewById<EditText>(R.id.userEmail)
        val userPhone = view.findViewById<EditText>(R.id.userPhone)
        val password = view.findViewById<EditText>(R.id.userPassword)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitRequest)

        setupDropdowns(yearAutoComplete, monthAutoComplete)

        btnSubmit.setOnClickListener {
            registerUser(userName, userEmail, userPhone, password)
        }
    }

    private fun initializeComponents() {
        try {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            userViewModel = ViewModelProvider(requireActivity())[SharedUserViewModel::class.java]
            Log.d("RegisterFragment", "Components initialized successfully")
        } catch (e: Exception) {
            Log.e("RegisterFragment", "Error initializing components", e)
            showError("Initialization error. Please restart the app.")
        }
    }

    private fun setupDropdowns(yearAutoComplete: AutoCompleteTextView, monthAutoComplete: AutoCompleteTextView) {
        val years = (1900..2025).map { it.toString() }
        val months = listOf("January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December")

        yearAutoComplete.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, years))
        monthAutoComplete.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, months))

        yearAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedYear = years[position]
            Log.d("RegisterFragment", "Selected year: $selectedYear")
        }
        monthAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedMonth = months[position]
            Log.d("RegisterFragment", "Selected month: $selectedMonth")
        }
    }

    private fun registerUser(userName: EditText, userEmail: EditText, userPhone: EditText, userPassword: EditText) {
        val name = userName.text.toString().trim()
        val email = userEmail.text.toString().trim()
        val phone = userPhone.text.toString().trim()
        val password = userPassword.text.toString().trim()

        Log.d("RegisterFragment", "Attempting registration for: $email")

        if (!validateInput(name, email, phone, password)) return

        // Store data in ViewModel first (for VerifyEmailFragment to use)
        userViewModel.name.value = name
        userViewModel.email.value = email
        userViewModel.phone.value = phone
        userViewModel.year.value = selectedYear
        userViewModel.month.value = selectedMonth

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        Log.d("RegisterFragment", "Firebase Auth successful, UID: $userId")

                        // Send verification email first
                        sendVerificationEmail(userId, name, email, phone)
                    } else {
                        Log.e("RegisterFragment", "User ID is null after successful registration")
                        showError("Registration failed. Please try again.")
                    }
                } else {
                    Log.e("RegisterFragment", "Firebase Auth failed", task.exception)
                    showError("Registration failed: ${task.exception?.message}")
                }
            }
    }

    private fun sendVerificationEmail(userId: String, name: String, email: String, phone: String) {
        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { emailTask ->
                if (emailTask.isSuccessful) {
                    Log.d("RegisterFragment", "Verification email sent successfully")

                    // Save basic user info to SharedPreferences (will be completed after verification)
                    sharedPref.edit()
                        .putString("tempUserId", userId)
                        .putString("tempUserName", name)
                        .putString("tempUserEmail", email)
                        .putString("tempUserPhone", phone)
                        .putString("tempUserYear", selectedYear)
                        .putString("tempUserMonth", selectedMonth)
                        .apply()

                    showError("Verification email sent to $email")
                    findNavController().navigate(R.id.action_fragment_register_to_verifyEmailFrag)
                } else {
                    Log.e("RegisterFragment", "Failed to send verification email", emailTask.exception)
                    showError("Failed to send verification email: ${emailTask.exception?.message}")
                }
            }
    }

    private fun validateInput(name: String, email: String, phone: String, password: String): Boolean {
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()
            || selectedYear.isNullOrEmpty() || selectedMonth.isNullOrEmpty()) {
            showError("Please fill all fields")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Invalid email format")
            return false
        }

        if (password.length < 6) {
            showError("Password must be at least 6 characters")
            return false
        }

        if (phone.length < 10) {
            showError("Please enter a valid phone number")
            return false
        }

        return true
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}