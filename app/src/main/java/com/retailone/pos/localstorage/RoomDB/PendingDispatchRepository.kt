package com.retailone.pos.localstorage.RoomDB

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.models.Dispatch.DispatchRequest
import com.retailone.pos.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PendingDispatchRepository(private val dao: PendingDispatchDao) {

    private val gson = Gson()

    suspend fun queueDispatchRequest(request: DispatchRequest): Long {
        val entity = PendingDispatchEntity(
            return_id = request.id,
            seal_no = request.seal_no,
            vehicle_no = request.vehicle_no,
            driver_name = request.driver_name,
            dispatch_request_json = gson.toJson(request)
        )
        return dao.insertDispatch(entity)
    }

    suspend fun getAllPendingDispatches(): List<PendingDispatchEntity> {
        return dao.getAllPendingDispatches()
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

    suspend fun getPendingDispatchByReturnId(returnId: Int): PendingDispatchEntity? {
        return dao.getPendingDispatchByReturnId(returnId)
    }

    /**
     * Sync all pending dispatches with the server.
     * Returns true if all syncs succeeded.
     */
    suspend fun syncAllPendingDispatches(context: Context): Boolean = withContext(Dispatchers.IO) {
        val pendingDispatches = getAllPendingDispatches()
        if (pendingDispatches.isEmpty()) {
            Log.d("OFFLINE_SYNC_DEBUG", "No pending dispatches to sync")
            return@withContext true
        }

        var allSuccessful = true
        pendingDispatches.forEach { entity ->
            try {
                Log.d("OFFLINE_SYNC_DEBUG", "🔄 Syncing dispatch for Return ID: ${entity.return_id}")
                updateSyncStatus(entity.local_id, "SYNCING")

                val request = gson.fromJson(entity.dispatch_request_json, DispatchRequest::class.java)
                
                val response = ApiClient().getApiService(context)
                    .dispatchStock(request)
                    .execute()

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        Log.d("OFFLINE_SYNC_DEBUG", "✅ API Success for dispatch of Return ${entity.return_id}")
                        markAsSynced(entity.local_id)
                    } else {
                        updateSyncStatus(entity.local_id, "FAILED", body.message)
                        Log.e("OFFLINE_SYNC_DEBUG", "❌ API Failed for dispatch of Return ${entity.return_id}: ${body.message}")
                        allSuccessful = false
                    }
                } else {
                    val errorMsg = try { response.errorBody()?.string() } catch(e:Exception){"Sync failed"}
                    updateSyncStatus(entity.local_id, "FAILED", errorMsg)
                    Log.e("OFFLINE_SYNC_DEBUG", "❌ API HTTP Failed for dispatch of Return ${entity.return_id}: $errorMsg")
                    allSuccessful = false
                }
            } catch (e: Exception) {
                updateSyncStatus(entity.local_id, "FAILED", e.message)
                Log.e("OFFLINE_SYNC_DEBUG", "❌ Error syncing dispatch of Return ${entity.return_id}: ${e.message}")
                allSuccessful = false
            }
        }
        allSuccessful
    }
}
