package com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv

data class MatRcvInvReq(
    val driver_name: String,
    val order_items: List<MatRcvOrderItem>,
    val products: List<MatInvItem>,
    val purchase_request_id: Int,
    val store_id: String,
    val vehicle_no: String,
    val stn: String,
)