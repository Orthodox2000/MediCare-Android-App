package com.example.medicare.data

import android.os.Handler
import android.os.Looper
import com.example.medicare.util.Iso8601
import com.google.firebase.auth.FirebaseUser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object Api {
    private const val BASE_URL = "https://medi-care-roan.vercel.app"
    private val jsonMediaType = "application/json".toMediaType()
    private val mainHandler = Handler(Looper.getMainLooper())

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    data class UserProfile(
        val uid: String?,
        val name: String?,
        val email: String?,
        val phone: String?,
        val provider: String?,
        val photo: String?
    )

    fun fetchUserProfile(
        uid: String? = null,
        email: String? = null,
        phone: String? = null,
        onResult: (profile: UserProfile?, message: String?) -> Unit
    ) {
        val urlBuilder = "$BASE_URL/api/users".toHttpUrl().newBuilder()
        uid?.takeIf { it.isNotBlank() }?.let { urlBuilder.addQueryParameter("uid", it) }
        email?.takeIf { it.isNotBlank() }?.let { urlBuilder.addQueryParameter("email", it) }
        phone?.takeIf { it.isNotBlank() }?.let { urlBuilder.addQueryParameter("phone", it) }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val details = e.localizedMessage?.takeIf { it.isNotBlank() }
                mainHandler.post { onResult(null, details ?: "Network error") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val message = if (response.code == 404) null else "Server error (${response.code})"
                        mainHandler.post { onResult(null, message) }
                        return
                    }

                    val bodyString = response.body?.string().orEmpty()
                    if (bodyString.isBlank()) {
                        mainHandler.post { onResult(null, null) }
                        return
                    }

                    val root = JSONObject(bodyString)
                    val exists = root.optBoolean("exists", false)
                    val data = when {
                        root.has("data") && !root.isNull("data") -> root.optJSONObject("data")
                        root.has("uid") || root.has("email") || root.has("phone") -> root
                        else -> null
                    }

                    if (!exists && data == null) {
                        mainHandler.post { onResult(null, null) }
                        return
                    }

                    val profile = data?.let {
                        UserProfile(
                            uid = it.optNullableString("uid"),
                            name = it.optNullableString("name"),
                            email = it.optNullableString("email"),
                            phone = it.optNullableString("phone"),
                            provider = it.optNullableString("provider"),
                            photo = it.optNullableString("photo")
                        )
                    }

                    mainHandler.post { onResult(profile, null) }
                }
            }
        })
    }

    fun upsertUserProfile(
        user: FirebaseUser,
        provider: String,
        nameOverride: String? = null,
        phoneOverride: String? = null,
        onResult: (success: Boolean, message: String?) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("uid", user.uid)
            put("name", nameOverride ?: user.displayName ?: "User")
            put("email", user.email)
            put("phone", phoneOverride ?: user.phoneNumber)
            put("provider", provider)
            put("photo", user.photoUrl?.toString())
            put("createdAt", Iso8601.fromEpochMillis(user.metadata?.creationTimestamp))
        }

        val request = Request.Builder()
            .url("$BASE_URL/api/users")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val details = e.localizedMessage?.takeIf { it.isNotBlank() }
                mainHandler.post { onResult(false, details ?: "Network error") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val ok = response.isSuccessful
                    val msg = if (ok) null else "Server error (${response.code})"
                    mainHandler.post { onResult(ok, msg) }
                }
            }
        })
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }
}
