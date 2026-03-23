package com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel

data class ProductDetails(
    val barcode: String,
    val category_id: String,
    val created_at: String,
    val deleted_at: String,
    val id: Int,
    val photo: String,
    val product_description: String,
    val product_name: String,
    val quantity: String,
    val status: String,
    val stock_life_time: String,
    val stock_type: String,
    // supluer id static empty
    val supplier_id:String? ,

    val tax_id: String,
    val type: String,
    val uom: String?,
    val updated_at: String
)