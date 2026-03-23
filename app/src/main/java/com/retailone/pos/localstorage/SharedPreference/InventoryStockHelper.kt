package com.retailone.pos.localstorage.SharedPreference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData

class InventoryStockHelper(context: Context) {

    private val inventoryPreferences: SharedPreferences =
        context.getSharedPreferences("Inventory", Context.MODE_PRIVATE)

    private val gson = Gson()

    // Save the list to SharedPreferences
    fun saveSearchResultsList(searchResultsList: List<StoreProData>) {
        val json = gson.toJson(searchResultsList)
        inventoryPreferences.edit().putString("inventorySearchList", json).apply()
    }

    // Retrieve the list from SharedPreferences
    fun getSearchResultsList(): List<StoreProData> {
        val json = inventoryPreferences.getString("inventorySearchList", "")
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            gson.fromJson(json, object : TypeToken<List<StoreProData>>() {}.type)
        }
    }


    fun saveSearchItem(newItem: StoreProData) {
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
            inventoryPreferences.edit().putString("inventorySearchList", json).apply()
        }
    }

    // Update the quantity of an item based on product_id and distribution_pack_id
    fun updateQuantity(product_id: Int, distribution_pack_id: Int, cartQuantity: Int) {
        val existingList = getSearchResultsList().toMutableList()

        // Find the item with the specified product_id and distribution_pack_id
        val existingItemIndex = existingList.indexOfFirst {
            it.product_id == product_id &&
                    it.distribution_pack_id == distribution_pack_id
        }

        if (existingItemIndex != -1) {
            // If the item exists, update its quantity
            val updatedItem = existingList[existingItemIndex].copy(cart_quantity = cartQuantity.toDouble())
            existingList[existingItemIndex] = updatedItem

            // Save the updated list to SharedPreferences
            val json = gson.toJson(existingList)
            inventoryPreferences.edit().putString("inventorySearchList", json).apply()
        }
    }


//    fun updateQuantity(product_id: Int, distribution_pack_id: Int, cartQuantity: Int) {
//        val existingList = getSearchResultsList().toMutableList()
//
//        // Find the item with the specified product_id and distribution_pack_id
//        val existingItemIndex = existingList.indexOfFirst {
//            it.product_id == product_id &&
//                    it.distribution_pack_id == distribution_pack_id
//        }
//
//        if (existingItemIndex != -1) {
//            // If the item exists, update its quantity
//            val updatedItem = existingList[existingItemIndex].copy(cart_quantity = cartQuantity)
//            existingList[existingItemIndex] = updatedItem
//
//            // Save the updated list to SharedPreferences
//            val json = gson.toJson(existingList)
//            inventoryPreferences.edit().putString("inventorySearchList", json).apply()
//        }
//    }



    // Remove an item based on product_id and distribution_pack_id
    fun removeItem(product_id: String, distribution_pack_id: Int) {
        val existingList = getSearchResultsList().toMutableList()

        // Remove the item with the specified product_id and distribution_pack_id
        existingList.removeIf {
            it.product_id == product_id.toInt() && it.distribution_pack_id == distribution_pack_id
        }

        // Save the updated list to SharedPreferences
        val json = gson.toJson(existingList)
        inventoryPreferences.edit().putString("inventorySearchList", json).apply()
    }

    // Clear all items from the search results list
    fun clearSearchResultsList() {
        inventoryPreferences.edit().remove("inventorySearchList").apply()
    }

}
