package com.retailone.pos.models.LogoutModel

data class LogoutRes(
    val `data`: List<Any>,
    val message: String,
    val status: Int
)