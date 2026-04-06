package com.retailone.pos.models.GoodsToWarehouseModel.ReturnStocks

data class ReturnBatchItem(
    val productId: Int,
    val productName: String, // ✅ ADDED
    val stockqqty: Int,
    val batchNo: String,
    val noOfPacks: Int,
    val condition: String,
    val returnedQty: Int,
    val expiry_date: String,
    val isEditable: Boolean = true,
    val fromGoodReturnedMap: Boolean = false,
    val totalBottles: Int = 0
)
