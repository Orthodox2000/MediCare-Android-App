package com.example.medicare.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R

class EmergencyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency)

        val btn112 = findViewById<Button>(R.id.btn112)
        val btnPoison = findViewById<Button>(R.id.btnPoison)
        val btnMental = findViewById<Button>(R.id.btnMental)
        val btnHospital = findViewById<Button>(R.id.btnHospital)
        val btnEmergency = findViewById<Button>(R.id.btnCallEmergency)

        btn112.setOnClickListener { callNumber("112") }
        btnPoison.setOnClickListener { callNumber("18002221222") }
        btnMental.setOnClickListener { callNumber("988") }
        btnHospital.setOnClickListener { callNumber("+15550123") }

        btnEmergency.setOnClickListener { callNumber("112") }
    }

    private fun callNumber(number: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$number")
        startActivity(intent)
    }
}