package com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel

data class PurchaseItem(
    val category_id: Int,
    val distribution_pack_id: Int,
    val product_id: Int,
    val requested_quantity: Int,
    val whole_sale_price: String,
    val retail_price: String
)