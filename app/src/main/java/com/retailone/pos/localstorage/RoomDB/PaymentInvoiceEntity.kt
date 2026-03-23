package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_invoices")
data class PaymentInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Store ID for filtering
    val store_id: Int,

    // Date range for filtering
    val from_date: String,
    val to_date: String,

    // Store complete InvoiceRes data as JSON
    val invoice_data_json: String,

    // Summary data for quick access
    val invoices_paid: Int,
    val invoices_unpaid: Int,
    val payments_due: Double,
    val payments_received: Double,
    val total_invoice_amount: Double,

    // Timestamp for cleanup
    val created_at: Long = System.currentTimeMillis()
)
