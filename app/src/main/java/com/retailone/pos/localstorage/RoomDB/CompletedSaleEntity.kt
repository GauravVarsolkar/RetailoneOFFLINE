package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_sales")
data class CompletedSaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Basic identifiers (for search and filtering)
    val invoice_id: String,
    val store_id: Int,
    val sale_id: Int,

    // Store COMPLETE sale data as JSON (exactly as received from API)
    val sale_data_json: String,  // The entire SalesData object as JSON

    // Timestamp for 7-day cleanup
    val created_at: Long = System.currentTimeMillis()
)
