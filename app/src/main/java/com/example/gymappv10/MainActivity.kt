package com.example.gymappv10

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {
    private lateinit var homeBtn : LinearLayout
    private lateinit var classesBtn : LinearLayout
    private lateinit var communityBtn : LinearLayout
    private lateinit var progressBtn : LinearLayout
    private lateinit var attendanceBtn : LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
         homeBtn = findViewById(R.id.homeButton)
         classesBtn = findViewById(R.id.classesButton)
         communityBtn = findViewById(R.id.communityButton)
         progressBtn = findViewById(R.id.progressButton)
         attendanceBtn = findViewById(R.id.attendanceButton)
        val sharedPref: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val isVerified = sharedPref.getString("isLoggedIn", "no")

        if (isVerified != "yes") {
            val intent = Intent(this@MainActivity, LoginPage::class.java)
            startActivity(intent)
            finish()
        }
        homeBtn.setOnClickListener {
            loadFragment(HomeFragment())
            changeBG(homeBtn)
        }
        classesBtn.setOnClickListener {
            loadFragment(ClassesFragment())
            changeBG(classesBtn)
        }
        communityBtn.setOnClickListener {
            loadFragment(CommunityFragment())
            changeBG(communityBtn)
        }
        attendanceBtn.setOnClickListener{
            loadFragment(AttendanceFragment())
            changeBG(attendanceBtn)
        }
        progressBtn.setOnClickListener {
            changeBG(progressBtn)
            loadFragment(ProgressFragment())
        }
    }
    private fun loadFragment(fragment : Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container,fragment).commit()
    }
    private fun changeBG(activeView : LinearLayout){
        val allTabs = listOf(homeBtn,classesBtn,communityBtn,progressBtn,attendanceBtn)
        allTabs.forEach{
            it.setBackgroundColor(resources.getColor(R.color.appBG))
            it.isClickable =  true
        }
        activeView.setBackgroundColor(resources.getColor(R.color.selectColor))
        activeView.isClickable = false
    }
}

