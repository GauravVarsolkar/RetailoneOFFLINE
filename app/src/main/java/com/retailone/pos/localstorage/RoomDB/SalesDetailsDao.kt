package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Dao
interface SalesDetailsDao {

    // Insert or update sales details
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesDetails(entity: SalesDetailsEntity): Long

    // Insert multiple sales details at once
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSalesDetails(entities: List<SalesDetailsEntity>): List<Long>

    // Get sales details by sale ID
    @Query("SELECT * FROM sales_details WHERE sale_id = :saleId LIMIT 1")
    suspend fun getSalesDetailsBySaleId(saleId: Int): SalesDetailsEntity?

    // Get sales details by invoice ID
    @Query("SELECT * FROM sales_details WHERE invoice_id = :invoiceId LIMIT 1")
    suspend fun getSalesDetailsByInvoiceId(invoiceId: String): SalesDetailsEntity?

    // Get all cached sales details
    @Query("SELECT * FROM sales_details ORDER BY created_at DESC")
    suspend fun getAllSalesDetails(): List<SalesDetailsEntity>

    // Delete sales details older than timestamp (for 7-day cleanup)
    @Query("DELETE FROM sales_details WHERE created_at < :timestamp")
    suspend fun deleteSalesDetailsOlderThan(timestamp: Long): Int

    // Delete sales details by sale ID
    @Query("DELETE FROM sales_details WHERE sale_id = :saleId")
    suspend fun deleteSalesDetailsBySaleId(saleId: Int): Int

    // Clear all sales details
    @Query("DELETE FROM sales_details")
    suspend fun clearAll()

    // Check if sales details exists
    @Query("SELECT EXISTS(SELECT 1 FROM sales_details WHERE sale_id = :saleId)")
    suspend fun salesDetailsExists(saleId: Int): Boolean

    // Check if sales details exists by invoice ID
    @Query("SELECT EXISTS(SELECT 1 FROM sales_details WHERE invoice_id = :invoiceId)")
    suspend fun salesDetailsExistsByInvoiceId(invoiceId: String): Boolean
}
