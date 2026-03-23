package com.retailone.pos.localstorage.RoomDB

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch

class Converters {

    private val gson = Gson()

    // ✅ EXISTING: Batch list converters (keep these)
    @TypeConverter
    fun toBatchJson(batches: List<PosSaleBatch>): String {
        return gson.toJson(batches)
    }

    @TypeConverter
    fun fromBatchJson(json: String): List<PosSaleBatch> {
        if (json.isEmpty()) return emptyList()
        val type = object : TypeToken<List<PosSaleBatch>>() {}.type
        return gson.fromJson(json, type)
    }

}
