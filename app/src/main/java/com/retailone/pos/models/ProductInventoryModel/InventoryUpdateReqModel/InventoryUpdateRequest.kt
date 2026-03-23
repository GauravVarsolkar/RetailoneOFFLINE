package com.retailone.pos.models.ProductInventoryModel.InventoryUpdateReqModel

data class InventoryUpdateRequest(
    val products: List<InventoryProduct>,
    val store_id: String
)