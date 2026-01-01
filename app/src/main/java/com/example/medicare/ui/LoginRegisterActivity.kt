package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.google.firebase.auth.FirebaseAuth

class LoginRegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()

        // Auto-login check
        if (auth.currentUser != null) {
            goToDashboard()
            return
        }

        val title = findViewById<TextView>(R.id.tvTitle)
        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val actionBtn = findViewById<Button>(R.id.btnAction)
        val toggleTxt = findViewById<TextView>(R.id.tvToggle)
        val loader = findViewById<ProgressBar>(R.id.progressLoader)

        fun updateUI() {
            if (isLoginMode) {
                title.text = "Login to MediCare"
                actionBtn.text = "Login"
                toggleTxt.text = "New here? Create account"
            } else {
                title.text = "Create MediCare Account"
                actionBtn.text = "Register"
                toggleTxt.text = "Already have an account? Login"
            }
        }

        updateUI()

        toggleTxt.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUI()
        }

        actionBtn.setOnClickListener {
            val emailTxt = email.text.toString().trim()
            val passTxt = password.text.toString().trim()

            if (emailTxt.isEmpty() || passTxt.length < 6) {
                Toast.makeText(
                    this,
                    "Enter valid email & password (min 6 chars)",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            loader.visibility = View.VISIBLE
            actionBtn.isEnabled = false

            if (isLoginMode) {
                auth.signInWithEmailAndPassword(emailTxt, passTxt)
                    .addOnCompleteListener {
                        loader.visibility = View.GONE
                        actionBtn.isEnabled = true
                        if (it.isSuccessful) {
                            goToDashboard()
                        } else {
                            Toast.makeText(
                                this,
                                it.exception?.localizedMessage ?: "Login failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                auth.createUserWithEmailAndPassword(emailTxt, passTxt)
                    .addOnCompleteListener {
                        loader.visibility = View.GONE
                        actionBtn.isEnabled = true
                        if (it.isSuccessful) {
                            goToDashboard()
                        } else {
                            Toast.makeText(
                                this,
                                it.exception?.localizedMessage ?: "Registration failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
        }
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
