package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.localstorage.RoomDB.PendingReturnDao
import com.retailone.pos.localstorage.RoomDB.PendingReturnEntity
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnSaleReq
import kotlinx.coroutines.flow.Flow

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
    suspend fun queueReturnRequest(returnRequest: ReturnSaleReq): Long {
        return try {
            val entity = PendingReturnEntity(
                invoice_id = "", // Will be derived from sales_id if needed
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
     * Get count of pending returns
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
     * Clear all pending returns
     */
    suspend fun clearAll() {
        dao.clearAll()
        Log.d(TAG, "🗑️ Cleared all pending returns")
    }
}
