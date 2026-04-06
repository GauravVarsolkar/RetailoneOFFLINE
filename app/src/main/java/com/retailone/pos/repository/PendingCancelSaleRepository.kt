package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.retailone.pos.localstorage.RoomDB.PendingCancelSaleDao
import com.retailone.pos.localstorage.RoomDB.PendingCancelSaleEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.CancelSaleitemRequest
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.Sale
import com.retailone.pos.utils.NetworkUtils

class PendingCancelSaleRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: PendingCancelSaleDao = database.pendingCancelSaleDao()

    companion object {
        private const val TAG = "PendingCancelSaleRepo"
    }

    /**
     * Save a cancel sale request locally (offline)
     */
    suspend fun saveCancelRequest(
        invoiceId: String,
        saleId: Int,
        saleDateTime: String,
        storeId: String,
        grandTotal: String,
        paymentType: String
    ): Boolean {
        return try {
            // ✅ Check if a cancel request already exists for this invoice (avoid duplication)
            if (dao.cancelRequestExists(invoiceId)) {
                Log.d(TAG, "⚠️ Cancel request already exists for invoice: $invoiceId - skipping duplicate")
                return false
            }

            // Also check if a synced cancel request exists (already cancelled)
            if (dao.anyCancelRequestExists(invoiceId)) {
                Log.d(TAG, "⚠️ Cancel request already exists (may be synced) for invoice: $invoiceId")
                return false
            }

            val entity = PendingCancelSaleEntity(
                invoice_id = invoiceId,
                sale_id = saleId,
                sale_date_time = saleDateTime,
                store_id = storeId,
                grand_total = grandTotal,
                payment_type = paymentType,
                sync_status = "PENDING"
            )

            val id = dao.insertPendingCancel(entity)
            Log.d(TAG, "✅ Saved pending cancel request: id=$id, invoice=$invoiceId, sale=$saleId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving cancel request: ${e.message}", e)
            false
        }
    }

    /**
     * Sync a single pending cancel request to server
     */
    suspend fun syncCancelRequest(cancelRequest: PendingCancelSaleEntity): Boolean {
        return try {
            // Mark as syncing
            dao.updateSyncStatus(cancelRequest.id, "SYNCING", System.currentTimeMillis())

            if (!NetworkUtils.isInternetAvailable(context)) {
                Log.d(TAG, "📴 No internet - cancel request ${cancelRequest.invoice_id} will sync later")
                dao.updateSyncStatus(cancelRequest.id, "PENDING", System.currentTimeMillis())
                return false
            }

            // Make API call synchronously
            val apiService = com.retailone.pos.network.ApiClient().getApiService(context)
            val request = CancelSaleitemRequest(invoiceID = cancelRequest.invoice_id)
            val response = apiService.cancelItemAPI(request).execute()

            if (response.isSuccessful && response.body() != null) {
                val res = response.body()!!
                if (res.status == 1) {
                    // Success - mark as synced
                    val reversalInvoiceId = res.data?.reversal_invoice_id ?: ""
                    val reversalSalesId = res.data?.reversal_sales_id ?: 0
                    dao.markAsSynced(
                        cancelRequest.id,
                        System.currentTimeMillis(),
                        reversalInvoiceId,
                        reversalSalesId
                    )
                    Log.d(TAG, "✅ Cancel request ${cancelRequest.invoice_id} synced successfully")
                    true
                } else {
                    // Server returned error
                    val errorMsg = res.message ?: "Server error"
                    dao.updateSyncStatusWithError(
                        cancelRequest.id,
                        "FAILED",
                        System.currentTimeMillis(),
                        errorMsg
                    )
                    Log.e(TAG, "❌ Cancel request ${cancelRequest.invoice_id} server error: $errorMsg")
                    false
                }
            } else {
                // HTTP error
                val errorMsg = response.errorBody()?.string() ?: "HTTP error ${response.code()}"
                dao.updateSyncStatusWithError(
                    cancelRequest.id,
                    "FAILED",
                    System.currentTimeMillis(),
                    errorMsg
                )
                Log.e(TAG, "❌ Cancel request ${cancelRequest.invoice_id} HTTP error: $errorMsg")
                false
            }

        } catch (e: Exception) {
            dao.updateSyncStatusWithError(
                cancelRequest.id,
                "FAILED",
                System.currentTimeMillis(),
                e.message ?: "Unknown error"
            )
            Log.e(TAG, "❌ Cancel request ${cancelRequest.invoice_id} sync error: ${e.message}")
            false
        }
    }

    /**
     * Sync all pending cancel requests to server
     */
    suspend fun syncAllPendingCancels(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val pendingCancels = dao.getPendingCancelsForSync()

            if (pendingCancels.isEmpty()) {
                Log.d(TAG, "No pending cancel requests to sync")
                return@withContext true
            }

            Log.d(TAG, "Found ${pendingCancels.size} pending cancel requests to sync")
            var successCount = 0
            var failCount = 0

            for (cancel in pendingCancels) {
                val result = syncCancelRequest(cancel)
                if (result) {
                    successCount++
                } else {
                    failCount++
                }
            }

            Log.d(TAG, "Cancel sync completed: $successCount success, $failCount failed")
            failCount == 0

        } catch (e: Exception) {
            Log.e(TAG, "Cancel sync error: ${e.message}")
            false
        }
    }

    /**
     * Check if a cancel request is already queued for an invoice
     */
    suspend fun isCancelQueued(invoiceId: String): Boolean {
        return dao.cancelRequestExists(invoiceId)
    }

    /**
     * Get pending cancel count as Flow
     */
    fun getPendingCancelCountFlow(): kotlinx.coroutines.flow.Flow<Int> {
        return dao.getPendingCancelCountFlow()
    }

    /**
     * Get all pending cancel requests
     */
    suspend fun getPendingCancels(): List<PendingCancelSaleEntity> {
        return dao.getPendingCancelsForSync()
    }

    /**
     * ✅ NEW: Get cancelled sales as Sale objects with negative amounts
     * These will be shown in the SalesAndPaymentActivity list
     */
    suspend fun getCancelledSalesAsNegativeSale(startTime: Long? = null, endTime: Long? = null): List<Sale> {
        return try {
            Log.d("OfflineCancelDebug", "🔍 Fetching cancelled sales from DB with startTime=$startTime, endTime=$endTime")
            val pendingCancels = if (startTime != null && endTime != null) {
                dao.getCancelsByDateRange(startTime, endTime)
            } else {
                dao.getAllCancels()
            }
            Log.d("OfflineCancelDebug", "📊 Found ${pendingCancels.size} raw pending cancel records in DB")
            
            val mappedSales = pendingCancels.map { cancel ->
                val amount = -(cancel.grand_total.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0)
                Log.d("OfflineCancelDebug", "🔄 Mapping invoice ${cancel.invoice_id}: grand_total='${cancel.grand_total}' -> amount=$amount")
                Sale(
                    id = cancel.id + 1000000, // ✅ Use positive unique ID for reversal entries (will be flipped to negative by activity)
                    invoice_id = cancel.invoice_id,
                    grand_total = amount, // Negative amount
                    sub_total = amount, 
                    subtotal_after_discount = amount,
                    discount_amount = 0.0,
                    tax = 0.0,
                    tax_amount = 0.0,
                    payment_type = cancel.payment_type,
                    store_id = cancel.store_id.toIntOrNull() ?: 0,
                    store_manager_id = 0,
                    amount_tendered = 0.0,
                    created_at = formatDateForApi(cancel.sale_date_time),
                    updated_at = formatDateForApi(cancel.sale_date_time),
                    status = 2 // Status 2 = Cancelled
                )
            }
            Log.d("OfflineCancelDebug", "✅ Returning ${mappedSales.size} negative Sale objects")
            mappedSales
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting cancelled sales: ${e.message}")
            Log.e("OfflineCancelDebug", "❌ Exception: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Format date from "dd-MMM-yyyy hh:mm a" to "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" for DateTimeFormatting
     */
    private fun formatDateForApi(inputDate: String): String {
        if (inputDate.isEmpty()) return inputDate
        
        // ✅ If already ISO format, don't re-parse
        if (inputDate.contains("T") && inputDate.endsWith("Z")) return inputDate

        return try {
            val localizationData = com.retailone.pos.localstorage.SharedPreference.LocalizationHelper(context).getLocalizationData()
            val zone = localizationData.timezone
            
            // Map store zone to full TimeZone ID
            val timezoneId = when (zone) {
                "IST" -> "Asia/Kolkata"
                "CAT" -> "Africa/Lusaka"
                else -> "Africa/Lusaka"
            }

            val inputFormat = java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a", java.util.Locale.ENGLISH)
            inputFormat.timeZone = java.util.TimeZone.getTimeZone(timezoneId) // ✅ Parse as Store Timezone
            
            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.ENGLISH)
            outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC") // ✅ Output as UTC
            
            val date = inputFormat.parse(inputDate)
            outputFormat.format(date ?: throw Exception("Null date"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error formatting date: $inputDate - ${e.message}")
            inputDate // Return original if parsing fails
        }
    }

    /**
     * ✅ NEW: Check if a sale is cancelled (has a pending cancel request)
     */
    suspend fun isSaleCancelled(saleId: Int): Boolean {
        return try {
            dao.getPendingCancelsForSync().any { it.sale_id == saleId && !it.is_synced }
        } catch (e: Exception) {
            false
        }
    }
}
