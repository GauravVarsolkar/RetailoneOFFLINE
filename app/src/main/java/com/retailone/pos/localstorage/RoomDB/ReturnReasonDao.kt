package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Dao
interface ReturnReasonDao {

    // Insert or replace return reason
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReason(reason: ReturnReasonEntity): Long

    // Insert multiple reasons
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReasons(reasons: List<ReturnReasonEntity>)

    // Get all return reasons
    @Query("SELECT * FROM return_reasons ORDER BY id ASC")
    suspend fun getAllReasons(): List<ReturnReasonEntity>

    // Check if reasons exist
    @Query("SELECT COUNT(*) FROM return_reasons")
    suspend fun getReasonsCount(): Int

    // Clear all reasons
    @Query("DELETE FROM return_reasons")
    suspend fun clearAll()

    // Delete a specific reason
    @Delete
    suspend fun deleteReason(reason: ReturnReasonEntity)

    // ✅ ADD THIS: Get reason by ID
    @Query("SELECT * FROM return_reasons WHERE id = :reasonId LIMIT 1")
    suspend fun getReasonById(reasonId: Int): ReturnReasonEntity?
}
