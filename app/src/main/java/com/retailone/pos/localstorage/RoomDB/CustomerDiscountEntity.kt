package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity

@Entity(tableName = "customer_discounts", primaryKeys = ["customer_id", "product_id", "distribution_pack_id", "batch_no"])
data class CustomerDiscountEntity(
    val customer_id: Int,
    val product_id: Int,
    val distribution_pack_id: Int,
    val batch_no: String,
    val discount: Double
)
