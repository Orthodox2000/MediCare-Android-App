package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val btnEmergency = findViewById<Button>(R.id.btnEmergency)
        val btnAddData = findViewById<Button>(R.id.btnAddData)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnEmergency.setOnClickListener {
            // Placeholder (we'll add real logic later)
            val intent = Intent(this, EmergencyActivity::class.java)
            startActivity(intent)
        }

        btnAddData.setOnClickListener {
            val intent = Intent(this, AddHealthDataActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginRegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
