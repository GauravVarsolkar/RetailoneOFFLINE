package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Entity(tableName = "stock_return_list")
data class StockReturnEntity(
    @PrimaryKey
    val store_id: Int,

    val stock_return_json: String,

    val updatedAt: Long = System.currentTimeMillis()
)
