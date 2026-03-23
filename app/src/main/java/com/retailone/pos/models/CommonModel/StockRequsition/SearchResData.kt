package com.retailone.pos.models.CommonModel.StockRequsition

data class SearchResData(
    val barcode: String,
    val category_id: String,

    val distribution_pack_id: Int,
    val no_of_packs: Int,
    val pack: Int,
    val pack_product_description: String,
    val product_description: String,
    val product_id: String,
    val product_name: String,
    val product_photo: String,
    val quantity: String,
    val stock_quantity: String,
    val uom: String?,
    val price: Double,
    val reorder_level: String,
    val store_stock: String,



    val cart_quantity:String = "0" //manually added extra
)


//"product_id": 2,
//"product_name": "Groundnut Oil",
//"product_description": "Groundnut Oil",
//"product_photo": "uploads/products/1729001862.jpg",
//"category_id": 1,
//"uom": "2",
//"quantity": 95,
//"barcode": "12345",
//"distribution_pack_id": 4,
//"pack_product_description": "1 x 12",
//"no_of_packs": 12,
//"stock_quantity": 95