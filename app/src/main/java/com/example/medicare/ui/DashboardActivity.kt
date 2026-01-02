package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.medicare.R
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔐 Session check (important)
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginRegisterActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_dashboard)

        val btnEmergency = findViewById<Button>(R.id.btnEmergency)
        val btnAddData = findViewById<Button>(R.id.btnAddData)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnEmergency.setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }

        btnAddData.setOnClickListener {
            startActivity(Intent(this, AddHealthDataActivity::class.java))
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginRegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
