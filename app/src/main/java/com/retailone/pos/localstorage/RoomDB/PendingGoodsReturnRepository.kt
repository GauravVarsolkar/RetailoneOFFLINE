package com.retailone.pos.localstorage.RoomDB

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnRequests
import com.retailone.pos.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PendingGoodsReturnRepository(private val dao: PendingGoodsReturnDao) {

    private val gson = Gson()

    suspend fun queueReturnRequest(request: StockReturnRequests): Long {
        val entity = PendingGoodsReturnEntity(
            store_id = request.store_id,
            return_date = request.return_date,
            remarks = request.remarks ?: "",
            return_request_json = gson.toJson(request)
        )
        return dao.insertReturn(entity)
    }

    suspend fun getAllPendingReturns(): List<PendingGoodsReturnEntity> {
        return dao.getAllPendingReturns()
    }

    suspend fun updateSyncStatus(id: Int, status: String, error: String? = null) {
        dao.updateSyncStatus(id, status, error)
    }

    suspend fun markAsSynced(id: Int) {
        dao.markAsSynced(id)
    }

    suspend fun getPendingCount(): Int {
        return dao.getPendingCount()
    }

    fun getPendingCountFlow(): kotlinx.coroutines.flow.Flow<Int> {
        return dao.getPendingCountFlow()
    }

    /**
     * Sync all pending warehouse returns with the server.
     * Returns true if all syncs succeeded.
     */
    suspend fun syncAllPendingReturns(context: Context): Boolean = withContext(Dispatchers.IO) {
        val pendingReturns = getAllPendingReturns()
        if (pendingReturns.isEmpty()) {
            Log.d("OFFLINE_SYNC_DEBUG", "No pending warehouse returns to sync")
            return@withContext true
        }

        var allSuccessful = true
        pendingReturns.forEach { entity ->
            try {
                Log.d("OFFLINE_SYNC_DEBUG", "🔄 Syncing warehouse return ID: ${entity.id}")
                updateSyncStatus(entity.id, "SYNCING")

                val request = gson.fromJson(entity.return_request_json, StockReturnRequests::class.java)
                
                val response = ApiClient().getApiService(context)
                    .submitStockReturn(request)
                    .execute()

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.status == "success") {
                        Log.d("OFFLINE_SYNC_DEBUG", "✅ API Success for warehouse return ${entity.id}")
                        markAsSynced(entity.id)
                    } else {
                        updateSyncStatus(entity.id, "FAILED", body.message)
                        Log.e("OFFLINE_SYNC_DEBUG", "❌ API Failed for warehouse return ${entity.id}: ${body.message}")
                        allSuccessful = false
                    }
                } else {
                    val errorMsg = try { response.errorBody()?.string() } catch(e:Exception){"Sync failed"}
                    updateSyncStatus(entity.id, "FAILED", errorMsg)
                    Log.e("OFFLINE_SYNC_DEBUG", "❌ API HTTP Failed for warehouse return ${entity.id}: $errorMsg")
                    allSuccessful = false
                }
            } catch (e: Exception) {
                updateSyncStatus(entity.id, "FAILED", e.message)
                Log.e("OFFLINE_SYNC_DEBUG", "❌ Error syncing warehouse return ${entity.id}: ${e.message}")
                allSuccessful = false
            }
        }
        allSuccessful
    }
}
