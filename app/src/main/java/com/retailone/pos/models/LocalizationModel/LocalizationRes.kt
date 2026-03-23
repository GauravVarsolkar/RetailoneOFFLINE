package com.retailone.pos.models.LocalizationModel

data class LocalizationRes(
    val `data`: LocalizationData,
    val message: String,
    val status: Int
)