package com.retailone.pos.models.SalesPaymentModel.InvoicePayment

data class Sale(
    val amount_tendered: Double,
    val created_at: String,
    val discount_amount: Double,
    val grand_total: Double,
    val id: Int,
    val invoice_id: String,
    val payment_type: String,
    val status: Int,
    val store_id: Int,
    val store_manager_id: Int,
    val sub_total: Double,
    val subtotal_after_discount: Double,
    val tax: Double,
    val tax_amount: Double,
    val updated_at: String
)