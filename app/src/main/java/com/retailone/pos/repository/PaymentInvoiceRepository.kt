package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.localstorage.RoomDB.PaymentInvoiceDao
import com.retailone.pos.localstorage.RoomDB.PaymentInvoiceEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.InvoiceRes

class PaymentInvoiceRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: PaymentInvoiceDao = database.paymentInvoiceDao()
    private val gson = Gson()

    companion object {
        private const val TAG = "PaymentInvoiceRepo"
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
    }

    /**
     * Save payment invoice data to local database
     */
    suspend fun savePaymentInvoice(storeId: Int, fromDate: String, toDate: String, invoiceRes: InvoiceRes) {
        try {
            val entity = PaymentInvoiceEntity(
                store_id = storeId,
                from_date = fromDate,
                to_date = toDate,
                invoice_data_json = gson.toJson(invoiceRes),
                invoices_paid = invoiceRes.data.invoices_paid,
                invoices_unpaid = invoiceRes.data.invoices_unpaid,
                payments_due = invoiceRes.data.payments_due,
                payments_received = invoiceRes.data.payments_received,
                total_invoice_amount = invoiceRes.data.total_invoice_amount
            )

            // Delete existing entry for same store and date range, then insert new
            dao.deletePaymentInvoice(storeId, fromDate, toDate)
            dao.insertPaymentInvoice(entity)
            Log.d(TAG, "✅ Saved payment invoice: store=$storeId, from=$fromDate, to=$toDate")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving payment invoice: ${e.message}", e)
        }
    }

    /**
     * Get payment invoice by store ID and date range (for offline viewing)
     */
    suspend fun getPaymentInvoiceByDateRange(storeId: Int, fromDate: String, toDate: String): InvoiceRes? {
        return try {
            val entity = dao.getPaymentInvoiceByDateRange(storeId, fromDate, toDate)
            if (entity != null) {
                gson.fromJson(entity.invoice_data_json, InvoiceRes::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving payment invoice: ${e.message}", e)
            null
        }
    }

    /**
     * Get latest payment invoice for a store (useful for showing recent data offline)
     */
    suspend fun getLatestPaymentInvoice(storeId: Int): InvoiceRes? {
        return try {
            val entity = dao.getLatestPaymentInvoice(storeId)
            if (entity != null) {
                gson.fromJson(entity.invoice_data_json, InvoiceRes::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving latest payment invoice: ${e.message}", e)
            null
        }
    }

    /**
     * Check if payment invoice exists in database
     */
    suspend fun paymentInvoiceExists(storeId: Int, fromDate: String, toDate: String): Boolean {
        return dao.paymentInvoiceExists(storeId, fromDate, toDate)
    }

    /**
     * Delete payment invoices older than 7 days
     */
    suspend fun deleteOldPaymentInvoices(): Int {
        val sevenDaysAgo = System.currentTimeMillis() - SEVEN_DAYS_MILLIS
        val deletedCount = dao.deletePaymentInvoicesOlderThan(sevenDaysAgo)
        Log.d(TAG, "🗑️ Deleted $deletedCount old payment invoices")
        return deletedCount
    }

    /**
     * Delete all payment invoices for a store
     */
    suspend fun deleteAllByStoreId(storeId: Int): Int {
        val deletedCount = dao.deleteAllByStoreId(storeId)
        Log.d(TAG, "🗑️ Deleted $deletedCount payment invoices for store $storeId")
        return deletedCount
    }

    /**
     * Clear all payment invoices
     */
    suspend fun clearAllPaymentInvoices() {
        dao.clearAll()
        Log.d(TAG, "🗑️ Cleared all payment invoices")
    }
}
