package com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel

data class CategoryDetails(
    val category_name: String,
    val category_slug: String,
    val created_at: String,
    val deleted_at: String,
    val icon: String,
    val id: Int,
    val status: String,
    val updated_at: String
)