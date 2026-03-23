package com.retailone.pos.models.VerifyOtpModel

data class VerifyOtpRequest(
    val mobile_no: String,
    val otp: String
)