package com.retailone.pos.models.SalesPaymentModel.SalesDetails

data class Customer(
    val created_at: String,
    val customer_mob_no: String,
    val customer_name: String?,
    val id: Int,
    val sales_id: String,
    val status: Int,
    val updated_at: String
)