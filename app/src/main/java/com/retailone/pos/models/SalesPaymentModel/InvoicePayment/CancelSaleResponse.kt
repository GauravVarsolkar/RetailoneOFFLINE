package com.retailone.pos.models.SalesPaymentModel.InvoicePayment

data class CancelSaleResponse(
    val status: Int,
    val message: String,
    val data: CancelSaleData?
)

data class CancelSaleData(
    val original_invoice_id: String,
    val reversal_invoice_id: String,
    val sales_id: Int,
    val reversal_sales_id: Int
)

