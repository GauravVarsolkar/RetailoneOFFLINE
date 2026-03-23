package com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv

import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.DispatchBatchDetails

data class MatRcvOrderItem(
    val id: Int,
    var received_quantity: Int,
)
