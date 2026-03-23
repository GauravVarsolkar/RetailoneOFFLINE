package com.retailone.pos.models.SendOTPModel

data class ResponseData(
    val mobile: String,
    val otp_code: Int,
    val otp_hash_key: String
)