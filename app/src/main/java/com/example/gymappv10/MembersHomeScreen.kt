package com.example.gymappv10

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class MembersHomeScreen : Fragment() {

    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_members_home_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

            val isVerified = sharedPref.getString("isLoggedIn", "no")

            if (isVerified != "yes") {
                redirectToLogin()
            }

            val buttonLogout = view.findViewById<Button>(R.id.btnLogout)
            buttonLogout.setOnClickListener {
                logoutUser()
            }
        } catch (e: Exception) {
            Log.e("MembersHomeScreen", "Error initializing SharedPreferences", e)
        }
    }

    private fun logoutUser() {
        try {
            FirebaseAuth.getInstance().signOut()

            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()

            redirectToLogin()
        } catch (e: Exception) {
            Log.e("MembersHomeScreen", "Error during logout", e)
        }
    }

    private fun redirectToLogin() {
        try {
            val intent = Intent(requireActivity(), LoginPage::class.java)
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            Log.e("MembersHomeScreen", "Error redirecting to LoginPage", e)
        }
    }
}
