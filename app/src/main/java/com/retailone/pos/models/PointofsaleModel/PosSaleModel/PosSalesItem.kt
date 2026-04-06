package com.retailone.pos.models.PointofsaleModel.PosSaleModel

import com.retailone.pos.models.PointofsaleModel.BatchCartItems
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.BatchCartItem

data class PosSalesItem(
    val distribution_pack_id: String,
    val product_id: String,
   // val quantity: Int,
    ///val quantity: String,
  ///  val retail_price: String,
    val total_amount: String,
    val whole_sale_price: String,
    val batch: List<BatchCartItem>,

    val product_name: String,
    val distribution_pack_name: String,
    val uom: String,
    // NEW: offline per-item taxes (if available from API response)
    val tax: Double? = null,
    val tax_amount: Double? = null,
    val discount: Int? = null
)



