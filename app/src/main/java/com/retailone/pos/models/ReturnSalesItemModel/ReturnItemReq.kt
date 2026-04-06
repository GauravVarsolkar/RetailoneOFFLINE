package com.retailone.pos.models.ReturnSalesItemModel

data class ReturnItemReq(
    val invoice_id: String,
    val store_id: String? = null
)