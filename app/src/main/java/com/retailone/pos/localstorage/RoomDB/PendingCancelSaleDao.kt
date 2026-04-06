package com.retailone.pos.localstorage.RoomDB

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingCancelSaleDao {

    // Insert a new pending cancel request
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingCancel(entity: PendingCancelSaleEntity): Long

    // Get all pending cancel requests (for sync purposes)
    @Query("SELECT * FROM pending_cancel_sales WHERE sync_status IN ('PENDING', 'FAILED') ORDER BY created_at ASC")
    suspend fun getPendingCancelsForSync(): List<PendingCancelSaleEntity>

    // Get all cancel requests (including SYNCED, for UI display)
    @Query("SELECT * FROM pending_cancel_sales ORDER BY created_at ASC")
    suspend fun getAllCancels(): List<PendingCancelSaleEntity>

    // Get all cancel requests by date range (for UI display)
    @Query("SELECT * FROM pending_cancel_sales WHERE created_at >= :startTime AND created_at <= :endTime ORDER BY created_at DESC")
    suspend fun getCancelsByDateRange(startTime: Long, endTime: Long): List<PendingCancelSaleEntity>

    // Get pending cancel count
    @Query("SELECT COUNT(*) FROM pending_cancel_sales WHERE sync_status = 'PENDING'")
    suspend fun getPendingCancelCount(): Int

    // Get pending cancel count as Flow
    @Query("SELECT COUNT(*) FROM pending_cancel_sales WHERE sync_status = 'PENDING'")
    fun getPendingCancelCountFlow(): Flow<Int>

    // Check if a cancel request already exists for an invoice (avoid duplication)
    @Query("SELECT EXISTS(SELECT 1 FROM pending_cancel_sales WHERE invoice_id = :invoiceId AND (sync_status = 'PENDING' OR sync_status = 'SYNCING'))")
    suspend fun cancelRequestExists(invoiceId: String): Boolean

    // Check if any cancel request exists for an invoice (regardless of status)
    @Query("SELECT EXISTS(SELECT 1 FROM pending_cancel_sales WHERE invoice_id = :invoiceId)")
    suspend fun anyCancelRequestExists(invoiceId: String): Boolean

    // Update sync status
    @Query("UPDATE pending_cancel_sales SET sync_status = :status, last_sync_attempt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, status: String, timestamp: Long)

    // Update sync status with error
    @Query("UPDATE pending_cancel_sales SET sync_status = :status, last_sync_attempt = :timestamp, error_message = :error, sync_attempts = sync_attempts + 1 WHERE id = :id")
    suspend fun updateSyncStatusWithError(id: Int, status: String, timestamp: Long, error: String)

    // Mark as synced
    @Query("UPDATE pending_cancel_sales SET sync_status = 'SYNCED', is_synced = 1, synced_at = :timestamp, reversal_invoice_id = :reversalInvoiceId, reversal_sales_id = :reversalSalesId WHERE id = :id")
    suspend fun markAsSynced(id: Int, timestamp: Long, reversalInvoiceId: String, reversalSalesId: Int)

    // Get cancel request by invoice ID
    @Query("SELECT * FROM pending_cancel_sales WHERE invoice_id = :invoiceId LIMIT 1")
    suspend fun getCancelByInvoiceId(invoiceId: String): PendingCancelSaleEntity?

    // Delete a cancel request
    @Query("DELETE FROM pending_cancel_sales WHERE id = :id")
    suspend fun deleteCancel(id: Int)

    // Delete all synced cancel requests (cleanup)
    @Query("DELETE FROM pending_cancel_sales WHERE is_synced = 1 AND synced_at < :timestamp")
    suspend fun deleteOldSyncedCancels(timestamp: Long): Int

    // Clear all pending cancel requests
    @Query("DELETE FROM pending_cancel_sales")
    suspend fun clearAll()
}
