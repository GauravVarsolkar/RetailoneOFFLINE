package com.retailone.pos.models.MaterialRcvModel

data class MaterialReceivedReq(
    val driver_name: String,
    val order_items: List<MatReceivedItem>,
    val purchase_request_id: Int,
    val vehicle_no: String
)