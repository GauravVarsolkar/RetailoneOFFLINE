package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseCategory.ExpenseCategoryRes
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseVendor.ExpenseVendorRes
import com.retailone.pos.models.PettycashReportModel.PettycashReportRes

class ExpenseCacheHelper(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ExpenseCache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveCategories(data: ExpenseCategoryRes) {
        sharedPreferences.edit().putString("categories", gson.toJson(data)).apply()
    }

    fun getCategories(): ExpenseCategoryRes? {
        val json = sharedPreferences.getString("categories", null) ?: return null
        return gson.fromJson(json, ExpenseCategoryRes::class.java)
    }

    fun saveVendors(data: ExpenseVendorRes) {
        sharedPreferences.edit().putString("vendors", gson.toJson(data)).apply()
    }

    fun getVendors(): ExpenseVendorRes? {
        val json = sharedPreferences.getString("vendors", null) ?: return null
        return gson.fromJson(json, ExpenseVendorRes::class.java)
    }

    fun savePettyCash(data: PettycashReportRes) {
        sharedPreferences.edit().putString("petty_cash", gson.toJson(data)).apply()
    }

    fun getPettyCash(): PettycashReportRes? {
        val json = sharedPreferences.getString("petty_cash", null) ?: return null
        return gson.fromJson(json, PettycashReportRes::class.java)
    }
}
