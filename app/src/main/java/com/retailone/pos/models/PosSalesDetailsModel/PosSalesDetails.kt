package com.retailone.pos.models.PosSalesDetailsModel

@com.google.gson.annotations.JsonAdapter(PosSalesDetailsAdapter::class)
data class PosSalesDetails(
    val `data`: Data?,
    val message: String,
    val status: Int
)

class PosSalesDetailsAdapter : com.google.gson.JsonDeserializer<PosSalesDetails> {
    override fun deserialize(
        json: com.google.gson.JsonElement,
        typeOfT: java.lang.reflect.Type,
        context: com.google.gson.JsonDeserializationContext
    ): PosSalesDetails {
        val obj = json.asJsonObject
        val status = obj.get("status")?.asInt ?: 0
        val message = obj.get("message")?.asString ?: ""
        
        val dataElement = obj.get("data")
        val data: Data? = try {
            if (dataElement != null && dataElement.isJsonObject) {
                context.deserialize(dataElement, Data::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
        
        return PosSalesDetails(data, message, status)
    }
}