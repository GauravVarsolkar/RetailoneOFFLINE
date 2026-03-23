package com.retailone.pos.models.CashupModel.SendOTP

data class SendOtpRes(
    val `data`: SendOtpData,
    val message: String,
    val status: Int
)