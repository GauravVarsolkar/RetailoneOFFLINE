package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val email: String,
    val pin: String,
    val token: String,
    val storeId: String,
    val storeManagerId: String,
    val cashupDateTime: String,
    val userProfileJson: String // Stores the full UserProfileResponse as JSON
)
