package com.retailone.pos.models.ProductInventoryModel.PiResponseModel

data class CategoryData(
    val category_id: String,
    val category_name: String,
    val products: List<Product>
)