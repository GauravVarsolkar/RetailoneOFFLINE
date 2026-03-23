package com.retailone.pos.models.PointofsaleModel

data class PointOfSaleItem(
    val Banner: List<Banner> = emptyList(),
    val img_path: String = "",
    val status: Int = 0
)


/*
data class PointOfSaleItem(
    val Banner: List<Banner>,
    val img_path: String,
    val status: Int
)*/
