package com.retailone.pos.models.GoodsToWarehouseModel.Stocklist

data class StockListResponse(
    val status: Int,
    val message: String,
    val data: List<CategoryData>
)

data class CategoryData(
    val category_id: Int,
    val category_name: String,
    val products: List<Product>
)

data class Product(
    val product_id: Int,
    val previous_requested_quantity: Int,
    val product_name: String,
    val photo: String,
    val distribution_pack_data: List<DistributionPack>,
    var isExpanded: Boolean = false
)

data class DistributionPack(
    val id: Int,
    val pack_description: String,
    val no_of_packs: Int,
    val stock_quatity: Int,
    val retail_price: Double,
    val expiry_date: String,
    val batch_no: String,
//    val returned_items: Map<String, Int>? = emptyMap(),// ✅ new field
//    val good_returned_items: Map<String, Int>? = emptyMap() // ✅ new field

    val returned_items: Map<String, ReturnedItemDetails>?,      // e.g. "others"
    val good_returned_items: Map<String, ReturnedItemDetails>?
)

typealias ReturnedItemDetails = Map<String, Int>


// total_bottles
