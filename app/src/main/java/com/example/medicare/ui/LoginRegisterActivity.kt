package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginRegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleLauncher: ActivityResultLauncher<Intent>

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()

        // Auto-login
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
        val googleBtn = findViewById<Button>(R.id.btnGoogleLogin)

        // 🔹 Google Sign-In config (CORRECT)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 🔹 Activity Result API (FIXES DEPRECATION)
        googleLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: Exception) {
                    loader.visibility = View.GONE
                    Toast.makeText(this, "Google login failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                loader.visibility = View.GONE
            }
        }

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
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loader.visibility = View.VISIBLE
            actionBtn.isEnabled = false

            if (isLoginMode) {
                auth.signInWithEmailAndPassword(emailTxt, passTxt)
                    .addOnCompleteListener {
                        loader.visibility = View.GONE
                        actionBtn.isEnabled = true
                        if (it.isSuccessful) goToDashboard()
                        else Toast.makeText(this, it.exception?.message, Toast.LENGTH_LONG).show()
                    }
            } else {
                auth.createUserWithEmailAndPassword(emailTxt, passTxt)
                    .addOnCompleteListener {
                        loader.visibility = View.GONE
                        actionBtn.isEnabled = true
                        if (it.isSuccessful) goToDashboard()
                        else Toast.makeText(this, it.exception?.message, Toast.LENGTH_LONG).show()
                    }
            }
        }

        // 🔹 Google Login Button
        googleBtn.setOnClickListener {

            googleSignInClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser
                    if (user != null) {
                        Toast.makeText(this, "Google Login Success", Toast.LENGTH_SHORT).show()
                        goToDashboard()
                    } else {
                        Toast.makeText(this, "User is null", Toast.LENGTH_LONG).show()
                    }

                } else {

                    val error = task.exception?.message ?: "Unknown error"
                    Toast.makeText(this, "Google Auth Failed: $error", Toast.LENGTH_LONG).show()
                }
            }
    }
    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}