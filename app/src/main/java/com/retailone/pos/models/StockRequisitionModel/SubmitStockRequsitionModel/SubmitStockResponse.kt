package com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel

data class SubmitStockResponse(
    val `data`: SubmitResponseData,
    val message: String,
    val status: Int
)