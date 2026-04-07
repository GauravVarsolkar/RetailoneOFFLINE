package com.retailone.pos.models.ReturnSalesItemModel


data class BatchReturnItem (
    val batch: String?,
    val quantity: Double?,
    val retail_price: Double?,
    val tax_exclusive_price: Double?,   // may be null in the batches array
    val subtotal: Double?,
    val sales_item_id: Int?,
    var return_quantity: Int?,
    val return_reason: String?,
    var batch_return_quantity: Int = 0, //manually added extra
    var batch_refund_amount: Double? = 0.0,//manually added extra
    var remark: String? = null,

    var defective_boxes: Int? = 0,        // No. of Box
    var defective_bottles: Int? = 0,       // No. of Packs

    var product_id: Int? = 0,             // added for robust matching
    var distribution_pack_id: Int? = 0,    // added for robust matching
    var discount: Double? = 0.0           // added for discount calculation
)

/*
data class BatchReturnItem (

    val batch: String,
    val quantity: Double,
    val retail_price: Double,
    val tax_exclusive_price:Double?,
    val subtotal: Double,
    val sales_item_id: Int,
    var return_quantity: Int,
    val return_reason: String?,
    var batch_return_quantity: Int = 0, //manually added extra
    var batch_refund_amount: Double = 0.0,//manually added extra
    var remark: String? = null

)*/
