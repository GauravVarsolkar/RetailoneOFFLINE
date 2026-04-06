package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Entity(tableName = "pending_goods_returns")
data class PendingGoodsReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val store_id: Int,
    val return_date: String,
    val remarks: String,
    val return_request_json: String,
    val sync_status: String = "PENDING", // PENDING, SYNCING, FAILED, SYNCED
    val sync_error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
