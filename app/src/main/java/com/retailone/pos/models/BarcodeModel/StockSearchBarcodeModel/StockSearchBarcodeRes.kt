package com.retailone.pos.models.BarcodeModel.StockSearchBarcodeModel

import com.retailone.pos.models.CommonModel.StockRequsition.SearchResData

data class StockSearchBarcodeRes(
    val `data`: List<SearchResData>,
    val message: String,
    val status: Int
)