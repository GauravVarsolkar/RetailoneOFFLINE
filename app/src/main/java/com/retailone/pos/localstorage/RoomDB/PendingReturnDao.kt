package com.retailone.pos.localstorage.RoomDB

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingReturnDao {

    // Insert pending return
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingReturn(pendingReturn: PendingReturnEntity): Long

    // Get all pending returns (not synced yet)
    @Query("SELECT * FROM pending_returns WHERE sync_status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getAllPendingReturns(): List<PendingReturnEntity>

    // Get pending returns as Flow (for real-time updates)
    @Query("SELECT * FROM pending_returns WHERE sync_status = 'PENDING' ORDER BY created_at ASC")
    fun getPendingReturnsFlow(): Flow<List<PendingReturnEntity>>

    // Get count of pending returns as Flow
    @Query("SELECT COUNT(*) FROM pending_returns WHERE sync_status = 'PENDING'")
    fun getPendingReturnsCountFlow(): Flow<Int>

    // Get count of pending returns
    @Query("SELECT COUNT(*) FROM pending_returns WHERE sync_status = 'PENDING'")
    suspend fun getPendingReturnsCount(): Int

    // Update sync status
    @Query("UPDATE pending_returns SET sync_status = :status, error_message = :errorMessage WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, status: String, errorMessage: String? = null)

    // Mark as synced
    @Query("UPDATE pending_returns SET sync_status = 'SYNCED', synced_at = :syncedAt WHERE id = :id")
    suspend fun markAsSynced(id: Int, syncedAt: Long = System.currentTimeMillis())

    // Delete synced returns older than 7 days
    @Query("DELETE FROM pending_returns WHERE sync_status = 'SYNCED' AND synced_at < :timestamp")
    suspend fun deleteSyncedReturnsOlderThan(timestamp: Long): Int

    // Delete a specific pending return
    @Delete
    suspend fun deletePendingReturn(pendingReturn: PendingReturnEntity)

    // Clear all pending returns
    @Query("DELETE FROM pending_returns")
    suspend fun clearAll()

    // Get return by ID
    @Query("SELECT * FROM pending_returns WHERE id = :id LIMIT 1")
    suspend fun getPendingReturnById(id: Int): PendingReturnEntity?

    @Query("SELECT * FROM pending_returns WHERE invoice_id = :invoiceId LIMIT 1")
    suspend fun getPendingReturnByInvoice(invoiceId: String): PendingReturnEntity?
}
