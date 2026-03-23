package com.retailone.pos.models.SalesPaymentModel.SalesList

data class SalesItem(
    val created_at: String,
    val distribution_pack_id: Int,
    val distribution_pack_name: String,
    val id: Int,
    val product_id: Int,
    val product_name: String,
    val quantity: Int,
    val retail_price: Int,
    val sales_id: String,
    val status: Int,
    val total_amount: Int,
    val updated_at: String,
    val whole_sale_price: Int
)