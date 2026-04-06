package com.retailone.pos.adapter

import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Locale
import NumberFormatter
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.retailone.pos.databinding.ReplaceItemBatchLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.ReturnSalesItemModel.BatchReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData
import org.json.JSONObject
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private fun round2(v: Double?): Double =
    BigDecimal.valueOf(v ?: 0.0).setScale(2, RoundingMode.HALF_UP).toDouble()

class ReplaceSalesItemBatchAdapter(
    private val returnitems: List<ReturnItemData>,
    val context: Context,
    val returnbatchlist: List<BatchReturnItem>,
    val readonlyMode: Boolean = false,
    private val parentEnabled: Boolean = false,
    val onBatchQuantityChange: (List<BatchReturnItem>) -> Unit
) : RecyclerView.Adapter<ReplaceSalesItemBatchAdapter.StockSearchViewHolder>() {

    private val sharedPrefHelper = SharedPrefHelper(context)
    val localizationData = LocalizationHelper(context).getLocalizationData()
    private val filteredList = returnbatchlist.filter { (it.quantity ?: 0.0) > 0.0 }

    private val matReceivedList = mutableListOf<BatchReturnItem>().apply { clear() }

    private data class RowState(
        var checked: Boolean = false, var boxCount: Int = 0, var bottleCount: Int = 0
    )

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

    private fun keyFor(item: BatchReturnItem): String =
        "${item.sales_item_id}_${batchText(item).trim().lowercase()}"

    private val rowState = mutableMapOf<String, RowState>()

    class StockSearchViewHolder(val binding: ReplaceItemBatchLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var suppressBoxWatcher = false
        var suppressButtolsWatcher = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSearchViewHolder {
        return StockSearchViewHolder(
            ReplaceItemBatchLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    private fun unitPriceRounded(batch: BatchReturnItem): Double {
        var v: Double? = batch.tax_exclusive_price
        if (v == null) {
            val detailed = returnitems.firstOrNull()?.sales_items
            v = detailed?.firstOrNull { it.id == batch.sales_item_id }?.tax_exclusive_price
        }
        val out =
            BigDecimal.valueOf(v ?: (batch.retail_price ?: 0.0)).setScale(2, RoundingMode.HALF_UP)
                .toDouble()
        Log.d("ReplaceAdapter", "unitPriceRounded sales_item_id=${batch.sales_item_id} -> $out")
        return out
    }

    private fun basePacksPerBox(item: BatchReturnItem): Int {
        val byIdAndBatch =
            returnitems.asSequence().mapNotNull { it.sales_items }.flatten().firstOrNull { si ->
                si.id == item.sales_item_id && (si.batch?.trim()
                    ?.equals(batchText(item).trim(), ignoreCase = true) ?: false)
            }?.distribution_pack?.no_of_packs

        if (byIdAndBatch != null) return byIdAndBatch

        return returnitems.asSequence().mapNotNull { it.sales_items }.flatten()
            .firstOrNull { si -> si.id == item.sales_item_id }?.distribution_pack?.no_of_packs ?: 1
    }

    private fun isAlreadyReturned(item: BatchReturnItem): Boolean {
        val bottles = item.return_quantity ?: 0
        val boxes = item.batch_return_quantity
        return bottles > 0 || (boxes ?: 0) > 0
    }

    /**
     * Get discount amount for this particular batch from the detailed list (sales_items).
     * Matches by sales_item_id + batch name; falls back to sales_item_id only.
     */
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

        // Fallback: match only by sales_item_id
        if (discount <= 0.0) {
            val byId = detailedSales.firstOrNull { it.id == item.sales_item_id }
            discount = byId?.discount ?: 0.0
        }

        Log.d(
            "ReplaceAdapter",
            "discountForBatch -> sales_item_id=${item.sales_item_id}, batch='$batchName', discount=$discount"
        )

        return discount.coerceAtLeast(0.0)
    }

    override fun onBindViewHolder(holder: StockSearchViewHolder, position: Int) {
        val item = filteredList[position]
        val key = keyFor(item)
        val state = rowState.getOrPut(key) { RowState() }

        val formattedPrice =
            NumberFormatter().formatPrice((item.retail_price ?: 0.0).toString(), localizationData)
        val purchasedQty = ceil(item.quantity ?: 0.0).toInt()
        val packsPerBox = max(1, basePacksPerBox(item))
        val alreadyReturned = isAlreadyReturned(item)

        holder.binding.batchNames.text = batchText(item)
        holder.binding.batchPrices.text = formattedPrice
        holder.binding.saleQuantitys.text = item.quantity.toString()

        val cb = holder.binding.cbPleaseCheckIt
        val inputsContainer = holder.binding.boxBottleInputs
        val etBox = holder.binding.etNoOfBox
        val etButtols = holder.binding.etNumberOfButtols

        cb.setOnCheckedChangeListener(null)

        if (alreadyReturned) {
            state.checked = false; state.boxCount = 0; state.bottleCount = 0
            cb.isEnabled = false; cb.isChecked = false
            inputsContainer.visibility = View.GONE
            etBox.isEnabled = false; etButtols.isEnabled = false
            if (etBox.text?.isNotEmpty() == true) {
                holder.suppressBoxWatcher = true; etBox.setText(""); holder.suppressBoxWatcher =
                    false
            }
            if (etButtols.text?.isNotEmpty() == true) {
                holder.suppressButtolsWatcher =
                    true; etButtols.setText(""); holder.suppressButtolsWatcher = false
            }
            calculateTotals(holder, item); return
        }

        cb.isEnabled = !readonlyMode && parentEnabled
        cb.alpha = if (cb.isEnabled) 1f else 0.6f
        cb.isChecked = state.checked

        inputsContainer.visibility = if (state.checked) View.VISIBLE else View.GONE
        etBox.isEnabled = state.checked && !readonlyMode
        etButtols.isEnabled = state.checked && !readonlyMode

        if (state.checked) {
            if (state.boxCount <= 0) state.boxCount = purchasedQty
            state.boxCount = min(state.boxCount, purchasedQty)
            if (state.bottleCount <= 0) state.bottleCount = state.boxCount * packsPerBox
        } else {
            state.boxCount = 0; state.bottleCount = 0
        }

        val boxText = state.boxCount.takeIf { it > 0 }?.toString() ?: ""
        if (etBox.text?.toString() != boxText) {
            holder.suppressBoxWatcher = true; etBox.setText(boxText); etBox.setSelection(
                etBox.text?.length ?: 0
            ); holder.suppressBoxWatcher = false
        }
        val buttolsText = state.bottleCount.takeIf { it > 0 }?.toString() ?: ""
        if (etButtols.text?.toString() != buttolsText) {
            holder.suppressButtolsWatcher =
                true; etButtols.setText(buttolsText); etButtols.setSelection(
                etButtols.text?.length ?: 0
            ); holder.suppressButtolsWatcher = false
        }

        calculateTotals(holder, item)

        cb.setOnCheckedChangeListener { _, checked ->
            state.checked = checked
            if (checked) {
                inputsContainer.visibility = View.VISIBLE
                etBox.isEnabled = !readonlyMode
                etButtols.isEnabled = !readonlyMode

                state.boxCount = purchasedQty
                state.bottleCount = state.boxCount * packsPerBox

                holder.suppressBoxWatcher =
                    true; etBox.setText(state.boxCount.toString()); etBox.setSelection(
                    etBox.text?.length ?: 0
                ); holder.suppressBoxWatcher = false
                holder.suppressButtolsWatcher =
                    true; etButtols.setText(state.bottleCount.toString()); etButtols.setSelection(
                    etButtols.text?.length ?: 0
                ); holder.suppressButtolsWatcher = false

                etBox.error = null; etButtols.error = null

                updateReceivedQuantity(item, state.boxCount)
                calculateTotals(holder, item)
            } else {
                inputsContainer.visibility = View.GONE
                etBox.isEnabled = false; etButtols.isEnabled = false
                state.boxCount = 0; state.bottleCount = 0
                holder.suppressBoxWatcher = true; etBox.text?.clear(); holder.suppressBoxWatcher =
                    false
                holder.suppressButtolsWatcher =
                    true; etButtols.text?.clear(); holder.suppressButtolsWatcher = false
                etBox.error = null; etButtols.error = null
                updateReceivedQuantity(item, 0)
                calculateTotals(holder, item)
            }
        }

        etBox.doOnTextChanged { text, _, _, _ ->
            if (holder.suppressBoxWatcher) return@doOnTextChanged
            if (!state.checked) return@doOnTextChanged

            val raw = text?.toString()?.trim() ?: ""

            // 1) Let the user clear the field while editing
            if (raw.isEmpty()) {
                etBox.error = "Required"
                state.boxCount = 0
                // keep bottles in sync with empty input
                val zeroBottles = 0
                if ((etButtols.text?.toString() ?: "") != zeroBottles.toString()) {
                    holder.suppressButtolsWatcher = true
                    etButtols.setText(zeroBottles.toString())
                    etButtols.setSelection(etButtols.text?.length ?: 0)
                    holder.suppressButtolsWatcher = false
                }
                state.bottleCount = zeroBottles
                updateReceivedQuantity(item, state.boxCount)
                calculateTotals(holder, item)
                return@doOnTextChanged
            }

            // 2) Parse number
            val value = raw.toIntOrNull()
            if (value == null) {
                etBox.error = "Invalid number"
                return@doOnTextChanged
            }

            // 3) Block exactly "0" but don't force any other value; clear it so user can type
            if (value == 0) {
                etBox.error = "You are not allowed to put 0"
                holder.suppressBoxWatcher = true
                etBox.setText("") // clear so they can immediately enter a valid number
                holder.suppressBoxWatcher = false
                return@doOnTextChanged
            }

            // 4) Normal bounds
            val bounded = when {
                value < 0 -> {
                    etBox.error = "Cannot be negative"; 1
                }
                value > purchasedQty -> {
                    etBox.error = "Cannot exceed purchased ($purchasedQty)"; purchasedQty
                }
                else -> {
                    etBox.error = null; value
                }
            }

            if (bounded.toString() != raw) {
                holder.suppressBoxWatcher = true
                etBox.setText(bounded.toString())
                etBox.setSelection(etBox.text?.length ?: 0)
                holder.suppressBoxWatcher = false
            }

            // 5) Sync state + bottles
            state.boxCount = bounded
            val newBottles = (state.boxCount * packsPerBox).coerceAtLeast(0)
            if (newBottles.toString() != (etButtols.text?.toString() ?: "")) {
                holder.suppressButtolsWatcher = true
                etButtols.setText(newBottles.toString())
                etButtols.setSelection(etButtols.text?.length ?: 0)
                holder.suppressButtolsWatcher = false
            }
            state.bottleCount = newBottles

            updateReceivedQuantity(item, state.boxCount)
            calculateTotals(holder, item)
        }

        etButtols.doOnTextChanged { text, _, _, _ ->
            if (holder.suppressButtolsWatcher) return@doOnTextChanged
            if (!state.checked) return@doOnTextChanged

            val raw = text?.toString()?.trim()
            val value = raw?.toIntOrNull() ?: 0

            val maxBottlesOverall = purchasedQty * packsPerBox
            val bounded = when {
                value < 0 -> 0
                value > maxBottlesOverall -> {
                    etButtols.error = "Cannot exceed max ($maxBottlesOverall)"; maxBottlesOverall
                }

                else -> {
                    etButtols.error = null; value
                }
            }

            if (bounded.toString() != raw) {
                holder.suppressButtolsWatcher = true
                etButtols.setText(bounded.toString())
                etButtols.setSelection(etButtols.text?.length ?: 0)
                holder.suppressButtolsWatcher = false
            }

            state.bottleCount = bounded

            updateReceivedQuantity(item, state.boxCount)
            calculateTotals(holder, item)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    // ✅ FIXED: Calculate tax from price difference (retail_price - tax_exclusive_price)
    private fun calculateTotals(holder: StockSearchViewHolder, item: BatchReturnItem) {
        val qtyPurchased = ceil(item.quantity ?: 0.0).toInt().coerceAtLeast(0)

        if (qtyPurchased > 0) {
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

            // Tax per unit = retail_price - tax_exclusive_price
            val taxPerUnit = retailPrice - taxExclusivePrice

            // Calculate subtotal (tax-exclusive)
            val rawSubtotal = qtyPurchased * taxExclusivePrice

            // Gross subtotal (excl. tax, before discount), rounded to 0 decimals
            val grossSubtotal = BigDecimal
                .valueOf(rawSubtotal)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            // Discount for this batch
            val discountForThisBatch = discountForBatch(item).coerceAtLeast(0.0)

            // Show / hide discount row
            if (discountForThisBatch > 0.0) {
                holder.binding.discountRow.visibility = View.VISIBLE
                holder.binding.discountamount.text = NumberFormatter().formatPrice(
                    String.format(Locale.US, "%.2f", discountForThisBatch),
                    localizationData
                )
            } else {
                holder.binding.discountRow.visibility = View.GONE
                holder.binding.discountamount.text = "RWF 0.00"
            }

            // Net subtotal after discount, still tax-exclusive
            val netSubtotal = (grossSubtotal - discountForThisBatch).coerceAtLeast(0.0)

            // ✅ Tax is calculated from unit price difference
            val taxAmount = qtyPurchased * taxPerUnit

            val taxAmountRounded = BigDecimal
                .valueOf(taxAmount)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            // Grand total: netSubtotal + tax
            val grandTotal = netSubtotal + taxAmountRounded

            val grandTotalRounded = BigDecimal
                .valueOf(grandTotal)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            // Logs for verification
            Log.d(
                "ReplaceAdapter",
                "calcTotals sid=${item.sales_item_id} batch='$batchName' qty=$qtyPurchased " +
                        "taxExclPrice=$taxExclusivePrice retailPrice=$retailPrice taxPerUnit=$taxPerUnit " +
                        "rawSub=$rawSubtotal grossSub=$grossSubtotal disc=$discountForThisBatch " +
                        "tax=$taxAmountRounded netSub=$netSubtotal grand=$grandTotalRounded"
            )

            // Bind to UI
            holder.binding.subtotalss.text = NumberFormatter().formatPrice(
                String.format(Locale.US, "%.2f", grossSubtotal), localizationData
            )

            holder.binding.taxAmountss.text = NumberFormatter().formatPrice(
                String.format(Locale.US, "%.2f", taxAmountRounded), localizationData
            )

            holder.binding.alltotalAmountss.text = NumberFormatter().formatPrice(
                String.format(Locale.US, "%.2f", grandTotalRounded), localizationData
            )

            // ✅ Show actual tax percentage based on this item
            val actualTaxPercent = if (taxExclusivePrice > 0) {
                (taxPerUnit / taxExclusivePrice * 100.0)
            } else 0.0
            holder.binding.taxfields.text = "(+) Tax @${formatPercent(actualTaxPercent)}%"

        } else {
            holder.binding.subtotalss.text = "RWF0.00"
            holder.binding.taxAmountss.text = "RWF0.00"
            holder.binding.alltotalAmountss.text = "RWF0.00"
            holder.binding.discountRow.visibility = View.GONE
            holder.binding.discountamount.text = "RWF 0.00"
            holder.binding.taxfields.text = "(+) Tax @0%"
        }
    }


    private fun updateReceivedQuantity(item: BatchReturnItem, value: Int) {
        val batchKey = batchText(item).trim().lowercase()
        val salesItemId = item.sales_item_id
        val bottles = rowState[keyFor(item)]?.bottleCount ?: 0

        matReceivedList.removeAll {
            batchText(it).trim().lowercase() == batchKey && it.sales_item_id == salesItemId
        }

        if (value > 0 || bottles > 0) {
            val unitPrice = unitPriceRounded(item)
            matReceivedList.add(
                item.copy(
                    batch_return_quantity = value,
                    return_quantity = bottles,
                    batch_refund_amount = value * unitPrice
                )
            )
        }

        val validItems = matReceivedList.filter {
            ((it.batch_return_quantity ?: 0) > 0) || ((it.return_quantity ?: 0) > 0)
        }
        Log.d(
            "ReplaceAdapter",
            "validItems: ${validItems.map { batchText(it) to (it.batch_return_quantity to (it.return_quantity ?: 0)) }}"
        )
        onBatchQuantityChange(validItems)
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
    }}
