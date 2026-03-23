package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences

/**
 * Handles OFFLINE login based on the last successful ONLINE login.
 *
 * Rule:
 * - User must have logged in ONLINE at least once on this device
 * - Offline login is allowed ONLY for that same user
 *
 * This is intentional and POS-safe.
 */
class OfflineLoginHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    companion object {
        private const val PREF_NAME = "OfflineLogin"
        private const val KEY_CUSTOMER_ID = "customer_id"
        private const val KEY_CUSTOMER_MOBILE = "customer_mobile"
    }

    /**
     * Call this ONLY after ONLINE login success
     */
    fun saveLogin(customerId: Int, mobile: String?) {
        prefs.edit()
            .putInt(KEY_CUSTOMER_ID, customerId)
            .putString(KEY_CUSTOMER_MOBILE, mobile)
            .apply()
    }

    /**
     * Check whether OFFLINE login is allowed
     */
    fun canLoginOffline(inputMobile: String?): Boolean {
        if (inputMobile.isNullOrEmpty()) return false

        val savedMobile = prefs.getString(KEY_CUSTOMER_MOBILE, null)
        return savedMobile != null && savedMobile == inputMobile
    }


    /**
     * Get customerId for offline session restoration
     */
    fun getCustomerId(): Int {
        return prefs.getInt(KEY_CUSTOMER_ID, -1)
    }

    /**
     * Clear offline login data (call on logout)
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
