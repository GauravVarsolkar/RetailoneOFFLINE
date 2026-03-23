package com.retailone.pos.models.BarcodeModel.StockSearchBarcodeModel

data class SearchData(
    val barcode: String,
    val category_id: Int,
    val distribution_pack_id: Int,
    val no_of_packs: Int,
    val pack_product_description: String,
    val product_description: String,
    val product_id: Int,
    val product_name: String,
    val product_photo: String,
    val quantity: Int,
    val retail_price: String,
    val stock_quantity: Int,
    val supplier_id: String,
    val uom: String,
    val whole_sale_price: String
)