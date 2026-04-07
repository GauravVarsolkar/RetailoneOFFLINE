package com.retailone.pos.models.PointofsaleModel.PosAddToCartModel

data class  PosAddToCartReq(
    val store_id: Int,
    val spot_discount_percentage: Double,
    val products: List<CartProductItem>
)