package com.retailone.pos.models.ChangePinModel

data class ChangePinRequest(
    val mobile_no: String,
    val pin: String
)