package com.example.medicare.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.medicare.R
import com.example.medicare.data.Api
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddHealthDataActivity : BaseActivity() {

    private val jsonMediaType = "application/json".toMediaType()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_health_data)

        val etWeight = findViewById<EditText>(R.id.etWeight)
        val etBP = findViewById<EditText>(R.id.etBP)
        val etSugar = findViewById<EditText>(R.id.etSugar)
        val etHeart = findViewById<EditText>(R.id.etHeart)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val weightStr = etWeight.text.toString().trim()
            val bpStr = etBP.text.toString().trim()
            val sugarStr = etSugar.text.toString().trim()
            val heartStr = etHeart.text.toString().trim()

            if (weightStr.isEmpty() || sugarStr.isEmpty()) {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val json = JSONObject().apply {
            put("userId", email)
            put("date", today)
            put("weight", weight)
            put("bloodPressure", bloodPressure)
            put("sugar", sugar)
            put("heartRate", heartRate)
        }

        val request = Request.Builder()
            .url("https://medi-care-roan.vercel.app/api/health/add")
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        Api.http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@AddHealthDataActivity, "Network error", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@AddHealthDataActivity, "Health data saved", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@AddHealthDataActivity, "Server error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}

