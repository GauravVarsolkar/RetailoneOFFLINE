package com.retailone.pos.localstorage.RoomDB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StoreSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: StoreSettingsEntity)

    @Query("SELECT * FROM store_settings WHERE storeId = :storeId")
    suspend fun getSettingsByStoreId(storeId: String): StoreSettingsEntity?

    @Query("SELECT * FROM store_settings")
    suspend fun getAllSettings(): List<StoreSettingsEntity>
}
