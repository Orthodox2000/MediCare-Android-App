package com.example.medicare.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Iso8601 {
    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun fromEpochMillis(epochMillis: Long?): String {
        val safeMillis = epochMillis ?: System.currentTimeMillis()
        return formatter.format(Date(safeMillis))
    }
}

