package com.retailone.pos.localstorage.RoomDB

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailedSaleDao {

    // Insert or update detailed sale data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailedSale(sale: DetailedSaleEntity): Long

    // Get detailed sale by invoice ID
    @Query("SELECT * FROM detailed_sales WHERE invoice_id = :invoiceId LIMIT 1")
    suspend fun getDetailedSaleByInvoiceId(invoiceId: String): DetailedSaleEntity?

    // Get detailed sale by sale_id
    @Query("SELECT * FROM detailed_sales WHERE sale_id = :saleId LIMIT 1")
    suspend fun getDetailedSaleBySaleId(saleId: Int): DetailedSaleEntity?

    // Get all detailed sales
    @Query("SELECT * FROM detailed_sales ORDER BY created_at DESC")
    suspend fun getAllDetailedSales(): List<DetailedSaleEntity>

    // Delete detailed sales older than timestamp (for 7-day cleanup)
    @Query("DELETE FROM detailed_sales WHERE created_at < :timestamp")
    suspend fun deleteDetailedSalesOlderThan(timestamp: Long): Int

    // Delete a specific detailed sale
    @Delete
    suspend fun deleteDetailedSale(sale: DetailedSaleEntity)

    // Clear all detailed sales
    @Query("DELETE FROM detailed_sales")
    suspend fun clearAll()

    // Check if detailed sale exists
    @Query("SELECT EXISTS(SELECT 1 FROM detailed_sales WHERE invoice_id = :invoiceId)")
    suspend fun detailedSaleExists(invoiceId: String): Boolean

    @Query("UPDATE detailed_sales SET invoice_id = :newInvoiceId WHERE invoice_id = :oldInvoiceId")
    suspend fun updateInvoiceId(oldInvoiceId: String, newInvoiceId: String)
}
