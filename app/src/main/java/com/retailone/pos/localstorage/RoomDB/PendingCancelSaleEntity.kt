package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_cancel_sales")
data class PendingCancelSaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val invoice_id: String,
    val sale_id: Int,
    val sale_date_time: String,
    val store_id: String,
    val grand_total: String,
    val payment_type: String,

    // Sync status tracking
    val sync_status: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val sync_attempts: Int = 0,
    val last_sync_attempt: Long? = null,
    val error_message: String? = null,
    val is_synced: Boolean = false,

    // Server response fields (filled after sync)
    val reversal_invoice_id: String? = null,
    val reversal_sales_id: Int? = null,

    // Timestamps
    val created_at: Long = System.currentTimeMillis(),
    val synced_at: Long? = null
)
