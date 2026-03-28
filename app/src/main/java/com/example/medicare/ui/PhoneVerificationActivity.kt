package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.Api
import com.example.medicare.data.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class PhoneVerificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROVIDER = "provider"
        const val EXTRA_NAME = "name"
        const val EXTRA_IS_NEW_USER = "isNewUser"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    private var verificationId: String? = null

    private lateinit var etPhone: TextInputEditText
    private lateinit var tilOtp: TextInputLayout
    private lateinit var etOtp: TextInputEditText
    private lateinit var btnSendOtp: MaterialButton
    private lateinit var btnVerifyOtp: MaterialButton
    private lateinit var loader: CircularProgressIndicator

    private val provider: String by lazy { intent.getStringExtra(EXTRA_PROVIDER) ?: "password" }
    private val nameOverride: String? by lazy { intent.getStringExtra(EXTRA_NAME) }
    private val isNewUser: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_NEW_USER, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_verification)

        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager(this)

        if (auth.currentUser == null) {
            goToAuth()
            return
        }

        etPhone = findViewById(R.id.etPhone)
        tilOtp = findViewById(R.id.tilOtp)
        etOtp = findViewById(R.id.etOtp)
        btnSendOtp = findViewById(R.id.btnSendOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        loader = findViewById(R.id.progressLoader)

        if (etPhone.text.isNullOrBlank()) {
            etPhone.setText("+91")
        }

        btnSendOtp.setOnClickListener { sendOtp() }
        btnVerifyOtp.setOnClickListener { verifyOtp() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isNewUser) {
                    cleanupNewUserAndExit()
                } else {
                    finish()
                }
            }
        })
    }

    private fun sendOtp() {
        val phone = normalizePhone(etPhone.text?.toString().orEmpty())
        if (phone == null) {
            toast("Enter a valid phone number")
            return
        }

        setLoading(true)
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    linkAndFinalize(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    setLoading(false)
                    toast(e.localizedMessage ?: "OTP verification failed")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@PhoneVerificationActivity.verificationId = verificationId
                    tilOtp.visibility = View.VISIBLE
                    btnVerifyOtp.visibility = View.VISIBLE
                    setLoading(false)
                    toast("OTP sent")
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOtp() {
        val id = verificationId
        if (id.isNullOrBlank()) {
            toast("Send OTP first")
            return
        }

        val otp = etOtp.text?.toString().orEmpty().trim()
        if (otp.length < 4) {
            toast("Enter a valid OTP")
            return
        }

        val credential = PhoneAuthProvider.getCredential(id, otp)
        linkAndFinalize(credential)
    }

    private fun linkAndFinalize(credential: PhoneAuthCredential) {
        val current = auth.currentUser ?: run {
            toast("Session expired. Please login again.")
            goToAuth()
            return
        }

        setLoading(true)
        current.linkWithCredential(credential)
            .addOnSuccessListener { result ->
                val linkedUser = result.user ?: current
                val verifiedPhone = linkedUser.phoneNumber ?: normalizePhone(etPhone.text?.toString().orEmpty())
                Api.upsertUserProfile(
                    user = linkedUser,
                    provider = provider,
                    nameOverride = nameOverride,
                    phoneOverride = verifiedPhone
                ) { success, message ->
                    if (success) {
                        sessionManager.saveSession(
                            user = linkedUser,
                            provider = provider,
                            nameOverride = nameOverride,
                            phoneOverride = verifiedPhone
                        )
                        goToDashboard()
                    } else {
                        setLoading(false)
                        toast(message ?: "Failed to create account profile")
                    }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast(e.localizedMessage ?: "Phone verification failed")
            }
    }

    private fun setLoading(isLoading: Boolean) {
        loader.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSendOtp.isEnabled = !isLoading
        btnVerifyOtp.isEnabled = !isLoading
    }

    private fun cleanupNewUserAndExit() {
        val user = auth.currentUser
        setLoading(true)
        user?.delete()
            ?.addOnCompleteListener {
                auth.signOut()
                sessionManager.clear()
                setLoading(false)
                goToAuth()
            } ?: run {
            auth.signOut()
            sessionManager.clear()
            setLoading(false)
            goToAuth()
        }
    }

    private fun normalizePhone(raw: String): String? {
        val trimmed = raw.trim().replace(" ", "")
        if (trimmed.startsWith("+") && trimmed.length >= 11) return trimmed
        val digits = trimmed.filter { it.isDigit() }
        return if (digits.length == 10) "+91$digits" else null
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun goToAuth() {
        val intent = Intent(this, LoginRegisterActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
