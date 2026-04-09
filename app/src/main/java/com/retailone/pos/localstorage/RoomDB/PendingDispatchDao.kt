package com.retailone.pos.localstorage.RoomDB

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingDispatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispatch(entity: PendingDispatchEntity): Long

    @Query("SELECT * FROM pending_dispatches WHERE sync_status != 'SYNCED' ORDER BY timestamp ASC")
    suspend fun getAllPendingDispatches(): List<PendingDispatchEntity>

    @Query("UPDATE pending_dispatches SET sync_status = :status, sync_error = :error WHERE local_id = :id")
    suspend fun updateSyncStatus(id: Int, status: String, error: String? = null)

    @Query("UPDATE pending_dispatches SET sync_status = 'SYNCED', sync_error = null WHERE local_id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("SELECT COUNT(*) FROM pending_dispatches WHERE sync_status != 'SYNCED'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_dispatches WHERE sync_status != 'SYNCED'")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM pending_dispatches WHERE return_id = :returnId AND sync_status != 'SYNCED' LIMIT 1")
    suspend fun getPendingDispatchByReturnId(returnId: Int): PendingDispatchEntity?

    @Delete
    suspend fun deleteDispatch(entity: PendingDispatchEntity)

    @Query("DELETE FROM pending_dispatches WHERE sync_status = 'SYNCED' AND timestamp < :beforeTimestamp")
    suspend fun deleteOldSynced(beforeTimestamp: Long): Int
}
