package com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv

import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.DispatchBatchDetails

data class MatInvItem(
    val category_id: String,
    val distribution_pack_id: Int,
    val product_id: String,
    val quantity: String,
    val supplier_id: String,
    val expiry_date: String,
    val batch_no: List<DispatchBatchDetails>
)