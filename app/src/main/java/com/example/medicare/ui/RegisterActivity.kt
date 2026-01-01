package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.medicare.R
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    private val BASE_URL = "http://10.0.2.2:3000/api/auth"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val registerBtn = findViewById<Button>(R.id.btnRegister)
        val loginTxt = findViewById<TextView>(R.id.tvLogin)

        registerBtn.setOnClickListener {
            val emailStr = email.text.toString().trim()
            val passStr = password.text.toString().trim()

            if (emailStr.isEmpty() || passStr.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = JSONObject()
            body.put("email", emailStr)
            body.put("password", passStr)

            val request = JsonObjectRequest(
                Request.Method.POST,
                "$BASE_URL/register",
                body,
                {
                    Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                },
                { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )

            Volley.newRequestQueue(this).add(request)
        }

        loginTxt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
