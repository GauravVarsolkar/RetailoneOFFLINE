package com.retailone.pos.models.StockRequisitionModel.StockSearchModel

data class StockSearchReq(
    val search_string: String,
    val store_id: String
)