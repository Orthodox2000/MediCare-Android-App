package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.Api
import com.example.medicare.data.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest

class LoginRegisterActivity : AppCompatActivity() {
    private enum class AuthStage(val message: String) {
        IDLE(""),
        EMAIL_LOGIN("Signing in..."),
        EMAIL_REGISTER("Creating your account..."),
        GOOGLE_PREPARE("Preparing Google sign-in..."),
        GOOGLE_ACCOUNT("Waiting for Google account selection..."),
        GOOGLE_FIREBASE("Signing in with Google..."),
        PROFILE_FETCH("Checking your profile..."),
        PROFILE_SYNC("Syncing your account...")
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleLauncher: ActivityResultLauncher<Intent>

    private var isLoginMode = true

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tilName: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnAction: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var tvToggle: TextView
    private lateinit var loader: CircularProgressIndicator
    private lateinit var tvAuthStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager(this)

        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tilName = findViewById(R.id.tilName)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnAction = findViewById(R.id.btnAction)
        btnGoogle = findViewById(R.id.btnGoogleLogin)
        tvToggle = findViewById(R.id.tvToggle)
        loader = findViewById(R.id.progressLoader)
        tvAuthStatus = findViewById(R.id.tvAuthStatus)

        // Auto-login (but enforce phone verification + profile upsert)
        auth.currentUser?.let { user ->
            updateAuthStage(AuthStage.PROFILE_FETCH)
            user.reload()
                .addOnSuccessListener {
                    val inferredProvider = inferProvider(user.providerData.map { it.providerId })
                    resolveAuthenticatedUser(
                        user = user,
                        provider = inferredProvider,
                        nameOverride = user.displayName,
                        isNewUser = false
                    )
                }
                .addOnFailureListener {
                    auth.signOut()
                    updateAuthStage(AuthStage.IDLE)
                }
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            updateAuthStage(AuthStage.GOOGLE_FIREBASE)
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    failAuth("Google login did not return an ID token. Check Firebase Google sign-in setup.")
                } else {
                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                failAuth(googleSignInErrorMessage(e.statusCode, e.localizedMessage))
            } catch (e: Exception) {
                failAuth(e.localizedMessage ?: "Google login failed")
            }
        }

        fun updateUI() {
            if (isLoginMode) {
                tvTitle.text = "Welcome back"
                tvSubtitle.text = "Sign in to continue"
                tilName.visibility = View.GONE
                btnAction.text = "Login"
                tvToggle.text = "New here? Create account"
            } else {
                tvTitle.text = "Create account"
                tvSubtitle.text = "Verify your phone to finish setup"
                tilName.visibility = View.VISIBLE
                btnAction.text = "Register"
                tvToggle.text = "Already have an account? Login"
            }
        }

        updateUI()

        tvToggle.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUI()
        }

        btnAction.setOnClickListener {
            val email = etEmail.text?.toString().orEmpty().trim()
            val password = etPassword.text?.toString().orEmpty().trim()
            val name = etName.text?.toString().orEmpty().trim()

            if (email.isEmpty() || password.length < 6) {
                toast("Invalid email or password")
                return@setOnClickListener
            }
            if (!isLoginMode && name.isEmpty()) {
                toast("Enter your name")
                return@setOnClickListener
            }

            updateAuthStage(if (isLoginMode) AuthStage.EMAIL_LOGIN else AuthStage.EMAIL_REGISTER)
            if (isLoginMode) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { handlePostAuth(provider = "password", nameOverride = null, isNewUser = false) }
                    .addOnFailureListener { e ->
                        failAuth(e.localizedMessage ?: "Login failed")
                    }
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        auth.currentUser?.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()
                        )?.addOnCompleteListener {
                            handlePostAuth(provider = "password", nameOverride = name, isNewUser = true)
                        }
                    }
                    .addOnFailureListener { e ->
                        failAuth(e.localizedMessage ?: "Registration failed")
                    }
            }
        }

        btnGoogle.setOnClickListener {
            updateAuthStage(AuthStage.GOOGLE_ACCOUNT)
            googleLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result.additionalUserInfo?.isNewUser == true
                    handlePostAuth(provider = "google", nameOverride = auth.currentUser?.displayName, isNewUser = isNewUser)
                } else {
                    val error = task.exception?.message ?: "Unknown error"
                    failAuth("Google auth failed: $error")
                }
            }
    }

    private fun handlePostAuth(provider: String, nameOverride: String?, isNewUser: Boolean) {
        val user = auth.currentUser
        if (user == null) {
            failAuth("Session error. Try again.")
            return
        }

        resolveAuthenticatedUser(user = user, provider = provider, nameOverride = nameOverride, isNewUser = isNewUser)
    }

    private fun resolveAuthenticatedUser(
        user: FirebaseUser,
        provider: String,
        nameOverride: String?,
        isNewUser: Boolean
    ) {
        val cachedPhone = sessionManager.getPhoneForUid(user.uid)
        val resolvedName = nameOverride ?: sessionManager.getNameForUid(user.uid) ?: user.displayName

        if (!user.phoneNumber.isNullOrBlank() || !cachedPhone.isNullOrBlank()) {
            updateAuthStage(AuthStage.PROFILE_SYNC)
            syncProfileAndEnter(
                user = user,
                provider = provider,
                nameOverride = resolvedName,
                phoneOverride = user.phoneNumber ?: cachedPhone,
                allowDashboardOnFailure = true
            )
            return
        }

        if (isNewUser) {
            setLoading(false)
            tvAuthStatus.text = "Sign-in complete. Phone verification required next."
            tvAuthStatus.visibility = View.VISIBLE
            goToPhoneVerification(provider = provider, nameOverride = resolvedName, isNewUser = true)
            return
        }

        updateAuthStage(AuthStage.PROFILE_FETCH)
        Api.fetchUserProfile(uid = user.uid, email = user.email) { profile, message ->
            if (!profile?.phone.isNullOrBlank()) {
                updateAuthStage(AuthStage.PROFILE_SYNC)
                syncProfileAndEnter(
                    user = user,
                    provider = provider,
                    nameOverride = profile?.name ?: resolvedName,
                    phoneOverride = profile?.phone,
                    allowDashboardOnFailure = true
                )
            } else {
                setLoading(false)
                tvAuthStatus.text = message ?: "Signed in. Phone verification required to continue."
                tvAuthStatus.visibility = View.VISIBLE
                goToPhoneVerification(provider = provider, nameOverride = resolvedName, isNewUser = false)
            }
        }
    }

    private fun syncProfileAndEnter(
        user: FirebaseUser,
        provider: String,
        nameOverride: String?,
        phoneOverride: String?,
        allowDashboardOnFailure: Boolean
    ) {
        updateAuthStage(AuthStage.PROFILE_SYNC)
        Api.upsertUserProfile(
            user = user,
            provider = provider,
            nameOverride = nameOverride,
            phoneOverride = phoneOverride
        ) { ok, msg ->
            if (ok || allowDashboardOnFailure) {
                sessionManager.saveSession(
                    user = user,
                    provider = provider,
                    nameOverride = nameOverride,
                    phoneOverride = phoneOverride
                )
                goToDashboard()
            } else {
                failAuth(msg ?: "Could not sync profile")
            }
        }
    }

    private fun updateAuthStage(stage: AuthStage) {
        if (stage == AuthStage.IDLE) {
            setLoading(false)
            tvAuthStatus.visibility = View.GONE
            tvAuthStatus.text = ""
            return
        }

        setLoading(true)
        tvAuthStatus.text = stage.message
        tvAuthStatus.visibility = View.VISIBLE
    }

    private fun failAuth(message: String) {
        setLoading(false)
        tvAuthStatus.text = message
        tvAuthStatus.visibility = View.VISIBLE
        toast(message)
    }

    private fun googleSignInErrorMessage(statusCode: Int, fallback: String?): String {
        return when (statusCode) {
            CommonStatusCodes.CANCELED, 12501 ->
                "Google sign-in was cancelled."
            10 ->
                "Google sign-in config error (code 10). Add SHA-1/SHA-256 in Firebase for com.example.medicare, re-download google-services.json, then rebuild."
            12500 ->
                "Google sign-in failed (12500). Check Firebase Google provider and OAuth setup."
            CommonStatusCodes.NETWORK_ERROR ->
                "Network error during Google sign-in. Check connection and try again."
            CommonStatusCodes.SIGN_IN_REQUIRED ->
                "Please select a Google account to continue."
            else ->
                "Google sign-in failed ($statusCode). ${fallback ?: "Try again."}"
        }
    }

    private fun setLoading(isLoading: Boolean) {
        loader.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnAction.isEnabled = !isLoading
        btnGoogle.isEnabled = !isLoading
        tvToggle.isEnabled = !isLoading
    }

    private fun inferProvider(providerIds: List<String>): String {
        return when {
            providerIds.any { it == "google.com" } -> "google"
            providerIds.any { it == "phone" } -> "phone"
            else -> "password"
        }
    }

    private fun goToPhoneVerification(provider: String, nameOverride: String? = null, isNewUser: Boolean) {
        val intent = Intent(this, PhoneVerificationActivity::class.java)
        intent.putExtra(PhoneVerificationActivity.EXTRA_PROVIDER, provider)
        intent.putExtra(PhoneVerificationActivity.EXTRA_NAME, nameOverride)
        intent.putExtra(PhoneVerificationActivity.EXTRA_IS_NEW_USER, isNewUser)
        startActivity(intent)
        finish()
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
