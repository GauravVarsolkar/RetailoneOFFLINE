package com.retailone.pos.models.SendOTPModel

data class SendOtpResponse(
    val `data`: ResponseData,
    val message: String,
    val status: Int
)