package com.example.medicare.data

import android.content.Context
import com.google.firebase.auth.FirebaseUser

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(
        user: FirebaseUser,
        provider: String,
        nameOverride: String? = null,
        phoneOverride: String? = null
    ) {
        prefs.edit()
            .putString(KEY_UID, user.uid)
            .putString(KEY_PROVIDER, provider)
            .putString(KEY_NAME, nameOverride ?: user.displayName ?: user.email?.substringBefore("@") ?: "User")
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, phoneOverride ?: user.phoneNumber)
            .commit()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    fun getPhoneForUid(uid: String): String? {
        return if (prefs.getString(KEY_UID, null) == uid) prefs.getString(KEY_PHONE, null) else null
    }

    fun getNameForUid(uid: String): String? {
        return if (prefs.getString(KEY_UID, null) == uid) prefs.getString(KEY_NAME, null) else null
    }

    companion object {
        private const val PREFS_NAME = "medicare_session"
        private const val KEY_UID = "uid"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
    }
}
