package com.retailone.pos.models.CashupModel.SendOTP

data class SendOtpData(
    val mobile: String,
    val otp_code: Int,
    val otp_hash_key: String
)