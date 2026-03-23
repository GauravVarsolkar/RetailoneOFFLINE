package com.retailone.pos.models.SalesPaymentModel.SalesDetails

data class SalesItem(
    val created_at: String,
    val distribution_pack: DistributionPack,
    val distribution_pack_id: Int,
    val distribution_pack_name: String,
    val id: Int,
    val product: Product,
    val product_id: Int,
    val product_name: String,
    val quantity: Double,
    val retail_price: Double,
    val sales_id: String,
    val status: Int,
    val total_amount: Double,
    val sub_total: Double,
    val total_quantity: Double,
    val updated_at: String,
    val tax: Int,
    val whole_sale_price: Double,
    val tax_amount: Double,
    val discount: Int
)