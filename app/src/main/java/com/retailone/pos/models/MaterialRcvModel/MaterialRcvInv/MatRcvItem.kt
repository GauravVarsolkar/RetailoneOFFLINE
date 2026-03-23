package com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv

import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.DispatchBatchDetails

data class MatRcvItem(
    val id: Int,
    var received_quantity: Int,
    var batch_list:List<DispatchBatchDetails>
)