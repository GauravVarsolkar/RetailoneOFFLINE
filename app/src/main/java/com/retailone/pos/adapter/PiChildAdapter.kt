//package com.retailone.pos.adapter
//
//import NumberFormatter
//import android.content.Context
//import android.os.Build
//import android.view.Gravity
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import android.widget.PopupWindow
//import android.widget.TextView
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.RecyclerView
//import com.retailone.pos.R
//import com.retailone.pos.databinding.HeaderrowlayoutBinding
//
//
//import com.retailone.pos.databinding.PiChildItemLayoutBinding
//import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
//import com.retailone.pos.models.ProductInventoryModel.PiChildData
//import com.retailone.pos.models.ProductInventoryModel.PiChildRow
//import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.DistributionPackData
//import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.InventoryField
//import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.toUiFields
//import com.retailone.pos.utils.FunUtils
//import kotlin.math.absoluteValue
//
//class PiChildAdapter(
//    private val context: Context,
//    private val packList: List<DistributionPackData>
//) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//    private val localizationData = LocalizationHelper(context).getLocalizationData()
//    companion object {
//        private const val TYPE_HEADER = 0
//        private const val TYPE_ITEM = 1
//    }
//    //third
//    private val rows: List<PiChildRow> = packList.flatMap { pack ->
//        val stockQty = pack.stock_quatity
//        val header = PiChildRow.Header(pack.batch_no ?: "-", stockQty,pack )
//        val children = mutableListOf<PiChildRow.Item>()
//
//        val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            java.time.LocalDate.now()
//        } else {
//            TODO("VERSION.SDK_INT < O")
//        }
//        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
//
//        val expiryDate = try {
//            java.time.LocalDate.parse(pack.expiry_date ?: "", formatter)
//        } catch (e: Exception) {
//            null
//        }
//
//        val isExpired = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            expiryDate != null && expiryDate.isBefore(today)
//        } else {
//            TODO("VERSION.SDK_INT < O")
//        }
//
//        // 1. Add auto-tagged stock items
//        if (stockQty > 0) {
//            val autoStatus = if (isExpired) "Expired" else "Store Stock"
//            children.add(
//                PiChildRow.Item(
//                    status = autoStatus,
//                    qty = stockQty.toInt(),
//                    data = pack
//                )
//            )
//        }
//
//        // 2. Add manual returned_items
////        pack.returned_items?.forEach { (status, qty) ->
////            if (!status.equals("Good", ignoreCase = true) && qty > 0) {
////                children.add(
////                    PiChildRow.Item(
////                        status = status,
////                        qty = qty,
////                        data = pack
////                    )
////                )
////            }
////        }
//
//        pack.returned_items?.forEach { (status, details) ->
//            val qty = details.total_quantity   // <-- was Int before, now from ReturnedItemDetails
//
//            if (!status.equals("Good", ignoreCase = true) && qty > 0) {
//                children.add(
//                    PiChildRow.Item(
//                        status = status,
//                        qty = qty,
//                        data = pack
//                    )
//                )
//            }
//        }
//
//
//        // 3. Add good_returned_items
//        // 3. Add good_returned_items but skip "Good"
////        pack.good_returned_items?.forEach { (status, qty) ->
////            if (/*!status.equals("Good", ignoreCase = true) && */qty > 0) {
////                children.add(
////                    PiChildRow.Item(
////                        status = status,
////                        qty = qty,
////                        data = pack
////                    )
////                )
////            }
////        }
//
//        pack.good_returned_items?.forEach { (status, details) ->
//            val qty = details.total_quantity
//
//            if (qty > 0) {
//                children.add(
//                    PiChildRow.Item(
//                        status = status,
//                        qty = qty,
//                        data = pack
//                    )
//                )
//            }
//        }
//
//
//
//        // Return header + child if any items exist
//        if (children.isNotEmpty()) listOf(header) + children else emptyList()
//    }
//
//
//
//
//    // ViewHolders
//    inner class HeaderViewHolder(val binding: HeaderrowlayoutBinding) : RecyclerView.ViewHolder(binding.root)
//    inner class ItemViewHolder(val binding: PiChildItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)
//
//    override fun getItemViewType(position: Int): Int {
//        return when (rows[position]) {
//            is PiChildRow.Header -> TYPE_HEADER
//            is PiChildRow.Item -> TYPE_ITEM
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        return if (viewType == TYPE_HEADER) {
//            val binding = HeaderrowlayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//            HeaderViewHolder(binding)
//        } else {
//            val binding = PiChildItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//            ItemViewHolder(binding)
//        }
//    }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        when (val row = rows[position]) {
//            is PiChildRow.Header -> {
//                val headerHolder = holder as HeaderViewHolder
//                headerHolder.binding.txtBatchHeader.text = "Batch: ${row.batchNo}"
//
//                val pack = row.data
//                val price = NumberFormatter().formatPrice(
//                    pack.retail_price ?: "-",
//                    localizationData
//                )
//                headerHolder.binding.txtBatchPrice.text = price
//            }
//
//            is PiChildRow.Item -> {
//                val itemHolder = holder as ItemViewHolder
//                val pack = row.data
//
//                // LEFT SIDE
//                itemHolder.binding.txtBatchName.text = pack.pack_description ?: "-"
//                itemHolder.binding.txtExpiry.text = pack.expiry_date
//
//                // ---- RIGHT SIDE DYNAMIC LIST ----
//
//                // 1. Get ReturnedItemDetails for this status (from returned_items or good_returned_items)
//                val details = pack.returned_items?.get(row.status)
//                    ?: pack.good_returned_items?.get(row.status)
//
//                // 2. Build the list of UI fields:
//                val fieldList: List<InventoryField> = if (details != null) {
//                    // Use reflection-based extension
//                    details.toUiFields()
//                } else {
//                    // For auto "Store Stock" / "Expired" created from stock_quatity
//                    listOf(
//                        InventoryField(
//                            label = "Quantity",
//                            value = row.qty.toString()
//                        )
//                    )
//                }
//
//                // 3. Bind the inner RecyclerView
//                itemHolder.binding.rvDetails.apply {
//                    if (layoutManager == null) {
//                        layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
//                            context,
//                            androidx.recyclerview.widget.LinearLayoutManager.VERTICAL,
//                            false
//                        )
//                    }
//                    adapter = InventoryFieldAdapter(fieldList)
//                }
//
//                // STATUS BADGE (same as you already did)
//                val status = row.status.trim()
//                val badgeText = status
//                    .split(" ")
//                    .filter { it.isNotEmpty() }
//                    .joinToString("") { it.first().uppercaseChar().toString() }
//                itemHolder.binding.viewStatus.text = badgeText
//
//                val badgeColorResId = when (status) {
//                    "Good" -> R.color.grad1s
//                    "Damaged", "Defective" -> R.color.badge_orange
//                    "Expired" -> R.color.badge_red
//                    "Store Stock" -> R.color.badge_green
//                    else -> {
//                        val colorIndex = (status.hashCode().absoluteValue % 10) + 1
//                        context.resources.getIdentifier(
//                            "badge_color_$colorIndex",
//                            "color",
//                            context.packageName
//                        )
//                    }
//                }
//                itemHolder.binding.viewStatus.backgroundTintList =
//                    ContextCompat.getColorStateList(context, badgeColorResId)
//
//                // Your popup on click = unchanged
//                itemHolder.binding.viewStatus.setOnClickListener { view ->
//                    val popupView = LayoutInflater.from(context)
//                        .inflate(R.layout.layout_status_popup, null)
//                    val popupText = popupView.findViewById<TextView>(R.id.txtStatusDetail)
//                    popupText.text = status
//
//                    val popupWindow = PopupWindow(
//                        popupView,
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        true
//                    )
//
//                    val location = IntArray(2)
//                    view.getLocationOnScreen(location)
//
//                    popupWindow.showAtLocation(
//                        view,
//                        Gravity.NO_GRAVITY,
//                        location[0] + view.width / 2,
//                        location[1] - view.height - 20
//                    )
//
//                    popupWindow.isOutsideTouchable = true
//                    popupWindow.setBackgroundDrawable(
//                        ContextCompat.getDrawable(context, android.R.color.transparent)
//                    )
//                }
//            }
//        }
//    }
//
//
////    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
////        when (val row = rows[position]) {
////            is PiChildRow.Header -> {
////                val headerHolder = holder as HeaderViewHolder
////                // headerHolder.binding.txtBatchHeader.text = "Batch: ${row.batchNo} | Stock: ${row.stock.toInt()}"
////                headerHolder.binding.txtBatchHeader.text = "Batch: ${row.batchNo}"
////                val pack = row.data   // DistributionPackData for this header
////                val price = NumberFormatter().formatPrice(
////                    pack.retail_price ?: "-",
////                    localizationData
////                )
////                headerHolder.binding.txtBatchPrice.text = price
////            }
////            is PiChildRow.Item -> {
////                val itemHolder = holder as ItemViewHolder
////                val pack = row.data
////
////                val quantityFromPack =
////                    pack.returned_items?.get(row.status)?.total_quantity
////                        ?: pack.good_returned_items?.get(row.status)?.total_quantity
////
////                val totalMissingButtol = pack.returned_items?.get(row.status)?.total_missing_bottles
////                    ?: pack.good_returned_items?.get(row.status)?.total_missing_bottles
////
////                itemHolder.binding.txtBatchName.text = pack.pack_description ?: "-"
////
////                itemHolder.binding.txtExpiry.text = pack.expiry_date
////                itemHolder.binding.txtQuantity.text = "Total QTY: ${quantityFromPack}"
////               // val price = NumberFormatter().formatPrice(pack.retail_price ?: "-", localizationData)
////                itemHolder.binding.txtPrice.text = "Total Bottles: ${totalMissingButtol}"
////                // val status = row.status.lowercase().trim()
////                // val badgeText = status.firstOrNull()?.uppercaseChar()?.toString() ?: "-"
////                val status = row.status.trim()
////                val badgeText = status
////                    .split(" ")
////                    .filter { it.isNotEmpty() }
////                    .joinToString("") { it.first().uppercaseChar().toString() }
////                itemHolder.binding.viewStatus.text = badgeText
////
////                val badgeColorResId = when (status) {
////                    "Good" -> R.color.grad1s
////                    "Damaged", "Defective" -> R.color.badge_orange
////                    "Expired" -> R.color.badge_red
////                    "Store Stock" -> R.color.badge_green
////                    else -> {
////                        val colorIndex = (status.hashCode().absoluteValue % 10) + 1
////                        context.resources.getIdentifier("badge_color_$colorIndex", "color", context.packageName)
////                    }
////                }
////                itemHolder.binding.viewStatus.backgroundTintList =
////                    ContextCompat.getColorStateList(context, badgeColorResId)
////
////                itemHolder.binding.viewStatus.setOnClickListener { view ->
////
////                    // Inflate the custom tooltip layout
////                    val popupView = LayoutInflater.from(context).inflate(R.layout.layout_status_popup, null)
////                    val popupText = popupView.findViewById<TextView>(R.id.txtStatusDetail)
////                    // popupText.text = "$badgeText - $status"
////                    popupText.text = "$status"
////
////                    // Create the PopupWindow
////                    val popupWindow = PopupWindow(
////                        popupView,
////                        ViewGroup.LayoutParams.WRAP_CONTENT,
////                        ViewGroup.LayoutParams.WRAP_CONTENT,
////                        true // Focusable to dismiss on outside touch
////                    )
////
////                    // Show above-right of viewStatus
////                    val location = IntArray(2)
////                    view.getLocationOnScreen(location)
////
////                    popupWindow.showAtLocation(view, Gravity.NO_GRAVITY,
////                        location[0] + view.width / 2, // X: half right
////                        location[1] - view.height - 20 // Y: above with padding
////                    )
////
////                    // Optional: close when clicking outside
////                    popupWindow.isOutsideTouchable = true
////                    popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.color.transparent))
////                }
////
////
////            }
////        }
////    }
//
//    override fun getItemCount(): Int = rows.size
//}


package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.databinding.HeaderrowlayoutBinding
import com.retailone.pos.databinding.PiChildItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.ProductInventoryModel.PiChildRow
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.DistributionPackData
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.InventoryField
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.ReturnedItemDetails
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.toUiFields
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

class PiChildAdapter(
    private val context: Context,
    private val packList: List<DistributionPackData>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val localizationData = LocalizationHelper(context).getLocalizationData()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    // ✅ FIX: Improved date handling for all Android versions
    private val rows: List<PiChildRow> = packList.flatMap { pack ->
        val stockQty = pack.stock_quatity ?: 0.0
        val header = PiChildRow.Header(pack.batch_no ?: "-", stockQty, pack)
        val children = mutableListOf<PiChildRow.Item>()

        // ✅ FIX: Better date handling that works on all Android versions
        val isExpired = checkIfExpired(pack.expiry_date)

        // 1. Add auto-tagged stock items
        if (stockQty > 0) {
            val autoStatus = if (isExpired) "Expired" else "Store Stock"
            children.add(
                PiChildRow.Item(
                    status = autoStatus,
                    qty = stockQty.toInt(),
                    data = pack
                )
            )
        }

        // 2. Add manual returned_items
        pack.returned_items?.forEach { (status, details: ReturnedItemDetails) ->
            val qty = details["total_quantity"] ?: details.values.sum()
            if (qty > 0) {
                children.add(
                    PiChildRow.Item(
                        status = status,
                        qty = qty,
                        data = pack
                    )
                )
            }
        }

        // 3. Add good_returned_items
        pack.good_returned_items?.forEach { (status, details: ReturnedItemDetails) ->
            val qty = details["total_quantity"] ?: details.values.sum()
            if (qty > 0) {
                children.add(
                    PiChildRow.Item(
                        status = status,
                        qty = qty,
                        data = pack
                    )
                )
            }
        }

        // Return header + children if any items exist
        if (children.isNotEmpty()) listOf(header) + children else emptyList()
    }

    // ✅ NEW: Date checking function that works on all Android versions
    private fun checkIfExpired(expiryDateStr: String?): Boolean {
        if (expiryDateStr.isNullOrBlank()) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use Java 8 Time API for newer versions
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val expiryDate = java.time.LocalDate.parse(expiryDateStr, formatter)
                val today = java.time.LocalDate.now()
                expiryDate.isBefore(today)
            } else {
                // Use Calendar for older versions
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expiryDate = sdf.parse(expiryDateStr)
                val today = Calendar.getInstance().time
                expiryDate?.before(today) ?: false
            }
        } catch (e: Exception) {
            false // If date parsing fails, assume not expired
        }
    }

    inner class HeaderViewHolder(val binding: HeaderrowlayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class ItemViewHolder(val binding: PiChildItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is PiChildRow.Header -> TYPE_HEADER
            is PiChildRow.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = HeaderrowlayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = PiChildItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is PiChildRow.Header -> {
                val headerHolder = holder as HeaderViewHolder
                headerHolder.binding.txtBatchHeader.text = "Batch: ${row.batchNo}"

                val pack = row.data
                val price = NumberFormatter().formatPrice(
                    pack.retail_price ?: "-",
                    localizationData
                )
                headerHolder.binding.txtBatchPrice.text = price
            }

            is PiChildRow.Item -> {
                val itemHolder = holder as ItemViewHolder
                val pack = row.data

                // LEFT SIDE
                itemHolder.binding.txtBatchName.text = pack.pack_description ?: "-"
                itemHolder.binding.txtExpiry.text = pack.expiry_date ?: "-"

                // Get ReturnedItemDetails for this status
                val details: ReturnedItemDetails? =
                    pack.returned_items?.get(row.status)
                        ?: pack.good_returned_items?.get(row.status)

                // Build dynamic fields list
                val fieldList: List<InventoryField> = if (details != null) {
                    details.toUiFields()
                } else {
                    listOf(
                        InventoryField(
                            label = "Quantity",
                            value = row.qty.toString()
                        )
                    )
                }

                // Bind inner RecyclerView
                itemHolder.binding.rvDetails.apply {
                    if (layoutManager == null) {
                        layoutManager = LinearLayoutManager(
                            itemHolder.itemView.context,
                            LinearLayoutManager.VERTICAL,
                            false
                        )
                    }
                    adapter = InventoryFieldAdapter(fieldList)
                }

                // STATUS BADGE
                val status = row.status.trim()
                val badgeText = status
                    .split(" ")
                    .filter { it.isNotEmpty() }
                    .joinToString("") { it.first().uppercaseChar().toString() }
                itemHolder.binding.viewStatus.text = badgeText

                val badgeColorResId = when (status) {
                    "Good" -> R.color.grad1s
                    "Damaged", "Defective" -> R.color.badge_orange
                    "Expired" -> R.color.badge_red
                    "Store Stock" -> R.color.badge_green
                    else -> {
                        val colorIndex = (status.hashCode().absoluteValue % 10) + 1
                        itemHolder.itemView.context.resources.getIdentifier(
                            "badge_color_$colorIndex",
                            "color",
                            itemHolder.itemView.context.packageName
                        )
                    }
                }
                itemHolder.binding.viewStatus.backgroundTintList =
                    ContextCompat.getColorStateList(context, badgeColorResId)

                // Popup on badge click
                itemHolder.binding.viewStatus.setOnClickListener { view ->
                    val popupView = LayoutInflater.from(context)
                        .inflate(R.layout.layout_status_popup, null)
                    val popupText = popupView.findViewById<TextView>(R.id.txtStatusDetail)
                    popupText.text = status

                    val popupWindow = PopupWindow(
                        popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true
                    )

                    val location = IntArray(2)
                    view.getLocationOnScreen(location)

                    popupWindow.showAtLocation(
                        view,
                        Gravity.NO_GRAVITY,
                        location[0] + view.width / 2,
                        location[1] - view.height - 20
                    )

                    popupWindow.isOutsideTouchable = true
                    popupWindow.setBackgroundDrawable(
                        ContextCompat.getDrawable(context, android.R.color.transparent)
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int = rows.size
}
