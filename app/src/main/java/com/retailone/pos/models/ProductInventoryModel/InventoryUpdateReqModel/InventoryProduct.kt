package com.retailone.pos.models.ProductInventoryModel.InventoryUpdateReqModel

data class InventoryProduct(
    val category_id: String,
    val distribution_pack_id: Int,
    val product_id: String,
    val quantity: String,
    val supplier_id: String
)