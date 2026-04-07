package com.retailone.pos.localstorage.RoomDB

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSaleDao {

    // Insert a new pending sale
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSale(sale: PendingSaleEntity): Long

    // Get all pending sales (not yet synced)
    @Query("SELECT * FROM pending_sales WHERE sync_status IN ('PENDING', 'FAILED') ORDER BY created_at ASC")
    suspend fun getPendingSales(): List<PendingSaleEntity>

    // Get all pending sales by date range
    @Query("SELECT * FROM pending_sales WHERE store_id = :storeId AND sync_status IN ('PENDING', 'FAILED', 'SYNCING', 'SYNCED') AND created_at >= :startTime AND created_at <= :endTime ORDER BY created_at DESC")
    suspend fun getSalesByDateRange(storeId: String, startTime: Long, endTime: Long): List<PendingSaleEntity>

    // Get all pending sales as Flow (reactive)
    @Query("SELECT * FROM pending_sales WHERE sync_status IN ('PENDING', 'FAILED') ORDER BY created_at DESC")
    fun getPendingSalesFlow(): Flow<List<PendingSaleEntity>>

    // Get all sales (including synced) as Flow
    @Query("SELECT * FROM pending_sales ORDER BY created_at DESC")
    fun getAllSalesFlow(): Flow<List<PendingSaleEntity>>

    // Get single sale by ID
    @Query("SELECT * FROM pending_sales WHERE id = :saleId")
    suspend fun getSaleById(saleId: Int): PendingSaleEntity?

    // Get sale by invoice ID and store ID (for duplicate check)
    @Query("SELECT * FROM pending_sales WHERE invoice_id = :invoiceId AND store_id = :storeId LIMIT 1")
    suspend fun getSaleByInvoice(invoiceId: String, storeId: String): PendingSaleEntity?

    // Update sync status
    @Query("""
        UPDATE pending_sales 
        SET sync_status = :status, 
            last_sync_attempt = :timestamp,
            sync_attempts = sync_attempts + 1
        WHERE id = :saleId
    """)
    suspend fun updateSyncStatus(saleId: Int, status: String, timestamp: Long)

    // Update sync status with error message
    @Query("""
        UPDATE pending_sales 
        SET sync_status = :status, 
            last_sync_attempt = :timestamp,
            sync_attempts = sync_attempts + 1,
            error_message = :errorMessage
        WHERE id = :saleId
    """)
    suspend fun updateSyncStatusWithError(saleId: Int, status: String, timestamp: Long, errorMessage: String)

    // Mark sale as synced
    @Query("""
        UPDATE pending_sales 
        SET sync_status = 'SYNCED', 
            synced_at = :timestamp,
            error_message = NULL
        WHERE id = :saleId
    """)
    suspend fun markAsSynced(saleId: Int, timestamp: Long)

    // Mark sale as synced and update its invoice ID to the server's invoice ID
    @Query("""
        UPDATE pending_sales 
        SET sync_status = 'SYNCED', 
            synced_at = :timestamp,
            error_message = NULL,
            invoice_id = :serverInvoiceId
        WHERE id = :saleId
    """)
    suspend fun markAsSyncedWithInvoiceId(saleId: Int, serverInvoiceId: String, timestamp: Long)

    // Delete synced sales older than X days (cleanup)
    @Query("DELETE FROM pending_sales WHERE sync_status = 'SYNCED' AND synced_at < :timestamp")
    suspend fun deleteSyncedSalesOlderThan(timestamp: Long)

    // Delete a specific sale
    @Delete
    suspend fun deleteSale(sale: PendingSaleEntity)

    // Get count of pending sales
    @Query("SELECT COUNT(*) FROM pending_sales WHERE sync_status IN ('PENDING', 'FAILED')")
    suspend fun getPendingSalesCount(): Int

    // Get count of pending sales as Flow
    @Query("SELECT COUNT(*) FROM pending_sales WHERE sync_status IN ('PENDING', 'FAILED')")
    fun getPendingSalesCountFlow(): Flow<Int>

    // Clear all sales (for testing/logout)
    @Query("DELETE FROM pending_sales")
    suspend fun clearAll()
}
