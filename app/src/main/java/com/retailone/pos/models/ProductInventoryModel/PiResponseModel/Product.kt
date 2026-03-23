package com.retailone.pos.models.ProductInventoryModel.PiResponseModel

data class Product(
    val distribution_pack_data: List<DistributionPackData>,
    val product_id: String,
    val product_name: String,
    val photo: String
)