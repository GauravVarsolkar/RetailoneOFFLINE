package com.retailone.pos.models.SalesPaymentModel.InvoicePayment

data class InvoiceReq(
    val store_id: Int,
    val from_date: String,
    val to_date: String,
)