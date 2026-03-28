package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.Api
import com.example.medicare.data.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            validateAndRouteExistingSession(auth, user)
            return
        }

        attemptGoogleSilentSignIn(auth)
    }

    private fun validateAndRouteExistingSession(auth: FirebaseAuth, user: FirebaseUser) {
        user.reload()
            .addOnSuccessListener {
                routeAuthenticatedUser(user)
            }
            .addOnFailureListener {
                auth.signOut()
                attemptGoogleSilentSignIn(auth)
            }
    }

    private fun attemptGoogleSilentSignIn(auth: FirebaseAuth) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, gso)
        googleClient.silentSignIn()
            .addOnSuccessListener { account ->
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    goToLogin()
                    return@addOnSuccessListener
                }

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        val signedInUser = auth.currentUser
                        if (signedInUser != null) routeAuthenticatedUser(signedInUser) else goToLogin()
                    }
                    .addOnFailureListener {
                        goToLogin()
                    }
            }
            .addOnFailureListener {
                goToLogin()
            }
    }

    private fun routeAuthenticatedUser(user: com.google.firebase.auth.FirebaseUser) {
        val provider = when {
            user.providerData.any { it.providerId == "google.com" } -> "google"
            user.providerData.any { it.providerId == "phone" } -> "phone"
            else -> "password"
        }
        val sessionManager = SessionManager(this)
        val cachedPhone = sessionManager.getPhoneForUid(user.uid)

        if (!user.phoneNumber.isNullOrBlank() || !cachedPhone.isNullOrBlank()) {
            sessionManager.saveSession(
                user = user,
                provider = provider,
                phoneOverride = user.phoneNumber ?: cachedPhone
            )
            goToDashboard()
            return
        }

        Api.fetchUserProfile(uid = user.uid, email = user.email) { profile, _ ->
            if (!profile?.phone.isNullOrBlank()) {
                sessionManager.saveSession(
                    user = user,
                    provider = provider,
                    nameOverride = profile?.name,
                    phoneOverride = profile?.phone
                )
                goToDashboard()
            } else {
                val intent = Intent(this, PhoneVerificationActivity::class.java)
                intent.putExtra(PhoneVerificationActivity.EXTRA_PROVIDER, provider)
                intent.putExtra(PhoneVerificationActivity.EXTRA_NAME, profile?.name ?: user.displayName)
                intent.putExtra(PhoneVerificationActivity.EXTRA_IS_NEW_USER, false)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginRegisterActivity::class.java))
        finish()
    }
}
