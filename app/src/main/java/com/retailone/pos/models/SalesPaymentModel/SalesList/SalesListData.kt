package com.retailone.pos.models.SalesPaymentModel.SalesList

data class SalesListData(
    val amount_tendered: Int,
    val created_at: String,
    val discount_amount: Int,
    val grand_total: Double,
    val id: Int,
    val invoice_id: String,
    val payment_type: String?,
    val sales_items: List<SalesItem>,
    val status: Int,
    val store_details: StoreDetails,
    val store_id: Int,
    val store_manager_details: StoreManagerDetails,
    val store_manager_id: Int,
    val sub_total: Int,
    val subtotal_after_discount: Int,
    val tax: Int,
    val tax_amount: Double,
    val updated_at: String
)