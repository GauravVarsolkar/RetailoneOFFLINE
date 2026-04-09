package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Entity(tableName = "pending_dispatches")
data class PendingDispatchEntity(
    @PrimaryKey(autoGenerate = true) val local_id: Int = 0,
    val return_id: Int, // The server return ID
    val seal_no: String,
    val vehicle_no: String,
    val driver_name: String,
    val dispatch_request_json: String,
    val sync_status: String = "PENDING", // PENDING, SYNCING, FAILED, SYNCED
    val sync_error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
