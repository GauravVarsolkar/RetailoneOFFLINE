package com.retailone.pos.models.LoginModels

data class ResponseData(
    val token: String,
    val store_id: Int,
    val spot_discount: SpotDiscount?  // nullable in case backend omits it
)

data class SpotDiscount(
    val is_spot_discount_enabled: Int,
    val max_spot_discount_limit: String
)