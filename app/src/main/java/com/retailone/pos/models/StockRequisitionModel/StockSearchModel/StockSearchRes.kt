package com.retailone.pos.models.StockRequisitionModel.StockSearchModel

import com.retailone.pos.models.CommonModel.StockRequsition.SearchResData

data class StockSearchRes(
    val `data`: List<SearchResData>,
    val message: String,
    val status: Int,
    val store_exposure: Any?,
    val actual_store_exposure_limit: Any?
)