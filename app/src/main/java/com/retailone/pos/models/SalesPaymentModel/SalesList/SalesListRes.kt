package com.retailone.pos.models.SalesPaymentModel.SalesList

data class SalesListRes(
    val `data`: List<SalesListData>,
    val message: String,
    val status: Int
)