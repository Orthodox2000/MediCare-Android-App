package com.example.medicare.ui

import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresApi
import com.example.medicare.R
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate

class AddHealthDataActivity : BaseActivity() {

    private val client = OkHttpClient()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_health_data)

        val etWeight = findViewById<EditText>(R.id.etWeight)
        val etBP = findViewById<EditText>(R.id.etBP)
        val etSugar = findViewById<EditText>(R.id.etSugar)
        val etHeart = findViewById<EditText>(R.id.etHeart)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {

            // 🔹 Read as String first
            val weightStr = etWeight.text.toString().trim()
            val bpStr = etBP.text.toString().trim()
            val sugarStr = etSugar.text.toString().trim()
            val heartStr = etHeart.text.toString().trim()

            // 🔹 Basic validation
            if (weightStr.isEmpty() || sugarStr.isEmpty()) {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 SAFE conversion (prevents crash)
            val weight = weightStr.toIntOrNull()
            val sugar = sugarStr.toIntOrNull()
            val heartRate = heartStr.toIntOrNull() ?: 0
            val bloodPressure = bpStr.toIntOrNull() ?: 0

            if (weight == null || sugar == null) {
                Toast.makeText(this, "Invalid numeric values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendToApi(
                weight = weight,
                bloodPressure = bloodPressure,
                sugar = sugar,
                heartRate = heartRate
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendToApi(
        weight: Int,
        bloodPressure: Int,
        sugar: Int,
        heartRate: Int
    ) {

        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email

        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val json = JSONObject().apply {
            put("userId", email)                 // ✅ REAL LOGGED-IN EMAIL
            put("date", LocalDate.now().toString())
            put("weight", weight)
            put("bloodPressure", bloodPressure)
            put("sugar", sugar)
            put("heartRate", heartRate)
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://medi-care-roan.vercel.app/api/health/add")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@AddHealthDataActivity,
                        "Network error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AddHealthDataActivity,
                            "Health data saved ✔",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@AddHealthDataActivity,
                            "Server error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}