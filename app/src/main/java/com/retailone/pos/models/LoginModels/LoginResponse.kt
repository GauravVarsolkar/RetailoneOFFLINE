package com.retailone.pos.models.LoginModels

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

data class LoginResponse(
    val `data`: ResponseData,
    val message: String,
    val status: Int,
    val cashup_date_time: JsonElement? = JsonPrimitive("0")
)