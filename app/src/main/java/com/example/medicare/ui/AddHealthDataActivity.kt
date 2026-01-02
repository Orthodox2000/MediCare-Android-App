package com.example.medicare.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AddHealthDataActivity : BaseActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_health_data)

        val etWeight = findViewById<EditText>(R.id.etWeight)
        val etHeight = findViewById<EditText>(R.id.etHeight)
        val etBP = findViewById<EditText>(R.id.etBP)
        val etSugar = findViewById<EditText>(R.id.etSugar)
        val etHeart = findViewById<EditText>(R.id.etHeart)
        val etSteps = findViewById<EditText>(R.id.etSteps)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {

            val weight = etWeight.text.toString().toDoubleOrNull()
            val height = etHeight.text.toString().toIntOrNull()
            val sugar = etSugar.text.toString().toIntOrNull()
            val heart = etHeart.text.toString().toIntOrNull()
            val steps = etSteps.text.toString().toIntOrNull()
            val bp = etBP.text.toString().trim().ifEmpty { null }

            if (bp != null && !bp.matches(Regex("\\d{2,3}/\\d{2,3}"))) {
                Toast.makeText(this, "Invalid BP format (120/80)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendToApi(weight, height, bp, sugar, heart, steps)
        }
    }

    private fun sendToApi(
        weight: Double?,
        height: Int?,
        bp: String?,
        sugar: Int?,
        heart: Int?,
        steps: Int?
    ) {
        val json = JSONObject().apply {
            put("username", "demo_user")               // replace later
            put("device", android.os.Build.MODEL)
            put("location", "IN")

            put("weight", weight)
            put("height", height)
            put("bp", bp)
            put("sugar", sugar)
            put("heartRate", heart)
            put("steps", steps)
        }

        val body = json.toString().toRequestBody(
            "application/json".toMediaTypeOrNull()
        )

        val request = Request.Builder()
            .url("http://localhost:3000/api/testadd")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@AddHealthDataActivity,
                        "Failed to send data",
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
