package com.retailone.pos.models.PointofsaleModel.PosAddToCartModel

import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.DispatchBatchDetails

data class CartProductItem(
    val distribution_pack_id: Int,
    val product_id: Int,
   /// val quantity: String,
//    val quantity: Int=0,
   /// val retail_price: String

    val batch: List<BatchCartItem>


)