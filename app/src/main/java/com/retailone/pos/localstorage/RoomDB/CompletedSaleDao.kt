package com.retailone.pos.localstorage.RoomDB

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedSaleDao {

    // Insert a completed sale
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: CompletedSaleEntity): Long

    // Insert multiple sales (for bulk insert from API)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<CompletedSaleEntity>)

    // Get all sales (reactive with Flow) - for displaying in RecyclerView
    @Query("SELECT * FROM completed_sales ORDER BY sale_id DESC")
    fun getAllSalesFlow(): Flow<List<CompletedSaleEntity>>

    // Get all sales (non-reactive) - for one-time fetch
    @Query("SELECT * FROM completed_sales ORDER BY sale_id DESC")
    suspend fun getAllSales(): List<CompletedSaleEntity>


    // Search by invoice ID (for search functionality)
    @Query("SELECT * FROM completed_sales WHERE invoice_id = :invoiceId LIMIT 1")
    suspend fun getSaleByInvoiceId(invoiceId: String): CompletedSaleEntity?

    // Search by invoice ID (reactive)
    @Query("SELECT * FROM completed_sales WHERE invoice_id = :invoiceId LIMIT 1")
    fun getSaleByInvoiceIdFlow(invoiceId: String): Flow<CompletedSaleEntity?>

    // Get sale by sale_id
    @Query("SELECT * FROM completed_sales WHERE sale_id = :saleId LIMIT 1")
    suspend fun getSaleBySaleId(saleId: Int): CompletedSaleEntity?

    // Get sales for a specific store
    @Query("SELECT * FROM completed_sales WHERE store_id = :storeId ORDER BY created_at DESC")
    fun getSalesByStore(storeId: Int): Flow<List<CompletedSaleEntity>>

    // Delete sales older than a specific timestamp (for 7-day cleanup)
    @Query("DELETE FROM completed_sales WHERE created_at < :timestamp")
    suspend fun deleteSalesOlderThan(timestamp: Long): Int

    // Delete a specific sale
    @Delete
    suspend fun deleteSale(sale: CompletedSaleEntity)

    // Delete sale by invoice ID
    @Query("DELETE FROM completed_sales WHERE invoice_id = :invoiceId")
    suspend fun deleteSaleByInvoiceId(invoiceId: String)

    // Get count of all sales
    @Query("SELECT COUNT(*) FROM completed_sales")
    suspend fun getSalesCount(): Int

    // Get count of sales as Flow
    @Query("SELECT COUNT(*) FROM completed_sales")
    fun getSalesCountFlow(): Flow<Int>

    // Clear all sales (for testing/logout)
    @Query("DELETE FROM completed_sales")
    suspend fun clearAll()

    // Check if sale exists by invoice ID
    @Query("SELECT EXISTS(SELECT 1 FROM completed_sales WHERE invoice_id = :invoiceId)")
    suspend fun saleExists(invoiceId: String): Boolean
}
