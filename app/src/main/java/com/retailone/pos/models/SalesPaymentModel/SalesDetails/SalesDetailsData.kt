package com.retailone.pos.models.SalesPaymentModel.SalesDetails


data class SalesDetailsData(
    val amount_tendered: Int,
    val created_at: String,
    val customer: Customer,
    val discount_amount: Double,
    val grand_total: Double,
    val id: Int,
    val invoice_id: String,
    val payment_type: String,
    val sales_items: List<SalesItem>,
    val status: Int,
    val store_details: StoreDetails,
    val store_id: Int,
    val store_manager_details: StoreManagerDetails,
    val store_manager_id: Int,
    val sub_total: Double,
    val subtotal_after_discount: Double,
    val tax: Int,
    val discount: Int,
    val tax_amount: Double,
    val updated_at: String,
    val total_refunded_amount: Double,
    val summary: Summary? = null
)
data class Summary(
    val total_sub_total: Double,
    val total_tax_amount: Double,
    val total_total_amount: Double
)