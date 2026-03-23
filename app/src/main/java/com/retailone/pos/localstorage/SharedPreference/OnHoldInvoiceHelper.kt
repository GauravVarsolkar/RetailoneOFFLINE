package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences

object OnHoldInvoiceHelper {
    private const val PREF_NAME = "on_hold_invoices"
    private const val KEY_INVOICE_SET = "invoice_set"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Mark an invoice as ON HOLD
     */
    fun markAsOnHold(context: Context, invoiceId: String) {
        val prefs = getPrefs(context)
        val existing = prefs.getStringSet(KEY_INVOICE_SET, mutableSetOf()) ?: mutableSetOf()
        val updated = existing.toMutableSet()
        updated.add(invoiceId)
        prefs.edit().putStringSet(KEY_INVOICE_SET, updated).apply()
    }

    /**
     * Check if an invoice is ON HOLD
     */
    fun isOnHold(context: Context, invoiceId: String): Boolean {
        val prefs = getPrefs(context)
        val set = prefs.getStringSet(KEY_INVOICE_SET, mutableSetOf()) ?: mutableSetOf()
        return set.contains(invoiceId)
    }

    /**
     * Remove an invoice from ON HOLD (optional - for future use)
     */
    fun removeOnHold(context: Context, invoiceId: String) {
        val prefs = getPrefs(context)
        val existing = prefs.getStringSet(KEY_INVOICE_SET, mutableSetOf()) ?: mutableSetOf()
        val updated = existing.toMutableSet()
        updated.remove(invoiceId)
        prefs.edit().putStringSet(KEY_INVOICE_SET, updated).apply()
    }

    /**
     * Clear all ON HOLD invoices (optional - for testing)
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
