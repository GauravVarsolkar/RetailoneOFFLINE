package com.retailone.pos.localstorage.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey val storeId: String,
    val isSpotDiscountEnabled: Boolean,
    val spotDiscountLimit: String
)
