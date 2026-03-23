package com.retailone.pos.models.PointofsaleModel.PosSaleModel

data class PosSaleRes(
    val `data`: PosResponseData,
    val message: String,
    val status: Int
)