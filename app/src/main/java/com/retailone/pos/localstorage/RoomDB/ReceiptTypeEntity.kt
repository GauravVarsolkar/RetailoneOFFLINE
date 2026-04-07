package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_types")
data class ReceiptTypeEntity(
    val id: Int,
    @PrimaryKey
    val code: String,
    val name: String,
    val useYn: String?,
    val created_at: String?,
    val updated_at: String?
)
