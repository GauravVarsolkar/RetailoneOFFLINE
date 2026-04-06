package com.retailone.pos.localstorage.RoomDB

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingGoodsReturnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(entity: PendingGoodsReturnEntity): Long

    @Query("SELECT * FROM pending_goods_returns WHERE sync_status != 'SYNCED' ORDER BY timestamp ASC")
    suspend fun getAllPendingReturns(): List<PendingGoodsReturnEntity>

    @Query("UPDATE pending_goods_returns SET sync_status = :status, sync_error = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, status: String, error: String? = null)

    @Query("UPDATE pending_goods_returns SET sync_status = 'SYNCED', sync_error = null WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("SELECT COUNT(*) FROM pending_goods_returns WHERE sync_status != 'SYNCED'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_goods_returns WHERE sync_status != 'SYNCED'")
    suspend fun getPendingCount(): Int

    @Delete
    suspend fun deleteReturn(entity: PendingGoodsReturnEntity)

    @Query("DELETE FROM pending_goods_returns WHERE sync_status = 'SYNCED' AND timestamp < :beforeTimestamp")
    suspend fun deleteOldSynced(beforeTimestamp: Long): Int
}
