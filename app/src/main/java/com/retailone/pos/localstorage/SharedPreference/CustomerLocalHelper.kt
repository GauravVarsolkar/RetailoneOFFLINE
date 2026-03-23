package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retailone.pos.models.GetCustomerModel.CustomerData

class CustomerLocalHelper(context: Context) {

    // ✅ MUST use applicationContext
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            "CustomerDB",
            Context.MODE_PRIVATE
        )

    private val gson = Gson()
    private val CUSTOMER_LIST_KEY = "customer_list"

    // ✅ SAVE / UPDATE ONE CUSTOMER (DO NOT OVERWRITE)
    fun saveCustomer(customer: CustomerData) {
        val customers = getCustomers().toMutableList()

        val index = customers.indexOfFirst { it.id == customer.id }

        if (index != -1) {
            customers[index] = customer
        } else {
            customers.add(customer)
        }

        prefs.edit()
            .putString(CUSTOMER_LIST_KEY, gson.toJson(customers))
            .apply()
    }

    // ⚠️ ONLY use if API returns FULL LIST
    fun saveCustomers(customers: List<CustomerData>) {
        prefs.edit()
            .putString(CUSTOMER_LIST_KEY, gson.toJson(customers))
            .apply()
    }

    fun getCustomers(): List<CustomerData> {
        val json = prefs.getString(CUSTOMER_LIST_KEY, null) ?: return emptyList()
        return gson.fromJson(
            json,
            object : TypeToken<List<CustomerData>>() {}.type
        )
    }

    fun findCustomer(query: String): CustomerData? {
        return getCustomers().firstOrNull {
            it.mobile_no == query || it.tin_tpin_no == query
        }
    }
    fun saveLastLoggedInCustomer(customer: CustomerData) {
        val json = gson.toJson(customer)
        prefs.edit()
            .putString("last_logged_in_customer", json)
            .apply()
    }

    fun getLastLoggedInCustomer(): CustomerData? {
        val json = prefs.getString("last_logged_in_customer", null)
        return if (json.isNullOrEmpty()) null
        else gson.fromJson(json, CustomerData::class.java)
    }


}
