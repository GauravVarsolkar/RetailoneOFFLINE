package com.retailone.pos.models.LoginModels

data class LoginRequest(
    val email: String,
    val password: String,
    val device_id:String
)
