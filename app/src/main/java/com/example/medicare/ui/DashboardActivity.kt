package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.medicare.R
import com.example.medicare.data.Api
import com.example.medicare.data.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val sessionManager = SessionManager(this)
        if (user == null) {
            startActivity(Intent(this, LoginRegisterActivity::class.java))
            finish()
            return
        }

        val cachedPhone = sessionManager.getPhoneForUid(user.uid)
        if (user.phoneNumber.isNullOrBlank() && cachedPhone.isNullOrBlank()) {
            Api.fetchUserProfile(uid = user.uid, email = user.email) { profile, _ ->
                if (!profile?.phone.isNullOrBlank()) {
                    sessionManager.saveSession(
                        user = user,
                        provider = inferProvider(user),
                        nameOverride = profile?.name,
                        phoneOverride = profile?.phone
                    )
                    recreate()
                } else {
                    val intent = Intent(this, PhoneVerificationActivity::class.java)
                    intent.putExtra(PhoneVerificationActivity.EXTRA_PROVIDER, inferProvider(user))
                    intent.putExtra(PhoneVerificationActivity.EXTRA_NAME, profile?.name ?: user.displayName)
                    intent.putExtra(PhoneVerificationActivity.EXTRA_IS_NEW_USER, false)
                    startActivity(intent)
                    finish()
                }
            }
            return
        }

        setContentView(R.layout.activity_dashboard)

        val displayName = sessionManager.getNameForUid(user.uid)
            ?: user.displayName
            ?: user.email?.substringBefore("@")
            ?: "User"
        val initial = displayName.firstOrNull()?.uppercase() ?: "U"

        findViewById<TextView>(R.id.tvWelcome)?.text = "Welcome back"
        findViewById<TextView>(R.id.tvUserName)?.text = displayName
        findViewById<TextView>(R.id.tvHeroSubtitle)?.text = "Your health, appointments, and essentials in one place."
        findViewById<TextView>(R.id.tvUserInitial)?.text = initial

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
            GoogleSignIn.getClient(
                this,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()
            sessionManager.clear()
            val intent = Intent(this, LoginRegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun inferProvider(user: com.google.firebase.auth.FirebaseUser): String {
        return when {
            user.providerData.any { it.providerId == "google.com" } -> "google"
            user.providerData.any { it.providerId == "phone" } -> "phone"
            else -> "password"
        }
    }
}
