package com.retailone.pos.models.LogoutModel

data class LogoutReq(
    val device_id: String,
    val user_id: String
)