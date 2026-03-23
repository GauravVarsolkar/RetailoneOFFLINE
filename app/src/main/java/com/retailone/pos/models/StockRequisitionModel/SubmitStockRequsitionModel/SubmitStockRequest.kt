package com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel

data class SubmitStockRequest(
    val purchase_items: List<PurchaseItem>,
    val store_id: String
)