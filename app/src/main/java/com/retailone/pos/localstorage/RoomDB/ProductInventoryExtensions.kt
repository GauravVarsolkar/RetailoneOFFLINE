package com.retailone.pos.localstorage.RoomDB

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.*

/**
 * Convert ProductInventoryResponse (API) to List<ProductInventoryEntity> (Database)
 */
fun ProductInventoryResponse.toEntities(storeId: Int): List<ProductInventoryEntity> {
    val entities = mutableListOf<ProductInventoryEntity>()
    val gson = Gson()

    this.data.forEach { category ->
        category.products.forEach { product ->
            product.distribution_pack_data.forEach { pack ->

                val compositeKey = "${storeId}_${category.category_id}_${product.product_id}_${pack.id}"

                val entity = ProductInventoryEntity(
                    compositeKey = compositeKey,
                    store_id = storeId,
                    category_id = category.category_id,
                    category_name = category.category_name,
                    product_id = product.product_id,
                    product_name = product.product_name,
                    product_photo = product.photo,
                    distribution_pack_id = pack.id,
                    no_of_packs = pack.no_of_packs,
                    stock_quantity = pack.stock_quatity,
                    pack_description = pack.pack_description,
                    retail_price = pack.retail_price,
                    expiry_date = pack.expiry_date,
                    batch_no = pack.batch_no,
                    returned_items_json = gson.toJson(pack.returned_items),
                    good_returned_items_json = gson.toJson(pack.good_returned_items)
                )

                entities.add(entity)
            }
        }
    }

    return entities
}

/**
 * Convert List<ProductInventoryEntity> (Database) to ProductInventoryResponse (API format)
 */
fun List<ProductInventoryEntity>.toInventoryResponse(): ProductInventoryResponse {
    val gson = Gson()
    val type = object : TypeToken<Map<String, ReturnedItemDetails>>() {}.type

    // Group by category
    val groupedByCategory = this.groupBy { it.category_id }

    val categoryDataList = groupedByCategory.map { (categoryId, items) ->

        // Group by product within category
        val groupedByProduct = items.groupBy { it.product_id }

        val productList = groupedByProduct.map { (productId, productItems) ->

            val firstItem = productItems.first()

            // Map distribution packs
            val distributionPacks = productItems.map { entity ->

                val returnedItems: Map<String, ReturnedItemDetails>? = try {
                    gson.fromJson(entity.returned_items_json, type)
                } catch (e: Exception) {
                    null
                }

                val goodReturnedItems: Map<String, ReturnedItemDetails>? = try {
                    gson.fromJson(entity.good_returned_items_json, type)
                } catch (e: Exception) {
                    null
                }

                DistributionPackData(
                    id = entity.distribution_pack_id,
                    no_of_packs = entity.no_of_packs,
                    stock_quatity = entity.stock_quantity,
                    pack_description = entity.pack_description,
                    retail_price = entity.retail_price,
                    expiry_date = entity.expiry_date,
                    batch_no = entity.batch_no,
                    returned_items = returnedItems,
                    good_returned_items = goodReturnedItems
                )
            }

            Product(
                product_id = firstItem.product_id,
                product_name = firstItem.product_name,
                photo = firstItem.product_photo,
                distribution_pack_data = distributionPacks
            )
        }

        CategoryData(
            category_id = categoryId,
            category_name = items.first().category_name,
            products = productList
        )
    }

    return ProductInventoryResponse(
        data = categoryDataList,
        message = "Loaded from offline cache",
        status = 1
    )
}
