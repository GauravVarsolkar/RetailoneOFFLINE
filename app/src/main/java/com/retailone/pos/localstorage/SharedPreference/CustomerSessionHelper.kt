package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences

class CustomerSessionHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("CustomerSession", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOMER_ID = "logged_in_customer_id"
        private const val KEY_CUSTOMER_NAME = "logged_in_customer_name"
        private const val KEY_CUSTOMER_MOBILE = "logged_in_customer_mobile"
    }

    fun saveLoggedInCustomer(
        customerId: Int,
        customerName: String?,
        mobile: String?
    ) {
        prefs.edit()
            .putInt(KEY_CUSTOMER_ID, customerId)
            .putString(KEY_CUSTOMER_NAME, customerName ?: "")
            .putString(KEY_CUSTOMER_MOBILE, mobile ?: "")
            .apply()
    }

    fun getCustomerId(): Int {
        return prefs.getInt(KEY_CUSTOMER_ID, -1)
    }

    fun getCustomerName(): String {
        return prefs.getString(KEY_CUSTOMER_NAME, "") ?: ""
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
