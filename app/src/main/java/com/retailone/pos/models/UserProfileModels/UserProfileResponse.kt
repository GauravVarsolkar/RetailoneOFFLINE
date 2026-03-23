package com.retailone.pos.models.UserProfileModels

data class UserProfileResponse(
    val `data`: Data,
    val message: String,
    val status: Int
)