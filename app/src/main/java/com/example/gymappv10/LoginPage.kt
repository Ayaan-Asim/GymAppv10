package com.example.gymappv10

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences

    private val isAdminApp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_page)

        // Initialize Firebase and SharedPreferences FIRST
        initializeFirebase()

        val loginEmail = findViewById<EditText>(R.id.loginEmailAddress)
        val loginPassword = findViewById<EditText>(R.id.editTextTextPassword)
        val btnDirectToRegisterPage = findViewById<TextView>(R.id.registeringPageDirector)
        val btnLoginUser = findViewById<Button>(R.id.btnLogin)

        btnDirectToRegisterPage.setOnClickListener {
            startActivity(Intent(this@LoginPage, RegisterationPage::class.java))
            finish()
        }

        btnLoginUser.setOnClickListener {
            val email = loginEmail.text.toString().trim()
            val password = loginPassword.text.toString().trim()

            if (!validateInputs(email, password)) return@setOnClickListener

            btnLoginUser.isEnabled = false  // Prevent spam taps
            loginUser(email, password, btnLoginUser)
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if Firebase is initialized before using it
        if (::auth.isInitialized && ::sharedPref.isInitialized) {
            checkExistingUser()
        }
    }

    private fun initializeFirebase() {
        try {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            Log.d("LoginPage", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("LoginPage", "Error initializing Firebase", e)
            showToast("Error initializing app. Please restart.")
        }
    }

    private fun checkExistingUser() {
        val currentUser = auth.currentUser
        val savedRole = sharedPref.getString("userRole", null)

        Log.d("LoginPage", "Current user: ${currentUser?.email}, Saved role: $savedRole")

        if (currentUser != null && savedRole != null) {
            if (savedRole == "member") {
                Log.d("LoginPage", "User already logged in, redirecting to MainActivity")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Log.d("LoginPage", "Invalid role for member app, signing out")
                auth.signOut()
                showToast("This account cannot access the Member App.")
            }
        }
    }

    private fun loginUser(email: String, password: String, loginButton: Button) {
        Log.d("LoginPage", "Attempting login for: $email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                loginButton.isEnabled = true

                if (task.isSuccessful) {
                    Log.d("LoginPage", "Firebase authentication successful")
                    val currentUser = auth.currentUser

                    if (currentUser != null) {
                        Log.d("LoginPage", "User UID: ${currentUser.uid}")
                        fetchUserRoleAndRedirect(currentUser.uid)
                    } else {
                        Log.w("LoginPage", "Current user is null after successful login")
                        showToast("Login failed. Please try again.")
                    }
                } else {
                    Log.w("LoginPage", "signInWithEmail:failure", task.exception)
                    val errorMessage = when (task.exception?.message) {
                        "The email address is badly formatted." -> "Please enter a valid email address"
                        "The password is invalid or the user does not have a password." -> "Incorrect password"
                        "There is no user record corresponding to this identifier. The user may have been deleted." -> "No account found with this email"
                        else -> "Login failed: ${task.exception?.message}"
                    }
                    showToast(errorMessage)
                }
            }
            .addOnFailureListener { exception ->
                loginButton.isEnabled = true
                Log.e("LoginPage", "Login failed with exception", exception)
                showToast("Login failed. Please check your internet connection.")
            }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please enter your email and password")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Invalid email format")
            return false
        }

        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            return false
        }

        return true
    }

    private fun fetchUserRoleAndRedirect(userId: String) {
        Log.d("LoginPage", "Fetching user role for UID: $userId")

        db.collection("users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                Log.d("LoginPage", "Firestore query successful")

                if (documentSnapshot.exists()) {
                    val role = documentSnapshot.getString("role")
                    val name = documentSnapshot.getString("name") ?: ""
                    val email = documentSnapshot.getString("email") ?: ""

                    Log.d("LoginPage", "User data - Role: $role, Name: $name")

                    if (role != null) {
                        val isValidRole = (!isAdminApp && role == "member")

                        if (!isValidRole) {
                            Log.w("LoginPage", "Invalid role: $role for member app")
                            auth.signOut()
                            showToast("This account is for a different app.")
                            return@addOnSuccessListener
                        }

                        // Save user data in SharedPreferences
                        sharedPref.edit()
                            .putString("userRole", role)
                            .putString("userUid", userId)
                            .putString("userName", name)
                            .putString("userEmail", email)
                            .putString("isLoggedIn", "yes")
                            .apply()

                        Log.d("LoginPage", "User data saved, redirecting to MainActivity")
                        showToast("Login successful!")

                        // Redirect to Member MainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Log.w("LoginPage", "User role is null")
                        showToast("User role not found. Please contact support.")
                    }
                } else {
                    Log.w("LoginPage", "User document does not exist in Firestore")
                    showToast("User profile not found. Please register first.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginPage", "Error fetching user role from Firestore", e)
                showToast("Error loading user data. Please try again.")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}