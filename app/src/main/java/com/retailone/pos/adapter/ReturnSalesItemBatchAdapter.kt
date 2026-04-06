package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.retailone.pos.R
import com.retailone.pos.databinding.ReturnItemBatchLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalReturnCartHelper
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.ReturnSalesItemModel.BatchReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnedItem
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Locale

// ---- helper: robust rounding to 2dp ----
private fun round2(v: Double?): Double =
    BigDecimal.valueOf(v ?: 0.0).setScale(2, RoundingMode.HALF_UP).toDouble()

class ReturnSalesItemBatchAdapter(
    private val returnitems: List<ReturnItemData>,
    val context: Context,
    val returnbatchlist: List<BatchReturnItem>,
    val readonlyMode: Boolean = false,
    private val returnReasonName: String = "Not Given",
    val onBatchQuantityChange: (List<BatchReturnItem>) -> Unit
) : RecyclerView.Adapter<ReturnSalesItemBatchAdapter.StockSearchViewHolder>() {

    private val sharedPrefHelper = SharedPrefHelper(context)
    private val localizationData = LocalizationHelper(context).getLocalizationData()
    private val filteredList = returnbatchlist.filter { (it.quantity ?: 0.0) > 0.0 }

    // Global list to maintain received quantities
    private val matReceivedList = mutableListOf<BatchReturnItem>()

    // Avoid stacking watchers due to recycling
    private val qtyWatchers = mutableMapOf<RecyclerView.ViewHolder, TextWatcher>()
    private val boxWatchers = mutableMapOf<RecyclerView.ViewHolder, TextWatcher>()
    private val packWatchers = mutableMapOf<RecyclerView.ViewHolder, TextWatcher>()

    var showDiscount: Boolean = false


    init {
        matReceivedList.clear()
        setHasStableIds(true)
    }

    class StockSearchViewHolder(val binding: ReturnItemBatchLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    private fun batchText(item: BatchReturnItem): String {
        return try {
            val obj = JSONObject(Gson().toJson(item))
            when {
                obj.has("batch") -> obj.optString("batch")
                obj.has("batch_no") -> obj.optString("batch_no")
                obj.has("batchNumber") -> obj.optString("batchNumber")
                obj.has("batch_name") -> obj.optString("batch_name")
                obj.has("lot") -> obj.optString("lot")
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSearchViewHolder {
        return StockSearchViewHolder(
            ReturnItemBatchLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }



    private fun discountForBatch(item: BatchReturnItem): Double {
        val batchName = batchText(item).trim()
        val detailedSales = returnitems
            .asSequence()
            .mapNotNull { it.sales_items }
            .flatten()

        // Try exact match on id + batch
        val exact = detailedSales.firstOrNull { si ->
            si.id == item.sales_item_id &&
                    (si.batch?.trim()?.equals(batchName, ignoreCase = true) ?: false)
        }

        var discount = exact?.discount ?: 0.0

        // Fallback 1: match only by sales_item_id
        if (discount <= 0.0) {
            val byId = detailedSales.firstOrNull { it.id == item.sales_item_id }
            discount = byId?.discount ?: 0.0
        }

        // Fallback 2: match only by product_id (good for offline sales)
        if (discount <= 0.0) {
            val byProdId = detailedSales.firstOrNull { it.product_id == item.product_id }
            discount = byProdId?.discount ?: 0.0
        }

        // Fallback 3: single item invoice
        if (discount <= 0.0 && detailedSales.toList().size == 1) {
            discount = detailedSales.firstOrNull()?.discount ?: 0.0
        }

        Log.d(
            "ReturnAdapter",
            "discountForBatch -> sales_item_id=${item.sales_item_id}, p_id=${item.product_id} discount=$discount"
        )

        return discount.coerceAtLeast(0.0)
    }

    // ---- unit price (rounded 2dp) with fallback ----
    private fun unitPriceRounded(batch: BatchReturnItem): Double {
        var v: Double? = batch.tax_exclusive_price
        if (v == null) {
            val detailed = returnitems.firstOrNull()?.sales_items
            v = detailed?.firstOrNull { it.id == batch.sales_item_id }?.tax_exclusive_price
        }
        return BigDecimal.valueOf(v ?: 0.0).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    // ---- packs-per-box from API (SalesItemDetailed.distribution_pack.no_of_packs) ----
    private fun basePacksPerBox(item: BatchReturnItem): Int {
        val byIdAndBatch =
            returnitems.asSequence().mapNotNull { it.sales_items }.flatten().firstOrNull { si ->
                si.id == item.sales_item_id && (si.batch?.trim()
                    ?.equals(item.batch?.trim() ?: "", ignoreCase = true) ?: false)
            }?.distribution_pack?.no_of_packs

        if (byIdAndBatch != null) return byIdAndBatch

        // Fallback: match by sales_item_id only
        return returnitems.asSequence().mapNotNull { it.sales_items }.flatten()
            .firstOrNull { si -> si.id == item.sales_item_id }?.distribution_pack?.no_of_packs
            ?: 1 // safe default so packs != always 0
    }

    private fun keyOf(item: BatchReturnItem): String =
        "${item.sales_item_id}::${item.batch?.trim()?.lowercase() ?: ""}"

    override fun onBindViewHolder(holder: StockSearchViewHolder, position: Int) {
        val rowItem = filteredList[position]

        // Clear old watchers (recycling)
        qtyWatchers[holder]?.let { holder.binding.returnQuantity.removeTextChangedListener(it) }
        boxWatchers[holder]?.let { holder.binding.etNoOfBox.removeTextChangedListener(it) }
        packWatchers[holder]?.let { holder.binding.etNumberOfPacks.removeTextChangedListener(it) }

        val formattedPrice = NumberFormatter().formatPrice(
            rowItem.retail_price.toString(), localizationData
        )
        // ✅ FIX: Use item-specific tax and reason instead of top-level invoice tax
        val itemTaxRaw = returnitems.firstOrNull()?.salesItems?.find { 
            it.id == rowItem.sales_item_id || (it.product_id == (rowItem.product_id ?: 0) && it.distribution_pack_id == (rowItem.distribution_pack_id ?: 0))
        }?.tax
        val itemReasonRaw = returnitems.firstOrNull()?.salesItems?.find { 
            it.id == rowItem.sales_item_id || (it.product_id == (rowItem.product_id ?: 0) && it.distribution_pack_id == (rowItem.distribution_pack_id ?: 0))
        }?.return_reason
        val taxToUse = itemTaxRaw?.toString() ?: returnitems.firstOrNull()?.tax ?: "0"
        val taxDisplay = formatTaxForDisplay(taxToUse)

        holder.binding.apply {
            if (readonlyMode) {
                // READ-ONLY UI
                quantityLayouts.isVisible = true
                batchNames.text = rowItem.batch
                batchPrices.text = formattedPrice
                saleQuantitys.text = rowItem.quantity.toString()
                returnQuantitys.isEnabled = false
                returnQuantitys.isFocusable = false
                returnQuantitys.setText((rowItem.return_quantity ?: 0).toString())
                returnReasonLayout.isVisible = true
                returnReason.isVisible = true
                returnReason.isSelected = true
                returnReason.isEnabled = false
                returnReason.isFocusable = false
                paymentcards.isVisible = true

                // Hide box/pack section in read-only
                boxPackLayout.isVisible = false
                cbClick.isChecked = false
                boxInputs.isVisible = false
                etNoOfBox.text = null
                etNumberOfPacks.text = null

                calculateTotalss(holder, rowItem)
                returnReason.setText(returnReasonName)  // ✅ Use the passed reason name
                
                // (Tax label is handled inside calculateTotalss)
            } else {
                // EDITABLE UI
                quantityLayout.isVisible = true
                batchName.text = rowItem.batch
                batchPrice.text = formattedPrice
                saleQuantity.text = rowItem.quantity.toString()
                paymentcard.isVisible = true
                returnReasonLayout.isVisible = false
                returnReason.isVisible = false

                // If user already entered qty earlier, keep it
                if ((rowItem.batch_return_quantity ?: 0) > 0) {
                    returnQuantity.setText(rowItem.batch_return_quantity.toString())
                } else {
                    returnQuantity.setText("")
                }
                // Prefill No. of Box from model (mirror previous qty if present)
                if ((rowItem.batch_return_quantity ?: 0) > 0) {
                    etNoOfBox.setText(rowItem.batch_return_quantity.toString())
                } else {
                    etNoOfBox.setText("")
                }

                // Prefill No. of Packs from model
                val packValInit = rowItem.return_quantity ?: 0
                if (packValInit > 0) {
                    etNumberOfPacks.setText(packValInit.toString())
                } else {
                    etNumberOfPacks.setText("")
                }

                // (Tax label is handled inside calculateTotals)

                // ----- Save/Remove state -----
                val currentList = LocalReturnCartHelper.getCartItems(context)
                val isSaved = currentList.any { it.id == rowItem.sales_item_id }

                // Qty field enabled if NOT saved (unchanged per your flow)
                val qtyEditable = !isSaved
                returnQuantity.isEnabled = qtyEditable
                returnQuantity.isFocusable = qtyEditable
                returnQuantity.isFocusableInTouchMode = qtyEditable

                // Keep the "Click" row visible ALWAYS
                boxPackLayout.isVisible = true
                cbClick.isEnabled = true
                etNumberOfPacks.isEnabled = true

                // Box editability depends only on checkbox
                boxInputs.isVisible = cbClick.isChecked
                etNoOfBox.isEnabled = cbClick.isChecked

                // --- prevent text-sync loops
                var suppressSync = false

                // utility: recompute packs from boxes ONLY; do not touch qty/totals/API qty
                fun updatePacksFromBoxes() {
                    val boxes = etNoOfBox.text?.toString()?.trim()?.toIntOrNull() ?: 0
                    val base = basePacksPerBox(rowItem).coerceAtLeast(1)
                    val packs = boxes * base

                    // ✅ store in model
                    rowItem.defective_boxes = boxes
                    rowItem.defective_bottles = packs

                    // update packs field in UI
                    suppressSync = true
                    etNumberOfPacks.setText(if (packs > 0) packs.toString() else "")
                    etNumberOfPacks.setSelection(etNumberOfPacks.text?.length ?: 0)
                    suppressSync = false

                    // totals still driven only by Qty
                    calculateTotals(holder, rowItem)

                    // notify Activity with latest model (incl. boxes & packs)
                    updateReceivedQuantity(rowItem)
                }

                // When checkbox becomes checked, mirror current Qty into No. of Box and compute packs
                cbClick.setOnCheckedChangeListener { _, checked ->
                    boxInputs.isVisible = checked
                    etNoOfBox.isEnabled = checked
                    if (checked) {
                        val q = returnQuantity.text?.toString()?.trim()?.toIntOrNull()
                        if ((q ?: 0) > 0) {
                            suppressSync = true
                            etNoOfBox.setText(q.toString())
                            etNoOfBox.setSelection(etNoOfBox.text?.length ?: 0)
                            suppressSync = false
                            updatePacksFromBoxes() // packs update only; totals untouched
                        } else {
                            etNoOfBox.text = null
                            etNumberOfPacks.text = null
                            rowItem.defective_boxes = 0
                            rowItem.defective_bottles = 0

                            // keep qty and totals as-is; only clear packs UI
                            calculateTotals(holder, rowItem)
                            updateReceivedQuantity(rowItem)
                        }
                    } else {
                        etNoOfBox.text = null
                        etNumberOfPacks.text = null

                        // keep qty and totals as-is; only clear packs UI
                        calculateTotals(holder, rowItem)
                        updateReceivedQuantity(rowItem)
                    }
                }

                // ----- Quantity watcher (validate + totals + mirror to No. of Box when checked) -----
                val qtyWatcher = object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                        val input = s.toString().trim()
                        val enteredValue = input.toIntOrNull()

                        if (input.isEmpty() || enteredValue == null || enteredValue == 0) {
                            // reset model
                            rowItem.batch_return_quantity = 0
                            rowItem.batch_refund_amount = 0.0
                            rowItem.return_quantity = 0   // API qty mirrors EditText
                            rowItem.defective_boxes = 0          // 👈 reset
                            rowItem.defective_bottles = 0        // 👈 reset

                            // UI totals -> zero
                            subtotals.text = "RWF0.00"
                            taxAmounts.text = "RWF0.00"
                            alltotalAmounts.text = "RWF0.00"

                            // also clear box/packs if checkbox is active
                            if (cbClick.isChecked && !suppressSync) {
                                suppressSync = true
                                etNoOfBox.setText("")
                                etNumberOfPacks.setText("")
                                suppressSync = false
                            }

                            // notify activity of change
                            updateReceivedQuantity(rowItem)

                            if (input.isNotEmpty()) {
                                Toast.makeText(context, "Quantity cannot be 0", Toast.LENGTH_SHORT)
                                    .show()
                                returnQuantity.text = null
                            }
                            return
                        } else {
                            if (enteredValue > (rowItem.quantity?.toInt() ?: 0)) {
                                Toast.makeText(
                                    context,
                                    "Entered quantity exceeds the quantity purchased.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                returnQuantity.text = null
                                return
                            }

                            val unitPrice = unitPriceRounded(rowItem)

                            rowItem.batch_return_quantity = enteredValue
                            rowItem.batch_refund_amount = enteredValue.toDouble() * unitPrice
                            rowItem.return_quantity =
                                enteredValue   // API field mirrors EditText qty

                            // Mirror Qty to "No. of Box" when checkbox is checked,
                            // then compute packs (packs only) + callback.
                            if (cbClick.isChecked && !suppressSync) {
                                suppressSync = true
                                etNoOfBox.setText(enteredValue.toString())
                                etNoOfBox.setSelection(etNoOfBox.text?.length ?: 0)
                                rowItem.defective_boxes = 0
                                rowItem.defective_bottles = 0
                                suppressSync = false
                                updatePacksFromBoxes()
                            } else {
                                // no checkbox => just update totals and callback
                                calculateTotals(holder, rowItem)
                                updateReceivedQuantity(rowItem)
                            }

                            // If No. of Box currently exceeds the new Qty, clamp it and recompute packs
                            val currentBox = etNoOfBox.text?.toString()?.trim()?.toIntOrNull() ?: 0
                            if (cbClick.isChecked && currentBox > enteredValue) {
                                suppressSync = true
                                etNoOfBox.setText(enteredValue.toString())
                                etNoOfBox.setSelection(etNoOfBox.text?.length ?: 0)
                                suppressSync = false
                                updatePacksFromBoxes()
                            }

                            // Also clamp packs against new max (Qty * basePacks) — UI only; do not change API qty
                            val base = basePacksPerBox(rowItem).coerceAtLeast(1)
                            val maxPacksAllowed = enteredValue * base
                            val currentPacks =
                                etNumberOfPacks.text?.toString()?.trim()?.toIntOrNull() ?: 0
                            if (currentPacks > maxPacksAllowed) {
                                Toast.makeText(
                                    context,
                                    "Packs cannot exceed $maxPacksAllowed for $enteredValue box(es).",
                                    Toast.LENGTH_SHORT
                                ).show()
                                suppressSync = true
                                etNumberOfPacks.setText(maxPacksAllowed.toString())
                                etNumberOfPacks.setSelection(etNumberOfPacks.text?.length ?: 0)
                                suppressSync = false
                                // DO NOT overwrite rowItem.return_quantity here
                                updateReceivedQuantity(rowItem)
                            }
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                }
                returnQuantity.addTextChangedListener(qtyWatcher)
                qtyWatchers[holder] = qtyWatcher

                // ----- "No. of Box" watcher (recompute packs only; do not change qty/totals/API qty) -----
                val boxWatcher = object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                        if (suppressSync) return
                        val inputStr = s?.toString()?.trim() ?: ""
                        val enteredBox = inputStr.toIntOrNull()

                        if (enteredBox == null) {
                            // cleared box -> clear packs UI and keep qty/API qty intact
                            rowItem.defective_boxes = 0
                            rowItem.defective_bottles = 0
                            suppressSync = true
                            etNumberOfPacks.setText("")
                            suppressSync = false

                            calculateTotals(holder, rowItem)   // totals still from qty
                            updateReceivedQuantity(rowItem)
                            return
                        }

                        val currentQty = returnQuantity.text?.toString()?.trim()?.toIntOrNull() ?: 0

                        if (currentQty <= 0) {
                            if (enteredBox > 0) {
                                Toast.makeText(context, "Enter Qty first.", Toast.LENGTH_SHORT)
                                    .show()
                                suppressSync = true
                                etNoOfBox.setText("")
                                etNumberOfPacks.setText("")
                                suppressSync = false

                                calculateTotals(holder, rowItem) // totals from qty (likely 0)
                                updateReceivedQuantity(rowItem)
                            }
                            return
                        }
                        if (enteredBox == 0) {
                            Toast.makeText(context, "0 is not allowed", Toast.LENGTH_SHORT).show()
                            etNoOfBox.error = "0 is not allowed"

                            suppressSync = true
                            // Fallback: set to 1 (only if there is stock); otherwise clear.
                            if (currentQty > 0) {
                                etNoOfBox.setText("1")
                            } else {
                                etNoOfBox.setText("")
                            }
                            etNoOfBox.setSelection(etNoOfBox.text?.length ?: 0)
                            suppressSync = false

                        }
                        // prevent crash + enforce limit: Box cannot exceed Qty
                        if (enteredBox > currentQty) {
                            Toast.makeText(
                                context,
                                "No. of Box cannot exceed Qty ($currentQty)",
                                Toast.LENGTH_SHORT
                            ).show()
                            suppressSync = true
                            etNoOfBox.setText(currentQty.toString())
                            etNoOfBox.setSelection(etNoOfBox.text?.length ?: 0)
                            suppressSync = false
                        }

                        // packs update only; qty/totals/API qty unaffected
                        updatePacksFromBoxes()
                    }

                    override fun afterTextChanged(s: Editable?) {}
                }
                etNoOfBox.addTextChangedListener(boxWatcher)
                boxWatchers[holder] = boxWatcher

                // ----- "No. of Packs" watcher (updates packs UI only; totals remain from qty; do NOT touch API qty) -----
                val packsWatcher = object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                        if (suppressSync) return

                        val packsInput = s?.toString()?.trim()
                        val packs = packsInput?.toIntOrNull() ?: 0
                        val currentQty = returnQuantity.text?.toString()?.trim()?.toIntOrNull() ?: 0
                        val base = basePacksPerBox(rowItem).coerceAtLeast(1)
                        val maxPacksAllowed = currentQty * base

                        // If Qty is zero, disallow packs entry (prevents invalid state)
                        if (currentQty <= 0) {
                            if (packs > 0) {
                                Toast.makeText(context, "Enter Qty first.", Toast.LENGTH_SHORT)
                                    .show()
                                suppressSync = true
                                etNumberOfPacks.setText("")
                                suppressSync = false
                            }
                            updateReceivedQuantity(rowItem)
                            return
                        }

                        // Enforce packs <= Qty * packsPerBox (UI only)
                        if (packs > maxPacksAllowed) {
                            Toast.makeText(
                                context,
                                "Packs cannot exceed $maxPacksAllowed for $currentQty box(es).",
                                Toast.LENGTH_SHORT
                            ).show()
                            suppressSync = true
                            etNumberOfPacks.setText(maxPacksAllowed.toString())
                            etNumberOfPacks.setSelection(etNumberOfPacks.text?.length ?: 0)
                            suppressSync = false
                        } else if (packs < 0) {
                            suppressSync = true
                            etNumberOfPacks.setText("0")
                            etNumberOfPacks.setSelection(etNumberOfPacks.text?.length ?: 0)
                            suppressSync = false
                        }

                        val finalPacks =
                            etNumberOfPacks.text?.toString()?.trim()?.toIntOrNull() ?: 0
                        rowItem.defective_bottles = finalPacks
                        // Do NOT overwrite rowItem.return_quantity here (API qty stays from EditText)

                        // totals are based on Qty; we only notify changes
                        updateReceivedQuantity(rowItem)
                    }

                    override fun afterTextChanged(s: Editable?) {}
                }
                etNumberOfPacks.addTextChangedListener(packsWatcher)
                packWatchers[holder] = packsWatcher

                // ----- Save/Remove toggle -----
                updateBtn.setOnClickListener {
                    val list = LocalReturnCartHelper.getCartItems(context).toMutableList()
                    val exists = list.any { it.id == rowItem.sales_item_id }

                    if (exists) {
                        // Remove from cart -> enable qty again
                        list.removeAll { it.id == rowItem.sales_item_id }
                        LocalReturnCartHelper.saveList(context, list)
                        Toast.makeText(context, "Item removed from cart", Toast.LENGTH_SHORT).show()

                        returnQuantity.isEnabled = true
                        returnQuantity.isFocusable = true
                        returnQuantity.isFocusableInTouchMode = true

                        // keep the click row visible and editable
                        cbClick.isEnabled = true
                        etNoOfBox.isEnabled = cbClick.isChecked
                        updateBtn.setImageResource(R.drawable.pencil)
                    } else {
                        val input = returnQuantity.text.toString().trim()
                        val returnQty = input.toIntOrNull()

                        if (returnQty != null && returnQty > 0) {
                            val item = ReturnedItem(
                                id = rowItem.sales_item_id ?: 0, return_quantity = returnQty
                            )
                            LocalReturnCartHelper.saveSingleItem(context, item)
                            Toast.makeText(context, "Saved to cart", Toast.LENGTH_SHORT).show()

                            // Saved -> disable Qty only. Keep Click & No. of Box editable.
                            returnQuantity.isEnabled = false
                            returnQuantity.isFocusable = false

                            cbClick.isEnabled = true
                            etNoOfBox.isEnabled = cbClick.isChecked
                            updateBtn.setImageResource(R.drawable.delete)
                        } else {
                            Toast.makeText(
                                context, "Enter valid return quantity", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                // Initial icon state
                updateBtn.setImageResource(if (isSaved) R.drawable.delete else R.drawable.pencil)
            }
        }
    }

    override fun getItemCount(): Int = filteredList.size
    override fun getItemViewType(position: Int) = position
    override fun getItemId(position: Int) = position.toLong()

    override fun onViewRecycled(holder: StockSearchViewHolder) {
        super.onViewRecycled(holder)
        qtyWatchers.remove(holder)
            ?.let { holder.binding.returnQuantity.removeTextChangedListener(it) }
        boxWatchers.remove(holder)?.let { holder.binding.etNoOfBox.removeTextChangedListener(it) }
        packWatchers.remove(holder)
            ?.let { holder.binding.etNumberOfPacks.removeTextChangedListener(it) }
        holder.binding.cbClick.setOnCheckedChangeListener(null)
    }

    // ---- READ-ONLY totals (uses item.return_quantity) ----
// ---- READ-ONLY totals (uses item.return_quantity) ----
    // ---- READ-ONLY totals (calculateTotalss) ----
    private fun calculateTotalss(holder: StockSearchViewHolder, item: BatchReturnItem) {
        // ✅ Get quantity from sales_items detailed list (same way you get prices)
        val batchName = batchText(item).trim()
        // ✅ Robust lookup: try snake_case list first, then camelCase as fallback
        val detailedItemsList = returnitems.firstOrNull()?.sales_items.orEmpty()
        val camelItemsList = returnitems.firstOrNull()?.salesItems.orEmpty()

        val detailedItem = detailedItemsList.firstOrNull { si ->
            si.id == item.sales_item_id &&
                    (batchName.isEmpty() || si.batch?.trim()?.equals(batchName, ignoreCase = true) == true)
        } ?: detailedItemsList.firstOrNull { si ->
            si.product_id == item.product_id && item.product_id != null && item.product_id != 0 &&
                    (batchName.isEmpty() || si.batch?.trim()?.equals(batchName, ignoreCase = true) == true)
        } ?: detailedItemsList.firstOrNull { si ->
            // Batch-name-only fallback: handles cases where both sales_item_id=0 and product_id=0
            // (common when offline-returning an online-made multi-item sale)
            batchName.isNotEmpty() && si.batch?.trim()?.equals(batchName, ignoreCase = true) == true
        } ?: if (detailedItemsList.size == 1) detailedItemsList[0] else null

        // If not found in snake_case, check camelCase (common for offline-merged sales)
        val camelItem = if (detailedItem == null) {
            camelItemsList.firstOrNull { it.id == item.sales_item_id && (item.sales_item_id ?: 0) > 0 }
                ?: camelItemsList.firstOrNull { it.product_id == item.product_id && (item.product_id ?: 0) > 0 && it.distribution_pack_id == item.distribution_pack_id }
        } else null

        // ✅ Fix: Use the actual RETURNED quantity. In read-only mode, if no return_quantity was
        // restored (common for multi-item offline offline sales), fall back to the original
        // purchased quantity so the totals card is not blank.
        val restoredQty = item.return_quantity?.takeIf { it > 0 }
            ?: item.batch_return_quantity?.takeIf { it > 0 }
        val fallbackQty = item.quantity?.takeIf { it > 0 }?.toInt()
            ?: detailedItem?.quantity?.takeIf { it > 0 }?.toInt()
            ?: camelItem?.quantity?.takeIf { it > 0 }?.toInt()
            ?: 0
        val qty = restoredQty ?: fallbackQty

        if (qty > 0) {
            // ✅ Price resolution priority:
            // 1. detailedItem (SalesItemDetailed, snake_case) — most reliable, from API
            // 2. item.retail_price — the batch item itself (synthetic batches carry the correct price from restore)
            // 3. camelItem (SalesItem, camelCase) — may have retail_price=0 due to cache serialization mismatch
            val taxExclusivePrice = detailedItem?.tax_exclusive_price?.takeIf { it > 0 }
                ?: item.tax_exclusive_price?.takeIf { it > 0 }
                ?: camelItem?.tax_exclusive_price?.takeIf { it > 0 }
                ?: 0.0

            val retailPrice = detailedItem?.retail_price?.takeIf { it > 0 }
                ?: item.retail_price?.takeIf { it > 0 }
                ?: camelItem?.retail_price?.takeIf { it > 0 }
                ?: 0.0

            // ✅ Calculate tax from price difference
            val taxPerUnit = retailPrice - taxExclusivePrice

            val itemTotal = qty * taxExclusivePrice  // Subtotal (tax-exclusive)
            val taxAmount = qty * taxPerUnit          // Total tax

            val taxAmountRounded = BigDecimal
                .valueOf(taxAmount)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            val baseGrandTotal = itemTotal + taxAmountRounded

            // Discount calculation
            val totalDiscountForBatch = discountForBatch(item).coerceAtLeast(0.0)
            val actualQtySold = item.quantity?.takeIf { it > 0.0 } ?: detailedItem?.quantity ?: camelItem?.quantity ?: 0.0

            val discountForThisQty: Double = if (totalDiscountForBatch > 0.0 && actualQtySold > 0.0) {
                val perUnit = totalDiscountForBatch / actualQtySold
                round2(perUnit * qty.toDouble())
            } else {
                0.0
            }

            val grandTotalAfterDiscount = (baseGrandTotal - discountForThisQty).coerceAtLeast(0.0)
            val grandTotalRounded = BigDecimal
                .valueOf(grandTotalAfterDiscount)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            // ✅ Round Subtotal to INT for consistency with bottom card
            val itemTotalRounded = BigDecimal
                .valueOf(itemTotal)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            // ✅ Display logic: Show actual tax applied.
            // In the presence of a discount, (GrandTotal - Subtotal) is not just Tax anymore.
            val finalTaxAmount = taxAmountRounded

            // UI updates
            holder.binding.subtotalss.text = NumberFormatter().formatPrice(
                String.format(Locale.US, "%.2f", itemTotalRounded), localizationData
            )
            holder.binding.taxAmountss.text = NumberFormatter().formatPrice(
                String.format(Locale.US, "%.2f", finalTaxAmount), localizationData
            )

            if (discountForThisQty > 0.0) {
                holder.binding.discountLayout.isVisible = true
                holder.binding.discountAmount.text = NumberFormatter().formatPrice(
                    String.format(Locale.US, "%.2f", discountForThisQty),
                    localizationData
                )
            } else {
                holder.binding.discountLayout.isVisible = false
                holder.binding.discountAmount.text = NumberFormatter().formatPrice("0.00", localizationData)
            }

            holder.binding.alltotalAmountss.text = NumberFormatter().formatPrice(
                String.format(Locale.US, "%.2f", grandTotalRounded), localizationData
            )

            // ✅ Real-time calculation logic for Online-made sales in offline mode
            // If tax amount is 0, percentage must stay 0%. Else calculate real-time for online sales.
            val invoiceIdStr = returnitems.firstOrNull()?.invoice_id ?: ""
            // Offline sales usually have "OFF" or are local records
            val isOnlineSaleSync = !invoiceIdStr.contains("OFF", ignoreCase = true) && !invoiceIdStr.startsWith("INV", ignoreCase = true)
            
            val taxPercentValue = if (isOnlineSaleSync) {
                if (taxExclusivePrice > 0.0) {
                    (taxPerUnit / taxExclusivePrice * 100.0)
                } else 0.0
            } else {
                // Prefer stored tax (Integer like 18), fallback to calculated (Double like 17.99)
                val storedTax = (detailedItem?.tax ?: camelItem?.tax ?: 0).toDouble()
                if (storedTax > 0.0) {
                    storedTax
                } else if (taxExclusivePrice > 0.0) {
                    (taxPerUnit / taxExclusivePrice * 100.0)
                } else 0.0
            }

            Log.d("DEBUG_TAX_PERCENT", "--- calculateTotalss Debug ---")
            Log.d("DEBUG_TAX_PERCENT", "Invoice: $invoiceIdStr, OnlineSync: $isOnlineSaleSync")
            Log.d("DEBUG_TAX_PERCENT", "Item ID: ${item.sales_item_id}, Batch: $batchName")
            Log.d("DEBUG_TAX_PERCENT", "DetailedItem Null: ${detailedItem == null}, CamelItem Null: ${camelItem == null}")
            Log.d("DEBUG_TAX_PERCENT", "Retail: $retailPrice, TaxExcl: $taxExclusivePrice, TaxPerUnit: $taxPerUnit")
            Log.d("DEBUG_TAX_PERCENT", "Stored Tax (Detailed): ${detailedItem?.tax}, Stored Tax (Camel): ${camelItem?.tax}")
            Log.d("DEBUG_TAX_PERCENT", "Final Tax Percent: $taxPercentValue")

            holder.binding.taxfields.text = "(+) Tax @${formatPercent(taxPercentValue)}%"

            Log.d(
                "ReturnAdapter",
                "calculateTotalss -> sid=${item.sales_item_id}, batch='$batchName', " +
                        "qty=$qty, taxExcl=$taxExclusivePrice, retail=$retailPrice, " +
                        "taxPerUnit=$taxPerUnit, subtotal=$itemTotal, tax=$taxAmountRounded, " +
                        "discount=$discountForThisQty, grand=$grandTotalRounded"
            )
        } else {
            holder.binding.subtotalss.text = "RWF0.00"
            holder.binding.taxAmountss.text = "RWF0.00"
            holder.binding.alltotalAmountss.text = "RWF0.00"
            holder.binding.discountLayout.isVisible = false
            holder.binding.discountAmount.text = NumberFormatter().formatPrice("0.00", localizationData)
            holder.binding.taxfields.text = "(+) Tax @0%"
        }
    }


    // ---- EDIT totals (calculateTotals) ----
    private fun calculateTotals(holder: StockSearchViewHolder, item: BatchReturnItem) {
        val qty = (item.batch_return_quantity ?: 0).coerceAtLeast(0)

        if (qty > 0) {
            // ✅ Get CORRECT prices from sales_items (detailed list)
            val batchName = batchText(item).trim()
            val detailedItem = returnitems.firstOrNull()?.sales_items?.firstOrNull { si ->
                si.id == item.sales_item_id &&
                        (batchName.isEmpty() || si.batch?.trim()?.equals(batchName, ignoreCase = true) == true)
            } ?: returnitems.firstOrNull()?.sales_items?.firstOrNull { si ->
                si.id == item.sales_item_id  // Fallback: match by ID only
            }

            val taxExclusivePrice = detailedItem?.tax_exclusive_price
                ?: item.tax_exclusive_price
                ?: 0.0

            val retailPrice = detailedItem?.retail_price
                ?: item.retail_price
                ?: 0.0

            // ✅ Calculate tax from price difference
            val taxPerUnit = retailPrice - taxExclusivePrice

            val itemTotal = qty * taxExclusivePrice  // Subtotal
            val taxAmount = qty * taxPerUnit          // Total tax

            val taxAmountRounded = BigDecimal
                .valueOf(taxAmount)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            val baseGrandTotal = itemTotal + taxAmountRounded

            // Discount calculation
            val totalDiscountForBatch = discountForBatch(item).coerceAtLeast(0.0)
            val actualQtySold = item.quantity?.takeIf { it > 0.0 } ?: detailedItem?.quantity ?: 0.0

            val discountForThisQty: Double = if (totalDiscountForBatch > 0.0 && actualQtySold > 0.0) {
                val perUnit = totalDiscountForBatch / actualQtySold
                round2(perUnit * qty.toDouble())
            } else {
                0.0
            }

            val grandTotalAfterDiscount = (baseGrandTotal - discountForThisQty).coerceAtLeast(0.0)
            val grandTotalRounded = BigDecimal
                .valueOf(grandTotalAfterDiscount)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            // ✅ Round Subtotal to INT for consistency
            val itemTotalRounded = BigDecimal
                .valueOf(itemTotal)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            // UI updates
            holder.binding.subtotals.text = NumberFormatter().formatPrice(
                String.format(Locale.US, "%.2f", itemTotalRounded), localizationData
            )
            holder.binding.taxAmounts.text = NumberFormatter().formatPrice(
                String.format(Locale.US, "%.2f", taxAmountRounded), localizationData
            )

            if (discountForThisQty > 0.0) {
                holder.binding.discountLayoutnew.isVisible = true
                holder.binding.discountAmountnew.text = NumberFormatter().formatPrice(
                    String.format(Locale.US, "%.2f", discountForThisQty),
                    localizationData
                )
            } else {
                holder.binding.discountLayoutnew.isVisible = false
                holder.binding.discountAmountnew.text = NumberFormatter().formatPrice("0.00", localizationData)
            }

            holder.binding.alltotalAmounts.text = NumberFormatter().formatPrice(
                String.format(Locale.US, "%.2f", grandTotalRounded), localizationData
            )

            // ✅ Show actual tax percentage
            val actualTaxPercent = if (taxExclusivePrice > 0) {
                (taxPerUnit / taxExclusivePrice * 100.0)
            } else 0.0
            val percentStr = "(+) Tax @${formatPercent(actualTaxPercent)}%"
            holder.binding.taxfield.text = percentStr
            holder.binding.taxfields.text = percentStr

            Log.d(
                "ReturnAdapter",
                "calculateTotals -> sid=${item.sales_item_id}, batch='$batchName', " +
                        "qty=$qty, taxExcl=$taxExclusivePrice, retail=$retailPrice, " +
                        "taxPerUnit=$taxPerUnit, subtotal=$itemTotal, tax=$taxAmountRounded, " +
                        "discount=$discountForThisQty, grand=$grandTotalRounded"
            )
        } else {
            holder.binding.subtotals.text = "RWF0.00"
            holder.binding.taxAmounts.text = "RWF0.00"
            holder.binding.alltotalAmounts.text = "RWF0.00"
            holder.binding.discountLayoutnew.isVisible = false
            holder.binding.discountAmountnew.text = NumberFormatter().formatPrice("0.00", localizationData)
            holder.binding.taxfield.text = "(+) Tax @0%"
            holder.binding.taxfields.text = "(+) Tax @0%"
        }
    }

    private fun updateReceivedQuantity(item: BatchReturnItem) {
        val batchKey = item.batch?.trim()?.lowercase() ?: ""
        val salesItemId = item.sales_item_id

        // Remove old entry for this (sales_item_id, batch)
        matReceivedList.removeAll {
            it.sales_item_id == salesItemId && (it.batch?.trim()?.lowercase() ?: "") == batchKey
        }

        // Prefer return_quantity (what user typed in Qty EditText), fallback to batch_return_quantity
        val qty = (item.return_quantity ?: item.batch_return_quantity ?: 0).coerceAtLeast(0)

        // Add only if qty > 0
        if (qty > 0) {
            val unitPrice = unitPriceRounded(item)
            matReceivedList.add(
                item.copy(
                    batch_return_quantity = qty,                 // for totals
                    return_quantity = qty,                      // qty that goes to API
                    defective_boxes = item.defective_boxes,     // ✅ total boxes for this batch
                    defective_bottles = item.defective_bottles, // ✅ total packs for this batch
                    batch_refund_amount = qty * unitPrice
                )
            )
        }

        // Validity is driven by qty that will go to API
        val validItems = matReceivedList.filter { (it.return_quantity ?: 0) > 0 }






        if (validItems.isEmpty()) {
            Toast.makeText(context, "No items selected for return", Toast.LENGTH_SHORT).show()
            onBatchQuantityChange(validItems) // still push empty to clear upstream
            return
        }

        onBatchQuantityChange(validItems)
    }


    fun getValidReturnBatches(): List<BatchReturnItem> =
        matReceivedList.filter { (it.batch_return_quantity ?: 0) > 0 }

    // -------------- percent utils --------------
    private fun formatTaxForDisplay(raw: Any?): String {
        val s0 = raw?.toString()?.trim().orEmpty()
        if (s0.isEmpty()) return "0"
        val s1 = s0.replace(Regex("[^0-9.,]"), "").replace(',', '.')
        if (s1.isEmpty() || s1 == ".") return "0"

        fun BigDecimal.pretty(): String {
            // ✅ Round to nearest integer
            val rounded = this.setScale(0, RoundingMode.HALF_UP)
            return rounded.toPlainString()
        }

        return try {
            val value: BigDecimal = if (s1.contains('.')) {
                val d = BigDecimal(s1)
                if (d <= BigDecimal.ONE) d.multiply(BigDecimal(100)) else d
            } else {
                val n = s1.toLong()
                when {
                    n <= 100 -> BigDecimal(n)
                    n <= 1000 -> BigDecimal(n).divide(BigDecimal.TEN)
                    else -> BigDecimal(n).divide(BigDecimal(100))
                }
            }
            value.pretty()  // Returns rounded integer like "18" instead of "18.02"
        } catch (_: Exception) {
            s1.trimEnd('.')
        }
    }


    private fun parseTaxPercent(raw: Any?): Double {
        val s0 = raw?.toString()?.trim().orEmpty()
        if (s0.isEmpty()) return 0.0
        val cleaned = s0.replace(Regex("[^0-9.,-]"), "").replace(',', '.')
        if (cleaned.isEmpty() || cleaned == "." || cleaned == "-") return 0.0
        val value = cleaned.toDoubleOrNull() ?: return 0.0
        if (!s0.contains('.') && !s0.contains(',') && value >= 100) {
            val by10 = value / 10.0
            return if (by10 <= 100) by10 else value / 100.0
        }
        return value
    }

    private fun formatPercent(p: Double): String {
        // ✅ Round to nearest integer
        return kotlin.math.round(p).toInt().toString()
    }
}