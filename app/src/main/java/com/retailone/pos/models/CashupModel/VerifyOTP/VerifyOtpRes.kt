package com.retailone.pos.models.CashupModel.VerifyOTP

data class VerifyOtpRes(
    val `data`: VerifyOtpData,
    val message: String,
    val status: Int
)