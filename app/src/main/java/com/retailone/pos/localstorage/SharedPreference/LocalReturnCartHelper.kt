package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import com.google.gson.Gson
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnedItem
import com.google.gson.reflect.TypeToken // ✅ Required for TypeToken

object LocalReturnCartHelper {

    private const val PREF_KEY = "RETURNED_ITEMS_LIST"

    fun saveSingleItem(context: Context, item: ReturnedItem) {
        val currentList = getCartItems(context).toMutableList()

        // Remove if already exists with same ID
        currentList.removeAll { it.id == item.id }

        currentList.add(item) // Add new entry

        val json = Gson().toJson(currentList)
        context.getSharedPreferences("RETURN_CART_PREF", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY, json)
            .apply()
    }

    fun getCartItems(context: Context): List<ReturnedItem> {
        val json = context.getSharedPreferences("RETURN_CART_PREF", Context.MODE_PRIVATE)
            .getString(PREF_KEY, null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<ReturnedItem>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun clearCart(context: Context) {
        context.getSharedPreferences("RETURN_CART_PREF", Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_KEY)
            .apply()
    }
    fun saveList(context: Context, list: List<ReturnedItem>) {
        val json = Gson().toJson(list)
        context.getSharedPreferences("RETURN_CART_PREF", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY, json)
            .apply()
    }

}
