package com.retailone.pos.models.ProductInventoryModel.InventoryUpdateResModel

data class InventoryUpdateResponse(
    val `data`: Data,
    val message: String,
    val status: Int
)