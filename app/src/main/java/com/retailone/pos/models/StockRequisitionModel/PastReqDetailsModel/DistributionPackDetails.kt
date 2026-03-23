package com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel

data class DistributionPackDetails(
    val created_at: String,
    val deleted_at: String,
    val id: Int,
    val no_of_packs: String,
    val product_description: String,
    val product_id: String,
    val status: String,
    val updated_at: String
)