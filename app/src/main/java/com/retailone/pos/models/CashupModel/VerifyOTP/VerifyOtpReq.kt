package com.retailone.pos.models.CashupModel.VerifyOTP

data class VerifyOtpReq(
    val mobile_no: String,
    val otp: String
)