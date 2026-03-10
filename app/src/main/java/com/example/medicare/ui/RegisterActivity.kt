package com.example.medicare.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var client: OkHttpClient

    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        client = OkHttpClient()

        val etName = findViewById<EditText>(R.id.etName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etOtp = findViewById<EditText>(R.id.etOtp)

        val btnSendOtp = findViewById<Button>(R.id.btnSendOtp)
        val btnVerifyOtp = findViewById<Button>(R.id.btnVerifyOtp)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        btnSendOtp.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.length < 10) {
                toast("Enter valid phone number")
                return@setOnClickListener
            }
            sendOtp(phone, etOtp, btnVerifyOtp)
        }

        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString()
            verifyOtp(
                otp,
                etName.text.toString(),
                etEmail.text.toString(),
                etPhone.text.toString()
            )
        }

        btnGoogle.setOnClickListener {
            startActivity(Intent(this, LoginRegisterActivity::class.java))
            finish()
        }
    }

    private fun sendOtp(phone: String, etOtp: EditText, btnVerify: Button) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phone,
            60,
            TimeUnit.SECONDS,
            this,
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

                override fun onVerificationFailed(e: FirebaseException) {
                    toast("OTP failed")
                }

                override fun onCodeSent(
                    id: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = id
                    etOtp.visibility = EditText.VISIBLE
                    btnVerify.visibility = Button.VISIBLE
                    toast("OTP sent")
                }
            }
        )
    }

    private fun verifyOtp(name: String, email: String, phone: String, otp: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
        auth.signInWithCredential(credential).addOnSuccessListener {
            uploadUserToApi(name, email, phone)
        }.addOnFailureListener {
            toast("OTP verification failed")
        }
    }

    private fun uploadUserToApi(name: String, email: String, phone: String) {
        val json = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("provider", "phone")
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://medi-care-roan.vercel.app/api/auth/register")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("API error") }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    toast("Registration successful")
                    startActivity(Intent(this@RegisterActivity, DashboardActivity::class.java))
                    finish()
                }
            }
        })
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}