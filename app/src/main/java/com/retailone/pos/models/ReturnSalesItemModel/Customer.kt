package com.retailone.pos.models.ReturnSalesItemModel

data class Customer(
    val created_at: String,
    val customer_mob_no: Any,
    val customer_name: Any,
    val id: Int,
    val sales_id: String,
    val status: Int,
    val updated_at: String
)