package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_replaces")
data class PendingReplaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val invoice_id: String,

    val store_id: Int,

    val store_manager_id: Int,

    val reason_id: Int,

    val sales_id: Int,

    val on_hold: Int,

    val remark: String?,

    // Store complete ReplaceSaleReq as JSON
    val replace_request_json: String,

    // Status: PENDING, SYNCING, SYNCED, FAILED
    val sync_status: String = "PENDING",

    // Error message if sync failed
    val error_message: String? = null,

    // Timestamp when replace was created
    val created_at: Long = System.currentTimeMillis(),

    // Timestamp when successfully synced
    val synced_at: Long? = null
)
