package com.retailone.pos.models.PointofsaleModel.SearchStoreProBarcodeModel

import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData

data class SearchStoreProBarcodeRes(
    val `data`: List<StoreProData>,
    val message: String,
    val status: Int
)