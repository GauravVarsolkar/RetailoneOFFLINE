package com.retailone.pos.models.SalesPaymentModel.SalesList

data class SalesListReq(
    val end_date: String,
    val start_date: String,
    val store_id: String
)