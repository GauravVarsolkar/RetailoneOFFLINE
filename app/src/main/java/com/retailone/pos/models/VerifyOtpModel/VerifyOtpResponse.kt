package com.retailone.pos.models.VerifyOtpModel

data class VerifyOtpResponse(
    val `data`: VerifyData,
    val message: String,
    val status: Int
)