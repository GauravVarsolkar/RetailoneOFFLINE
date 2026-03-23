package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retailone.pos.models.CommonModel.StockRequsition.SearchResData
import kotlin.math.roundToInt

class SharedPrefHelper(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private val gson = Gson()
    private val KEY_FINAL_EXPOSURE = "final_exposure_value"

    fun setFinalExposure(value: Double) {
        sharedPreferences.edit().putInt(KEY_FINAL_EXPOSURE, value.roundToInt()).apply()
    }

    fun getFinalExposure(): Int {
        return sharedPreferences.getInt(KEY_FINAL_EXPOSURE, 0)
    }


    // Save the list to SharedPreferences
    fun saveSearchResultsList(searchResultsList: List<SearchResData>) {
        val json = gson.toJson(searchResultsList)
        sharedPreferences.edit().putString("searchResultsList", json).apply()
    }

    // Retrieve the list from SharedPreferences
    fun getSearchResultsList(): List<SearchResData> {
        val json = sharedPreferences.getString("searchResultsList", "")
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            gson.fromJson(json, object : TypeToken<List<SearchResData>>() {}.type)
        }
    }

    //✅ Step 1: Add Method in `SharedPrefHelper.kt`
/*
    fun getTotalCartValue(): Double {
        val itemList = getSearchResultsList()
        var total = 0.0
        for (item in itemList) {
            val price = item.price?.toDoubleOrNull() ?: 0.0
            val qty = item.cart_quantity?.toIntOrNull() ?: 0
            total += price * qty
        }
        return total
    }*/
    fun getTotalCartValue(): Double {
        val itemList = getSearchResultsList()
        var total = 0.0
        for (item in itemList) {
            val price = item.price ?: 0.0
            val qty = item.cart_quantity.toIntOrNull() ?: 0
            val pack = item.no_of_packs

            if (qty > 0 && pack > 0) {
                total += price * qty * pack
            }
            //total += price * qty
        }
        return total
    }




    fun saveSearchItem(newItem: SearchResData) {
        val existingList = getSearchResultsList().toMutableList()

        // Check if the item with the same product_id and distribution_pack_id already exists
        val itemExists = existingList.any {
            it.product_id == newItem.product_id &&
                    it.distribution_pack_id == newItem.distribution_pack_id
        }

        if (!itemExists) {
            // If the item doesn't exist, add it to the list
            existingList.add(newItem)
            val json = gson.toJson(existingList)
            sharedPreferences.edit().putString("searchResultsList", json).apply()
        }
    }

    // Update the quantity of an item based on product_id and distribution_pack_id
    fun updateQuantity(product_id: String, distribution_pack_id: Int, cartQuantity: String) {
        val existingList = getSearchResultsList().toMutableList()

        // Find the item with the specified product_id and distribution_pack_id
        val existingItemIndex = existingList.indexOfFirst {
            it.product_id == product_id &&
                    it.distribution_pack_id == distribution_pack_id
        }

        if (existingItemIndex != -1) {
            // If the item exists, update its quantity
            val updatedItem = existingList[existingItemIndex].copy(cart_quantity = cartQuantity)
            existingList[existingItemIndex] = updatedItem

            // Save the updated list to SharedPreferences
            val json = gson.toJson(existingList)
            sharedPreferences.edit().putString("searchResultsList", json).apply()
        }
    }


    // Remove an item based on product_id and distribution_pack_id
    fun removeItem(product_id: String, distribution_pack_id: Int) {
        val existingList = getSearchResultsList().toMutableList()

        // Remove the item with the specified product_id and distribution_pack_id
        existingList.removeIf {
            it.product_id == product_id && it.distribution_pack_id == distribution_pack_id
        }

        // Save the updated list to SharedPreferences
        val json = gson.toJson(existingList)
        sharedPreferences.edit().putString("searchResultsList", json).apply()
    }

    fun clearStockList() {
        sharedPreferences.edit().remove("searchResultsList").apply()
    }


    // Check if a product is already added based on product_id and distribution_pack_id
    fun isProductAdded(product_id: String, distribution_pack_id: Int): Boolean {
        val existingList = getSearchResultsList()
        return existingList.any {
            it.product_id == product_id && it.distribution_pack_id == distribution_pack_id
        }
    }

    // Get the quantity of a specific product based on product_id and distribution_pack_id
    fun getQuantity(product_id: String, distribution_pack_id: Int): String {
        val existingList = getSearchResultsList()
        val selectedItem = existingList.find {
            it.product_id == product_id && it.distribution_pack_id == distribution_pack_id
        }
        return selectedItem?.cart_quantity ?: "0"
    }

}
