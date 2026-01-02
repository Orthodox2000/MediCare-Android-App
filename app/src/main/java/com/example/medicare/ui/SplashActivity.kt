package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            // User already logged in
            startActivity(Intent(this, DashboardActivity::class.java))
        } else {
            // User not logged in
            startActivity(Intent(this, LoginRegisterActivity::class.java))
        }

        finish()
    }
}
