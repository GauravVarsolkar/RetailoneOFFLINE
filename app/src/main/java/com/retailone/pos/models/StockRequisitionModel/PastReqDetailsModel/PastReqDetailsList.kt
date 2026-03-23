package com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel

data class PastReqDetailsList(
    val id: Int,
    val manager_id: Any,
    val vehicle_no: String?,
    val driver_name: String?,
    val order_id: String?,
    val manager_incharge_conformation_date: Any,
    val order_items: List<OrderItem>,
    val pi_date: String,
    val status: String,
    val store_id: String,
    val updated_at: String,

    val approve_date: String?,
    val receive_date: String?,
    val created_at: String?,
    val dispatch_date: String?
)