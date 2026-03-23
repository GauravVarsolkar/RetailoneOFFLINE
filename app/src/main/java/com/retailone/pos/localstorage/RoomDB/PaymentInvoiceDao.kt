package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Dao
interface PaymentInvoiceDao {

    // Insert or update payment invoice data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentInvoice(entity: PaymentInvoiceEntity): Long

    // Get payment invoice by store ID and date range
    @Query("SELECT * FROM payment_invoices WHERE store_id = :storeId AND from_date = :fromDate AND to_date = :toDate ORDER BY created_at DESC LIMIT 1")
    suspend fun getPaymentInvoiceByDateRange(storeId: Int, fromDate: String, toDate: String): PaymentInvoiceEntity?

    // Get latest payment invoice for a store
    @Query("SELECT * FROM payment_invoices WHERE store_id = :storeId ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestPaymentInvoice(storeId: Int): PaymentInvoiceEntity?

    // Get all payment invoices for a store
    @Query("SELECT * FROM payment_invoices WHERE store_id = :storeId ORDER BY created_at DESC")
    suspend fun getAllPaymentInvoices(storeId: Int): List<PaymentInvoiceEntity>

    // Delete payment invoices older than timestamp (for 7-day cleanup)
    @Query("DELETE FROM payment_invoices WHERE created_at < :timestamp")
    suspend fun deletePaymentInvoicesOlderThan(timestamp: Long): Int

    // Delete payment invoices for specific store and date range
    @Query("DELETE FROM payment_invoices WHERE store_id = :storeId AND from_date = :fromDate AND to_date = :toDate")
    suspend fun deletePaymentInvoice(storeId: Int, fromDate: String, toDate: String): Int

    // Delete all payment invoices for a store
    @Query("DELETE FROM payment_invoices WHERE store_id = :storeId")
    suspend fun deleteAllByStoreId(storeId: Int): Int

    // Clear all payment invoices
    @Query("DELETE FROM payment_invoices")
    suspend fun clearAll()

    // Check if payment invoice exists
    @Query("SELECT EXISTS(SELECT 1 FROM payment_invoices WHERE store_id = :storeId AND from_date = :fromDate AND to_date = :toDate)")
    suspend fun paymentInvoiceExists(storeId: Int, fromDate: String, toDate: String): Boolean
}
