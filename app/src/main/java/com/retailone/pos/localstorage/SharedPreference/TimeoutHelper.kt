package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences

class TimeoutHelper(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    private val SESSION_TIMESTAMP_KEY = "session_timestamp"

    fun saveSessionTimestamp() {
        val timestamp = System.currentTimeMillis()
        val editor = sharedPreferences.edit()
        editor.putLong(SESSION_TIMESTAMP_KEY, timestamp)
        editor.apply()
    }

    fun getSessionTimestamp(): Long {
        return sharedPreferences.getLong(SESSION_TIMESTAMP_KEY, 0)
    }

    fun isSessionValid(): Boolean {
        val sessionTimestamp = getSessionTimestamp()
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - sessionTimestamp
        val millisecondsIn24Hours = 24 * 60 * 60 * 1000
       // val millisecondsIn24Hours =10000

        return timeDifference < millisecondsIn24Hours
    }

    fun clearSession() {
        val editor = sharedPreferences.edit()
        editor.remove(SESSION_TIMESTAMP_KEY)
        editor.apply()
    }
}
