package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detailed_sales")
data class DetailedSaleEntity(
    @PrimaryKey
    val invoice_id: String,

    val sale_id: Int,

    // Store complete ReturnItemData as JSON (includes batches!)
    val detailed_data_json: String,

    // Timestamp for cleanup
    val created_at: Long = System.currentTimeMillis()
)
