package com.retailone.pos.models.ReturnSalesItemModel

data class ReturnCalculation(
    val subTotal: Double,
    val taxAmount: Double,
    val grandTotal: Double
)
