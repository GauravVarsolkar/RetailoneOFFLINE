package com.retailone.pos.models.ProductInventoryModel.PiResponseModel

/*
data class DistributionPackData(
    val id: String,
    val no_of_packs: Int,
    val stock_quatity: Double,
    val pack_description: String,
    val retail_price: String?,
    val expiry_date: String?,
    val batch_no: String?,
)*/
//data class DistributionPackData(
//    val id: String,
//    val no_of_packs: Int,
//    val stock_quatity: Double,
//    val pack_description: String,
//    val retail_price: String?,
//    val expiry_date: String?,
//    val batch_no: String?,
//    val returned_items: Map<String, Int>? ,// <-- Important for Good, Expired, Defective
//    val good_returned_items: Map<String, Int>? // <-- Important for Good, Expired, Defective
//)

data class DistributionPackData(
    val id: String,
    val no_of_packs: Int,
    val stock_quatity: Double,
    val pack_description: String,
    val retail_price: String?,
    val expiry_date: String?,
    val batch_no: String?,
    val returned_items: Map<String, ReturnedItemDetails>?,      // e.g. "others"
    val good_returned_items: Map<String, ReturnedItemDetails>?  // e.g. "expired", "defective", "no_sell"
)

typealias ReturnedItemDetails = Map<String, Int>



//data class ReturnedItemDetails(
//    val total_quantity: Int,
//    val total_missing_bottles: Int
//)
// A simple label–value pair for UI
data class InventoryField(
    val label: String,
    val value: String
)

// Now ReturnedItemDetails is Map<String, Int>
fun ReturnedItemDetails.toUiFields(): List<InventoryField> {
    return this
        .filter { (_, value) -> value != 0 }  // optional: skip zero values
        .map { (key, value) ->
            val label = key
                .replace('_', ' ')               // total_missing_bottles -> total missing bottles
                .replaceFirstChar { it.uppercaseChar() } // Total missing bottles
            InventoryField(label, value.toString())
        }
}

