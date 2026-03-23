package com.retailone.pos.models.ChangePinModel

data class ChangePinResponse(
    val `data`: ChangePinData,
    val message: String,
    val status: Int
)