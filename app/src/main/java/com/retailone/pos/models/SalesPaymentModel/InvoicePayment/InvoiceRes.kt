package com.retailone.pos.models.SalesPaymentModel.InvoicePayment

data class InvoiceRes(
    val `data`: InvoiceData,
    val message: String,
    val status: Int
)