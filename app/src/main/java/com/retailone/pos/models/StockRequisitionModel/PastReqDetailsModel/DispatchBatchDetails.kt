package com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel

data class DispatchBatchDetails (
    val batch_no: String,
    val quantity: Int,
    val expiry_date: String,
    var received_quantity: Int = 0,

)


