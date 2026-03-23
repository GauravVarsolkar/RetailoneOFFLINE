package com.retailone.pos.models.PointofsaleModel.SearchStroreProModel

data class SearchStoreProReq(
    val search_string: String,
    val store_id: Int,
    val customer_id: Int
)