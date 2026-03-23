package com.retailone.pos.models.StockRequisitionModel.PastRequsitionModel

data class OrderItem(
    val approved_quantity: String,
    val category_id: String,
    val created_at: String,
    val distribution_pack_id: String,
    val id: Int,
    val pi_id: String,
    val product_id: String,
    val quantity_request: String,
    val received_quantity: String,
    val remark: Any,
    val status: String,
    val updated_at: String
)