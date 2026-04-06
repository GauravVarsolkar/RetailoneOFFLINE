package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Dao
interface PendingReplaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(replace: PendingReplaceEntity): Long

    @Query("SELECT * FROM pending_replaces WHERE sync_status = :status")
    suspend fun getReplacesByStatus(status: String): List<PendingReplaceEntity>

    @Query("SELECT * FROM pending_replaces WHERE sync_status IN ('PENDING', 'FAILED')")
    suspend fun getAllPendingReplaces(): List<PendingReplaceEntity>

    @Query("UPDATE pending_replaces SET sync_status = :status, error_message = :error, synced_at = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(
        id: Int,
        status: String,
        error: String? = null,
        syncedAt: Long? = null
    )

    @Query("DELETE FROM pending_replaces WHERE sync_status = 'SYNCED'")
    suspend fun deleteSyncedReplaces()

    @Query("SELECT COUNT(*) FROM pending_replaces WHERE sync_status IN ('PENDING', 'FAILED')")
    fun getPendingCountFlow(): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_replaces WHERE sync_status IN ('PENDING', 'FAILED')")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM pending_replaces WHERE invoice_id = :invoiceId AND sync_status IN ('PENDING', 'FAILED') LIMIT 1")
    suspend fun getPendingReplaceByInvoice(invoiceId: String): PendingReplaceEntity?
}
