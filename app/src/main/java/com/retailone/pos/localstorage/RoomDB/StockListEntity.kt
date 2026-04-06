package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Entity(tableName = "warehouse_stock_list")
data class StockListEntity(
    @PrimaryKey
    val store_id: Int,

    val stock_list_json: String,

    val updatedAt: Long = System.currentTimeMillis()
)
