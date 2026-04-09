package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.localstorage.RoomDB.PendingReturnDao
import com.retailone.pos.localstorage.RoomDB.PendingReturnEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnSaleReq
import com.retailone.pos.network.ApiClient
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemRes
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PendingReturnRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: PendingReturnDao = database.pendingReturnDao()
    private val gson = Gson()

    companion object {
        private const val TAG = "PendingReturnRepo"
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
    }

    /**
     * Save return request to queue (for offline submission)
     */
    suspend fun queueReturnRequest(invoiceId: String, returnRequest: ReturnSaleReq): Long {
        return try {
            val entity = PendingReturnEntity(
                invoice_id = invoiceId,
                store_id = returnRequest.store_id,
                store_manager_id = returnRequest.store_manager_id,
                reason_id = returnRequest.reason_id,
                sales_id = returnRequest.sales_id,
                return_request_json = gson.toJson(returnRequest),
                sync_status = "PENDING"
            )

            val id = dao.insertPendingReturn(entity)
            Log.d(TAG, "✅ Queued return request (ID: $id) for offline sync")
            id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error queuing return request: ${e.message}", e)
            -1L
        }
    }

    /**
     * Get all pending returns waiting to be synced
     */
    suspend fun getAllPendingReturns(): List<PendingReturnEntity> {
        return dao.getAllPendingReturns()
    }

    /**
     * Get pending returns as Flow (for real-time updates)
     */
    fun getPendingReturnsFlow(): Flow<List<PendingReturnEntity>> {
        return dao.getPendingReturnsFlow()
    }

    /**
     * Get count of pending returns as Flow
     */
    fun getPendingReturnsCountFlow(): Flow<Int> {
        return dao.getPendingReturnsCountFlow()
    }

    /**
     * Get count of pending returns (synchronous/suspend)
     */
    suspend fun getPendingReturnsCount(): Int {
        return dao.getPendingReturnsCount()
    }

    /**
     * Convert PendingReturnEntity back to ReturnSaleReq
     */
    fun entityToReturnRequest(entity: PendingReturnEntity): ReturnSaleReq {
        return gson.fromJson(entity.return_request_json, ReturnSaleReq::class.java)
    }

    /**
     * Mark return as successfully synced
     */
    suspend fun markAsSynced(id: Int) {
        dao.markAsSynced(id)
        Log.d(TAG, "✅ Marked return $id as synced")
    }

    /**
     * Update sync status (SYNCING, FAILED, etc.)
     */
    suspend fun updateSyncStatus(id: Int, status: String, errorMessage: String? = null) {
        dao.updateSyncStatus(id, status, errorMessage)
        Log.d(TAG, "📝 Updated return $id status to: $status")
    }

    /**
     * Delete synced returns older than 7 days
     */
    suspend fun deleteSyncedReturns(): Int {
        val sevenDaysAgo = System.currentTimeMillis() - SEVEN_DAYS_MILLIS
        val deletedCount = dao.deleteSyncedReturnsOlderThan(sevenDaysAgo)
        Log.d(TAG, "🗑️ Deleted $deletedCount old synced returns")
        return deletedCount
    }

    /**
     * Delete a specific pending return
     */
    suspend fun deletePendingReturn(entity: PendingReturnEntity) {
        dao.deletePendingReturn(entity)
    }

    /**
     * Check if a pending return exists for a specific invoice
     */
    suspend fun hasPendingReturnForInvoice(invoiceId: String): Boolean {
        return dao.getPendingReturnByInvoice(invoiceId) != null
    }

    /**
     * Get pending return entity by invoice ID
     */
    suspend fun getPendingReturnByInvoice(invoiceId: String): PendingReturnEntity? {
        return dao.getPendingReturnByInvoice(invoiceId)
    }

    /**
     * Clear all pending returns
     */
    suspend fun clearAll() {
        dao.clearAll()
        Log.d(TAG, "🗑️ Cleared all pending returns")
    }

    /**
     * Recovery: Fetches real server-assigned IDs for an invoice
     * Right before sync, we refresh from the server to get the real sales_id and sales_item_ids
     */
    private suspend fun recoverRealServerIds(context: Context, invoiceId: String, currentReq: ReturnSaleReq): ReturnSaleReq? {
        return try {
            Log.d("OFFLINE_SYNC_DEBUG", "🔍 [RECOVERY] Fetching real IDs for Invoice $invoiceId...")
            // Pass store_id if it's required by the API
            val response = ApiClient().getApiService(context)
                .getReturnSalesItemAPI(ReturnItemReq(invoiceId, currentReq.store_id.toString()))
                .execute()

            if (response.isSuccessful && response.body()?.status == 1 && response.body()?.data?.isNotEmpty() == true) {
                val serverSale = response.body()!!.data[0]
                val serverId = serverSale.id
                
                Log.d("OFFLINE_SYNC_DEBUG", "✅ [RECOVERY] Found server sale ID: $serverId for $invoiceId")

                val serverItems = serverSale.sales_items ?: emptyList()
                var patchedCount = 0

                val patchedItems = currentReq.returned_items.map { local ->
                    // Try to match by product/pack first, then fallback to index-based matching
                    var match = serverItems.find { si ->
                        val localProdId = local.product_id ?: 0
                        val localPackId = local.distribution_pack_id ?: 0
                        (localProdId != 0 && si.product_id == localProdId && si.distribution_pack_id == localPackId)
                    }
                    
                    if (match == null) {
                        // Offline items often get IDs 1, 2, 3 based on array index (index + 1)
                        val assumedIndex = (local.id - 1).coerceIn(0, serverItems.lastIndex)
                        if (assumedIndex >= 0 && assumedIndex < serverItems.size) {
                            match = serverItems[assumedIndex]
                        }
                    }

                    if (match != null) {
                        patchedCount++
                        local.copy(id = match.id)
                    } else {
                        Log.w("OFFLINE_SYNC_DEBUG", "⚠️ [RECOVERY] No match for item Prod:${local.product_id} Pack:${local.distribution_pack_id} in $invoiceId")
                        local
                    }
                }
                
                Log.d("OFFLINE_SYNC_DEBUG", "🛠️ [RECOVERY] Patched $patchedCount/${currentReq.returned_items.size} items with real IDs")
                currentReq.copy(sales_id = serverId, returned_items = patchedItems)
            } else {
                val msg = response.body()?.message ?: "Not found"
                Log.w("OFFLINE_SYNC_DEBUG", "⚠️ [RECOVERY] No server data for $invoiceId: $msg")
                null
            }
        } catch (e: Exception) {
            Log.e("OFFLINE_SYNC_DEBUG", "❌ [RECOVERY] Error for $invoiceId: ${e.message}")
            null
        }
    }
    
    /**
     * Sync all pending returns with the server.
     * Returns true if all syncs succeeded.
     */
    suspend fun syncAllPendingReturns(context: Context): Boolean = withContext(Dispatchers.IO) {
        val pendingReturns = getAllPendingReturns()
        if (pendingReturns.isEmpty()) {
            Log.d(TAG, "No pending returns to sync")
            return@withContext true
        }

        var allSuccessful = true
        val detailedSaleRepo = DetailedSaleRepository(context)

        pendingReturns.forEach { entity ->
            try {
                Log.d(TAG, "🔄 Syncing return: Invoice ${entity.invoice_id}")
                updateSyncStatus(entity.id, "SYNCING")

                var returnRequest = entityToReturnRequest(entity)
                
                // 🛠️ RECOVERY: Patch IDs from server if they might be wrong (offline sale case)
                val patchedRequest = recoverRealServerIds(context, entity.invoice_id, returnRequest)
                if (patchedRequest != null) {
                    returnRequest = patchedRequest
                    Log.d("OFFLINE_SYNC_DEBUG", "🛠️ [PATCHED] Applied real server ID: ${returnRequest.sales_id}")
                } else {
                    // 🚩 CRITICAL: If recovery failed and we have no valid sales_id, we MUST NOT proceed
                    Log.w("OFFLINE_SYNC_DEBUG", "⏳ [RETURN WAIT] Could not recover server IDs for ${entity.invoice_id}. Skipping.")
                    updateSyncStatus(entity.id, "FAILED", "Sale details not found on server")
                    allSuccessful = false
                    return@forEach // Skip to next entity
                }

                // 🔍 DEBUG: Log the full request JSON
                val requestJson = gson.toJson(returnRequest)
                Log.d("OFFLINE_SYNC_DEBUG", "🚀 [REQUEST] Syncing Return for Invoice ${entity.invoice_id}:\n$requestJson")

                val response = ApiClient().getApiService(context)
                    .getReturnSalesSubmitAPI(returnRequest)
                    .execute()
                
                Log.d("OFFLINE_SYNC_DEBUG", "📡 [RESPONSE] Received response for ${entity.invoice_id}. Code: ${response.code()}")

                if (response.isSuccessful && response.body()?.status == 1) {
                    val responseBody = gson.toJson(response.body())
                    Log.d("OFFLINE_SYNC_DEBUG", "✅ [SUCCESS] Return ${entity.invoice_id} synced! Response:\n$responseBody")
                    markAsSynced(entity.id)

                    // ✅ Update cached sale to mark as refunded
                    val invoiceId = entity.invoice_id
                    val saleDetails = detailedSaleRepo.getDetailedSaleByInvoiceId(invoiceId)

                    if (saleDetails != null) {
                        val grandTotal = saleDetails.grand_total
                        val reasonId = returnRequest.reason_id ?: -1
                        detailedSaleRepo.updateRefundedAmount(invoiceId, grandTotal, reasonId)
                        Log.d("OFFLINE_SYNC_DEBUG", "💾 [CACHE] Updated local cache for $invoiceId")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    val responseMsg = response.body()?.message ?: "No message"
                    val status = response.body()?.status ?: -1
                    Log.e("OFFLINE_SYNC_DEBUG", "❌ [FAILED] Return ${entity.invoice_id} failed. Status: $status, Msg: $responseMsg, ErrorBody: $errorBody")
                    
                    val errorMsg = responseMsg
                    updateSyncStatus(entity.id, "FAILED", errorMsg)
                    allSuccessful = false
                }
            } catch (e: Exception) {
                Log.e("OFFLINE_SYNC_DEBUG", "❌ [EXCEPTION] Error syncing return ${entity.invoice_id}: ${e.message}", e)
                updateSyncStatus(entity.id, "FAILED", e.message)
                allSuccessful = false
            }
        }
        allSuccessful
    }
}
