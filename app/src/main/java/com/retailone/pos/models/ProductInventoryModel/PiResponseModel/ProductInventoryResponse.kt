package com.retailone.pos.models.ProductInventoryModel.PiResponseModel

data class ProductInventoryResponse(
    val `data`: List<CategoryData>,
    val message: String,
    val status: Int
)