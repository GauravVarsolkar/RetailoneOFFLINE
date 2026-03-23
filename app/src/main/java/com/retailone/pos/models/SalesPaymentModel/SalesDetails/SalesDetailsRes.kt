package com.retailone.pos.models.SalesPaymentModel.SalesDetails

data class SalesDetailsRes(
    val `data`: List<SalesDetailsData>,
    val message: String,
    val status: Int
)