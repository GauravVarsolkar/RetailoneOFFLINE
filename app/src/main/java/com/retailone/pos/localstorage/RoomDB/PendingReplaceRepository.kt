package com.retailone.pos.localstorage.RoomDB

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.models.ReplaceModel.ReplaceSaleReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemReq
import com.retailone.pos.network.ApiClient
import com.retailone.pos.repository.DetailedSaleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PendingReplaceRepository(private val pendingReplaceDao: PendingReplaceDao) {

    suspend fun queueReplaceRequest(invoiceId: String, replaceRequest: ReplaceSaleReq): Long {
        val entity = PendingReplaceEntity(
            invoice_id = invoiceId,
            store_id = replaceRequest.store_id,
            store_manager_id = replaceRequest.store_manager_id,
            reason_id = replaceRequest.reason_id,
            sales_id = replaceRequest.sales_id,
            on_hold = replaceRequest.on_hold,
            remark = replaceRequest.remark,
            replace_request_json = Gson().toJson(replaceRequest)
        )
        return pendingReplaceDao.insertReplace(entity)
    }

    suspend fun getAllPendingReplaces(): List<PendingReplaceEntity> {
        return pendingReplaceDao.getAllPendingReplaces()
    }

    suspend fun updateSyncStatus(id: Int, status: String, error: String? = null) {
        pendingReplaceDao.updateSyncStatus(id, status, error)
    }

    suspend fun markAsSynced(id: Int) {
        pendingReplaceDao.updateSyncStatus(id, "SYNCED", null, System.currentTimeMillis())
    }

    fun getPendingCountFlow(): kotlinx.coroutines.flow.Flow<Int> {
        return pendingReplaceDao.getPendingCountFlow()
    }

    suspend fun getPendingCount(): Int {
        return pendingReplaceDao.getPendingCount()
    }

    fun entityToReplaceRequest(entity: PendingReplaceEntity): ReplaceSaleReq {
        return Gson().fromJson(entity.replace_request_json, ReplaceSaleReq::class.java)
    }

    suspend fun hasPendingReplaceForInvoice(invoiceId: String): Boolean {
        return pendingReplaceDao.getPendingReplaceByInvoice(invoiceId) != null
    }

    /**
     * Sync all pending replaces with the server.
     * Returns true if all syncs succeeded.
     */
    suspend fun syncAllPendingReplaces(context: Context): Boolean = withContext(Dispatchers.IO) {
        val pendingReplaces = getAllPendingReplaces()
        if (pendingReplaces.isEmpty()) {
            Log.d("OFFLINE_SYNC_DEBUG", "No pending replaces to sync")
            return@withContext true
        }

        var allSuccessful = true
        val detailedSaleRepo = DetailedSaleRepository(context)

        pendingReplaces.forEach { entity ->
            try {
                Log.d("OFFLINE_SYNC_DEBUG", "🔄 Syncing replace: Invoice ${entity.invoice_id}")
                updateSyncStatus(entity.id, "SYNCING")

                var replaceRequest = entityToReplaceRequest(entity)
                
                // 🛠️ Date format fix: Ensure return_date_time matches "yyyy-MM-dd HH:mm:ss"
                val rawDate = replaceRequest.return_date_time
                if (!rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))) {
                    Log.d("OFFLINE_SYNC_DEBUG", "🛠️ [DATE FIX] Found old format date: $rawDate, fixing...")
                    try {
                        // Try parsing old formats (dd-MMM-yyyy hh:mm a)
                        val oldFormat = java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a", java.util.Locale.US)
                        val date = oldFormat.parse(rawDate)
                        if (date != null) {
                            val newFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                            replaceRequest = replaceRequest.copy(return_date_time = newFormat.format(date))
                            Log.d("OFFLINE_SYNC_DEBUG", "🛠️ [DATE FIX] Patched date: ${replaceRequest.return_date_time}")
                        }
                    } catch (e: Exception) {
                        Log.e("OFFLINE_SYNC_DEBUG", "❌ [DATE FIX] Failed to fix date format, keeping original: ${e.message}")
                    }
                }
                
                // 🛠️ RECOVERY: Patch IDs from server if they might be wrong (offline sale case)
                val patchedRequest = recoverRealServerIds(context, entity.invoice_id, replaceRequest)
                if (patchedRequest != null) {
                    replaceRequest = patchedRequest
                    Log.d("OFFLINE_SYNC_DEBUG", "🛠️ [REPLACE PATCHED] Applied real server ID: ${replaceRequest.sales_id}")
                }

                val response = ApiClient().getApiService(context)
                    .replaceSale(replaceRequest)
                    .execute()

                if (response.isSuccessful && response.body() != null) {
                    val bodyString = response.body()!!.string()
                    Log.d("OFFLINE_SYNC_DEBUG", "📡 Replace Response Length: ${bodyString.length}")

                    var status = 0
                    var message = ""
                    try {
                        val obj = org.json.JSONObject(bodyString)
                        status = obj.optInt("status", 0)
                        message = obj.optString("message", "Success")
                    } catch (e: Exception) {
                        Log.e("OFFLINE_SYNC_DEBUG", "❌ Cannot parse JSON from response: $bodyString")
                    }

                    if (status == 1) {
                        Log.d("OFFLINE_SYNC_DEBUG", "✅ API Success for replace ${entity.id}")
                        markAsSynced(entity.id)

                        // ✅ Update cached sale to mark as replaced
                        val invoiceId = entity.invoice_id
                        val saleDetails = detailedSaleRepo.getDetailedSaleByInvoiceId(invoiceId)

                        if (saleDetails != null) {
                            val grandTotal = saleDetails.grand_total
                            val reasonId = replaceRequest.reason_id ?: -1
                            detailedSaleRepo.updateReplacedAmount(invoiceId, grandTotal, reasonId)
                            Log.d("OFFLINE_SYNC_DEBUG", "💾 Updated local cache for $invoiceId")
                        }
                    } else {
                        updateSyncStatus(entity.id, "FAILED", message)
                        Log.e("OFFLINE_SYNC_DEBUG", "❌ API Failed for replace ${entity.id}: $message")
                        allSuccessful = false
                    }
                } else {
                    val errorMsg = try { response.errorBody()?.string() } catch(e:Exception){"Sync failed"}
                    updateSyncStatus(entity.id, "FAILED", errorMsg)
                    Log.e("OFFLINE_SYNC_DEBUG", "❌ API HTTP Failed for replace ${entity.id}: $errorMsg")
                    allSuccessful = false
                }
            } catch (e: Exception) {
                updateSyncStatus(entity.id, "FAILED", e.message)
                Log.e("OFFLINE_SYNC_DEBUG", "❌ Error syncing replace ${entity.id}: ${e.message}")
                allSuccessful = false
            }
        }
        allSuccessful
    }

    /**
     * Recovery: Fetches real server-assigned IDs for an invoice
     * Right before sync, we refresh from the server to get the real sales_id and sales_item_ids
     */
    private suspend fun recoverRealServerIds(context: Context, invoiceId: String, currentReq: ReplaceSaleReq): ReplaceSaleReq? {
        return try {
            Log.d("OFFLINE_SYNC_DEBUG", "🔍 [REPLACE RECOVERY] Fetching real IDs for Invoice $invoiceId...")
            // Pass store_id if it's required by the API
            val response = ApiClient().getApiService(context)
                .getReturnSalesItemAPI(ReturnItemReq(invoiceId, currentReq.store_id.toString()))
                .execute()

            if (response.isSuccessful && response.body()?.status == 1 && response.body()?.data?.isNotEmpty() == true) {
                val serverSale = response.body()!!.data[0]
                val serverId = serverSale.id
                
                Log.d("OFFLINE_SYNC_DEBUG", "✅ [REPLACE RECOVERY] Found server sale ID: $serverId for $invoiceId")

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
                        Log.w("OFFLINE_SYNC_DEBUG", "⚠️ [REPLACE RECOVERY] No match for item Prod:${local.product_id} Pack:${local.distribution_pack_id} in $invoiceId")
                        local
                    }
                }
                
                Log.d("OFFLINE_SYNC_DEBUG", "🛠️ [REPLACE RECOVERY] Patched $patchedCount/${currentReq.returned_items.size} items with real IDs")
                currentReq.copy(sales_id = serverId, returned_items = patchedItems)
            } else {
                val msg = response.body()?.message ?: "Not found"
                Log.w("OFFLINE_SYNC_DEBUG", "⚠️ [REPLACE RECOVERY] No server data for $invoiceId: $msg")
                null
            }
        } catch (e: Exception) {
            Log.e("OFFLINE_SYNC_DEBUG", "❌ [REPLACE RECOVERY] Error for $invoiceId: ${e.message}")
            null
        }
    }
}
