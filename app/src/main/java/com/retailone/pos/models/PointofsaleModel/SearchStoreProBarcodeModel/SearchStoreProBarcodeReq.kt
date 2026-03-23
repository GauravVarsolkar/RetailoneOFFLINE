package com.retailone.pos.models.PointofsaleModel.SearchStoreProBarcodeModel

data class SearchStoreProBarcodeReq(
    val customer_id: Int,
    val search_string: String,
    val store_id: Int
)