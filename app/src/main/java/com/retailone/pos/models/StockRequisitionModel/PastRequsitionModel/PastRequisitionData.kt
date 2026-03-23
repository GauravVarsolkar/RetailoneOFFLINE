package com.retailone.pos.models.StockRequisitionModel.PastRequsitionModel

data class PastRequisitionData(
    val created_at: String,
    val id: Int,
    val manager_id: Any,
    val manager_incharge_conformation_date: Any,
    val order_id: String,
    val order_items: List<OrderItem>,
    val pi_date: String,
    val status: String,
    val store_id: String,
    val updated_at: String
)