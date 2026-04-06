package com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel

data class ReturnedItem(
    val id: Int,
    val return_quantity: Int,
    val defective_boxes: Int? = null,
    val defective_bottles: Int? = null,
    val product_id: Int? = null,
    val distribution_pack_id: Int? = null
)