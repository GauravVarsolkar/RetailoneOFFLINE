import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StockReturnResponse(
    val data: List<StockReturn>
) : Parcelable

@Parcelize
data class StockReturn(
    val id: Int,
    val status: Int,
    val requested_date: String,
    val products: List<ReturnedProduct>
) : Parcelable

@Parcelize
data class ReturnedProduct(
    val id: Int,
    val stock_return_id: Int,
    val product_id: Int,
    val quantity: Int,
    val current_stock: Int,
    val approved_quantity: Int?,
    val received_quantity: Int?,

    val remarks: String?,
    val created_at: String?,
    val updated_at: String?,
    val condition: String,
    val rejected: Int?,
    val seal_no: String?,
    val remark: String?,
    val product: Product?            // 👈 Nested object
) : Parcelable

@Parcelize
data class Product(
    val id: Int,
    val product_name: String,
    val product_description: String,
    val type: String,
    val category_id: Int,
    val tax_id: Int,
    val photo: String?,
    val photo_name: String?,
    val deleted_at: String?,
    val created_at: String?,
    val updated_at: String?,
    val status: Int
) : Parcelable

/*// StockReturnResponse.kt
data class StockReturnResponse(
    val data: List<StockReturn>
)

data class StockReturn(
    val id: Int,
    val status: Int,
    val requested_date: String,
    val products: List<ReturnedProduct>
)

data class ReturnedProduct(
    val id: Int,
    val stock_return_id: Int,
    val product_id: Int,
    val quantity: Int,
    val current_stock: Int,
    val approved_quantity: Int?,
    val remarks: String?,
    val created_at: String?,
    val updated_at: String?,
    val condition: String
)*/
