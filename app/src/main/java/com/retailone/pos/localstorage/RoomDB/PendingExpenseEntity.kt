package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_expenses")
data class PendingExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val amount: String,
    val category_name: String,
    val invoice: String,
    val store_manager_id: String,
    val expense_date_time: String,
    val store_id: String,
    val vendor_name: String,

    val vat: String,
    val total_amount: String,
    val sdc_no: String,
    val receipt_no: String,
    val remarks: String,

    // Sync status tracking
    val sync_status: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val sync_attempts: Int = 0,
    val last_sync_attempt: Long? = null,
    val error_message: String? = null,

    // Timestamps
    val created_at: Long = System.currentTimeMillis(),
    val synced_at: Long? = null
)
