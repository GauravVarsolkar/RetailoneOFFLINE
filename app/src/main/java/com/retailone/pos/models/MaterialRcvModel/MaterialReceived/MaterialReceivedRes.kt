package com.retailone.pos.models.MaterialRcvModel

data class MaterialReceivedRes(
    val `data`: MatReceivedResData,
    val message: String,
    val status: Int
)
