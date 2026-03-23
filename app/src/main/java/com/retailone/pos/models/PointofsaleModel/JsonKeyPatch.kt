package com.retailone.pos.models.PointofsaleModel

import com.google.gson.*
import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSaleReq

fun PosSaleReq.toPatchedJson(): JsonObject {
    val gson = Gson()
    val root = gson.toJsonTree(this).asJsonObject
    renameKeyRecursive(root, oldKey = "batch_no", newKey = "batchno")
    return root
}

private fun renameKeyRecursive(element: JsonElement, oldKey: String, newKey: String) {
    when {
        element.isJsonObject -> {
            val obj = element.asJsonObject
            val toRename = mutableListOf<Pair<String, JsonElement>>()
            for ((k, v) in obj.entrySet()) {
                if (k == oldKey) toRename += k to v
                renameKeyRecursive(v, oldKey, newKey)
            }
            // perform renames after iteration
            for ((k, v) in toRename) {
                obj.remove(k)
                obj.add(newKey, v)
            }
        }
        element.isJsonArray -> {
            element.asJsonArray.forEach { renameKeyRecursive(it, oldKey, newKey) }
        }
        else -> Unit
    }
}
