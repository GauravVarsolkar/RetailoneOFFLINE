package com.retailone.pos.models.PointofsaleModel.SearchStroreProModel

import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData

data class SearchStoreProRes(
    val `data`: List<StoreProData>,
    val message: String,
    val status: Int
)