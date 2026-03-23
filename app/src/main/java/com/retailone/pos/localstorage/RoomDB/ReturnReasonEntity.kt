package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "return_reasons")
data class ReturnReasonEntity(
    @PrimaryKey
    val id: Int,

    val reason_name: String,

    // Store complete ReturnReasonData as JSON (in case there are other fields)
    val reason_data_json: String,

    // Timestamp for tracking
    val created_at: Long = System.currentTimeMillis()
)
