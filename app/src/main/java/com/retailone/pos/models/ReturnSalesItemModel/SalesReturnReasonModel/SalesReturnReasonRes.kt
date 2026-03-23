package com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel

data class SalesReturnReasonRes(
    val `data`: List<ReturnReasonData>,
    val message: String,
    val status: Int
)