package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales_details")
data class SalesDetailsEntity(
    @PrimaryKey
    val sale_id: Int,

    val invoice_id: String,

    // Store complete SalesDetailsRes data as JSON
    val sales_details_json: String,

    // Timestamp for cleanup (7 days)
    val created_at: Long = System.currentTimeMillis()
)
