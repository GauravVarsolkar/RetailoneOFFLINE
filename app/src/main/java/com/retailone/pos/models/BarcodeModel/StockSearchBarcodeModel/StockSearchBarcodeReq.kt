package com.retailone.pos.models.BarcodeModel.StockSearchBarcodeModel

data class StockSearchBarcodeReq(
    val barcode: String,
    val store_id: String
)