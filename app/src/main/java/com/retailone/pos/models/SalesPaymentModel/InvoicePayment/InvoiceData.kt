package com.retailone.pos.models.SalesPaymentModel.InvoicePayment

data class InvoiceData(
    val invoices_paid: Int,
    val invoices_unpaid: Int,
    val payments_due: Double,
    val payments_received: Double,
    val sales: List<Sale>,
    val total_invoice_amount: Double
)