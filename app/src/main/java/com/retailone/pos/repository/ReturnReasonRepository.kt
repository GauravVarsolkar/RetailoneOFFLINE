package com.retailone.pos.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retailone.pos.localstorage.RoomDB.PosDatabase
import com.retailone.pos.localstorage.RoomDB.ReturnReasonDao
import com.retailone.pos.localstorage.RoomDB.ReturnReasonEntity
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData

class ReturnReasonRepository(private val context: Context) {

    private val database = PosDatabase.getDatabase(context)
    private val dao: ReturnReasonDao = database.returnReasonDao()
    private val gson = Gson()

    companion object {
        private const val TAG = "ReturnReasonRepo"
    }

    /**
     * Save return reasons to local database
     */
    suspend fun saveReturnReasons(reasons: List<ReturnReasonData>) {
        try {
            // Clear old reasons first (to avoid outdated data)
            dao.clearAll()

            val entities = reasons.map { reason ->
                ReturnReasonEntity(
                    id = reason.id,
                    reason_name = reason.reason_name,
                    reason_data_json = gson.toJson(reason)
                )
            }

            dao.insertReasons(entities)
            Log.d(TAG, "✅ Saved ${entities.size} return reasons to local DB")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving return reasons: ${e.message}", e)
        }
    }

    /**
     * Get all return reasons from local database
     */
    suspend fun getAllReturnReasons(): List<ReturnReasonData> {
        return try {
            val entities = dao.getAllReasons()
            entities.map { entity ->
                gson.fromJson(entity.reason_data_json, ReturnReasonData::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving return reasons: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Check if return reasons exist in local database
     */
    suspend fun hasReturnReasons(): Boolean {
        return dao.getReasonsCount() > 0
    }

    /**
     * Clear all return reasons
     */
    suspend fun clearReturnReasons() {
        dao.clearAll()
        Log.d(TAG, "🗑️ Cleared all return reasons")
    }

    // ✅ NEW: Get reason name by ID from local cache
    /**
     * Get reason name by ID from local cache
     */
    suspend fun getReasonNameById(reasonId: Int): String {
        return try {
            Log.d(TAG, "🔍 Looking for reason with ID: $reasonId")

            val entity = dao.getReasonById(reasonId)
            if (entity != null) {
                Log.d(TAG, "✅ Found reason entity: ${entity.reason_name}")

                // Parse the JSON to get the reason data
                val reasonData = gson.fromJson(entity.reason_data_json, ReturnReasonData::class.java)
                val reasonName = reasonData.reason_name ?: "Not Given"

                Log.d(TAG, "✅ Returning reason name: '$reasonName'")
                reasonName
            } else {
                Log.w(TAG, "⚠️ Reason ID $reasonId not found in cache")

                // ✅ DEBUG: Check what reasons ARE in the cache
                val allReasons = dao.getAllReasons()
                Log.d(TAG, "📋 Total reasons in cache: ${allReasons.size}")
                allReasons.forEach {
                    Log.d(TAG, "   - ID ${it.id}: ${it.reason_name}")
                }

                "Not Given"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting reason name: ${e.message}", e)
            e.printStackTrace()
            "Not Given"
        }
    }
}
