package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var btnGoogle: Button
    private lateinit var loader: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AUTO-LOGIN CHECK
        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return // Prevent further execution
        }

        setContentView(R.layout.activity_login)

        // MediCare Auth
        auth = FirebaseAuth.getInstance()

        btnGoogle = findViewById(R.id.btnGoogleLogin)
        loader = findViewById(R.id.progressLoader)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogle.setOnClickListener {
            showLoader(true)
            startActivityForResult(
                googleSignInClient.signInIntent,
                101
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                showLoader(false)
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoader(false)
                if (task.isSuccessful) {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage ?: "Unknown error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun showLoader(show: Boolean) {
        loader.visibility = if (show) View.VISIBLE else View.GONE
        btnGoogle.isEnabled = !show
    }
}