package com.retailone.pos.models.GoodsToWarehouseModel.Stock

import com.google.gson.annotations.SerializedName

/*data class StockReturnItem(
    val product_id: Int,
    var quantity: Int,
    val condition: String
)

data class StockReturnRequests(
    val store_id: Int,
    val return_date: String,
    val remarks: String?,
    val items: List<StockReturnItem>
)*/
data class StockReturnRequests(
    @SerializedName("store_id")
    val store_id: Int,

    @SerializedName("return_date")
    val return_date: String,

    @SerializedName("remark")
    val remarks: String,

    @SerializedName("items")
    val items: List<StockReturnItem>
)

data class StockReturnItem(
    @SerializedName("product_id")
    val product_id: Int,

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("condition")
    val condition: String,

    @SerializedName("remark")
    val remarks: String,

    @SerializedName("batch")
    val batch_no: String,

    @SerializedName("total_bottles")
    val bottle_quantity: Int,

    @Transient val fromGoodReturnedMap: Boolean = false
)


data class  StockReturnResponses(
    val status: String,
    val message: String,
    val data: ReturnedData? = null,
    val errors: Map<String, List<String>>? = null
)

data class ReturnedData(
    val id: Int,
    val store_id: Int,
    val return_date: String,
    val status: Int,
    val items: List<ReturnedItem>
)

data class ReturnedItem(
    val id: Int,
    val product_id: Int,
    val quantity: Int,
    val current_stock: Int,
    val condition: String,
    val product: Product
)

data class Product(
    val id: Int,
    val name: String,
    val category: Category
)

data class Category(
    val id: Int,
    val name: String
)

