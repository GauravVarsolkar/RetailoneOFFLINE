package com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel

data class PastReqDetailsRes (
    val `data`: List<PastReqDetailsList>,
    val message: String,
    val status: Int
)
