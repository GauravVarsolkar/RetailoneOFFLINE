package com.retailone.pos.models.ReturnSalesItemModel

data class ReturnItemRes(
    val `data`: List<ReturnItemData>,
    val message: String,
    val status: Int
)