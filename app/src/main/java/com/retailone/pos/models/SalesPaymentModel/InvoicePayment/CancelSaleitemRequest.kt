package com.retailone.pos.models.SalesPaymentModel.InvoicePayment



import com.google.gson.annotations.SerializedName

data class CancelSaleitemRequest(
    @SerializedName("invoice_id")
    val invoiceID: String
)

