package com.retailone.pos.adapter

import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.databinding.ItemBatchRowBinding
import com.retailone.pos.models.GoodsToWarehouseModel.ReturnStocks.ReturnBatchItem
import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BatchListAdapter(
    private val returnItems: List<ReturnBatchItem>,
    private val reasonNames: List<String>
) : RecyclerView.Adapter<BatchListAdapter.BatchViewHolder>() {

    private val selectedItems = mutableListOf<StockReturnItem>()

    inner class BatchViewHolder(val binding: ItemBatchRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchViewHolder {
        val binding = ItemBatchRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BatchViewHolder, position: Int) {
        val item = returnItems[position]
        val fromGoodReturned = item.fromGoodReturnedMap == true
        val context = holder.itemView.context
        holder.binding.tvStatus.isSelected = true

        with(holder.binding) {

            val normalizedCond = item.condition.trim().lowercase(Locale.getDefault())
            val condPlain = normalizedCond.replace(" ", "").replace("_", "")

            Log.d(
                "BatchList",
                "onBind pos=$position condition='${item.condition}' condPlain=$condPlain fromGoodReturned=$fromGoodReturned"
            )

            val isGoodFromReturnedMap =
                item.condition.equals("Good", ignoreCase = true)

            // 🔹 Available qty
            tvAvailableQty.text = item.returnedQty.toString()

            tvStock.text = item.stockqqty.toString()
            tvBatchNos.text = item.batchNo

            // 🔹 Status text: first letter capital (Expired, Store stock, Others…)
            val displayCondition = normalizedCond.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
            tvStatus.text = displayCondition

            // 🔹 Total bottles label
            tvBottleQty.text = item.totalBottles.toString()

            // reset editable fields to avoid recycle issues
            etEnterQty.setText("")
            etBottleQty.setText("")
            etRemarks.setText("")
            llExtraFields.visibility = View.GONE
            spReason.visibility = View.GONE
            etRemarks.visibility = View.GONE

            // 🔹 Status color
            val statusColor = when (condPlain) {
                "good" -> ContextCompat.getColor(context, R.color.green)
                "expired" -> ContextCompat.getColor(context, R.color.red)
                "defective", "damaged" -> ContextCompat.getColor(context, R.color.orange)
                else -> ContextCompat.getColor(context, R.color.grey)
            }

            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 30f
                setColor(statusColor)
            }

            tvStatus.background = bgDrawable
            tvStatus.setTextColor(ContextCompat.getColor(context, R.color.white))

            val reasonAdapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_item, reasonNames)
            reasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spReason.adapter = reasonAdapter

            // 🔒 Non-editable rows (you had disabling commented → keep as-is)
            if (!item.isEditable) {
                // keep previous behaviour if you add disabled state
            } else {

                // Pre-calc condition flags
                val isStoreStock = condPlain == "storestock"
                val isExcessive = condPlain == "excessivestock"
                val isOthers = condPlain == "others"
                val isExtra = condPlain == "extrastock" || condPlain == "extra"
                val isGood = condPlain == "good"
                val isExpired = condPlain == "expired"
                val isDefective = condPlain == "defective"
                val isNoSell = condPlain == "nosell"

                // ✅ Only these will use remarks-only UI
                val remarkOnlyStatus = isStoreStock || isExcessive || isOthers || isExtra

                // ✅ 1. UNIT QTY change
                etEnterQty.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: android.text.Editable?) {
                        val enteredQty = etEnterQty.text.toString().toIntOrNull() ?: 0
                        val availableQty = item.returnedQty

                        Log.d(
                            "BatchList",
                            "qtyChanged pos=$position cond=${item.condition} qty=$enteredQty avail=$availableQty"
                        )


                         // refresh bottle quantity in case any changes in QTY
                           etBottleQty.setText("")
                           //etBottleQty.setSelection(etBottleQty.text.length)

                           val unitQty = etEnterQty.text.toString().toIntOrNull() ?: 0
                           saveOrUpdateSelectedItem(
                               item,
                               unitQty,
                               0,
                               etRemarks.text.toString(),
                               spReason.selectedItem?.toString() ?: ""
                           )




                        if (enteredQty > availableQty) {
                            etEnterQty.setText("")
                            etEnterQty.setSelection(etEnterQty.text.length)
                            llExtraFields.visibility = View.GONE
                            spReason.visibility = View.GONE
                            etRemarks.visibility = View.GONE
                            etRemarks.setText("")
                            android.widget.Toast.makeText(
                                context,
                                "Entered quantity cannot be more than available ($availableQty)",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()

                            val currentBottle =
                                etBottleQty.text.toString().toIntOrNull() ?: 0
                            saveOrUpdateSelectedItem(
                                item,
                                0,
                                currentBottle,
                                etRemarks.text.toString(),
                                spReason.selectedItem?.toString() ?: ""
                            )
                            return
                        }

                        // ===== VISIBILITY rules (fixed) =====
                        when {
                            enteredQty <= 0 -> {
                                // Nothing selected → no extras
                                llExtraFields.visibility = View.GONE
                                spReason.visibility = View.GONE
                                etRemarks.visibility = View.GONE
                                etRemarks.setText("")
                            }

                            isGood -> {
                                // Good → show spinner (reason). Remarks controlled by spinner.
                                llExtraFields.visibility = View.VISIBLE
                                spReason.visibility = View.VISIBLE
                                etRemarks.visibility = View.GONE
                                etRemarks.isEnabled = false
                                etRemarks.alpha = 0.5f
                                etRemarks.setText("")
                            }

                            remarkOnlyStatus -> {
                                // Store Stock / Excessive / Others / Extra → only remarks
                                llExtraFields.visibility = View.VISIBLE
                                spReason.visibility = View.GONE
                                etRemarks.visibility = View.VISIBLE
                                etRemarks.isEnabled = true
                                etRemarks.alpha = 1.0f
                            }

                            // ❌ Expired / Defective / No Sell / others → NO reason, NO remarks
                            isExpired || isDefective || isNoSell -> {
                                llExtraFields.visibility = View.GONE
                                spReason.visibility = View.GONE
                                etRemarks.visibility = View.GONE
                                etRemarks.setText("")
                            }

                            else -> {
                                // Any other condition → no extra fields
                                llExtraFields.visibility = View.GONE
                                spReason.visibility = View.GONE
                                etRemarks.visibility = View.GONE
                                etRemarks.setText("")
                            }
                        }

                        val currentBottle = etBottleQty.text.toString().toIntOrNull() ?: 0
                        saveOrUpdateSelectedItem(
                            item,
                            enteredQty,
                            currentBottle,
                            etRemarks.text.toString(),
                            spReason.selectedItem?.toString() ?: ""
                        )
                    }
                })

                // ✅ 2. BOTTLE QTY change + limit check
                etBottleQty.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: android.text.Editable?) {
                        val maxBottles = item.totalBottles
                        val enteredBtl = etBottleQty.text.toString().trim().toIntOrNull() ?: 0

                        Log.d(
                            "BatchList",
                            "bottleChanged pos=$position cond=${item.condition} entered=$enteredBtl max=$maxBottles"
                        )

                        val  qt=  etEnterQty.text.toString().toIntOrNull() ?: 0
                        val  maxBottleThatCanEntered=  qt*(item.noOfPacks)

                        // Bottle qty cannot exceed total bottles
                        //  if (maxBottles >= 0 && enteredBtl > maxBottles) {

                        if ((qt >= 0 && enteredBtl > maxBottleThatCanEntered)||enteredBtl > maxBottles) {

                            Log.d(
                                "BatchList",
                                "bottleChanged pos=$position -> INVALID (entered=$enteredBtl > max=$maxBottles). Resetting to 0"
                            )

                            etBottleQty.setText("")
                            etBottleQty.setSelection(etBottleQty.text.length)

                            if(enteredBtl > maxBottleThatCanEntered){
                                android.widget.Toast.makeText(
                                    context,
                                    "Bottle qty cannot be more than $maxBottleThatCanEntered",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            else if(enteredBtl > maxBottles){
                                android.widget.Toast.makeText(
                                    context,
                                    "Bottle qty cannot be more than $maxBottles",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }

                            /*android.widget.Toast.makeText(
                                context,
                                "Bottle qty cannot be more than $maxBottleThatCanEntered",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()*/

                            val unitQty = etEnterQty.text.toString().toIntOrNull() ?: 0
                            saveOrUpdateSelectedItem(
                                item,
                                unitQty,
                                0,
                                etRemarks.text.toString(),
                                spReason.selectedItem?.toString() ?: ""
                            )
                            return
                        }

                        val unitQty = etEnterQty.text.toString().toIntOrNull() ?: 0
                        saveOrUpdateSelectedItem(
                            item,
                            unitQty,
                            enteredBtl,
                            etRemarks.text.toString(),
                            spReason.selectedItem?.toString() ?: ""
                        )
                    }
                })

                // ✅ 3. Remarks change
                etRemarks.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: android.text.Editable?) {
                        val qty = etEnterQty.text.toString().toIntOrNull() ?: 0
                        val bottleQty =
                            etBottleQty.text.toString().toIntOrNull() ?: 0
                        saveOrUpdateSelectedItem(
                            item,
                            qty,
                            bottleQty,
                            s.toString(),
                            spReason.selectedItem?.toString() ?: ""
                        )
                    }
                })

                // ✅ 4. Reason spinner (for Good only)
                spReason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        pos: Int,
                        id: Long
                    ) {
                        val selectedReason =
                            parent.getItemAtPosition(pos)?.toString()?.trim() ?: ""
                        val enteredQty = etEnterQty.text.toString().toIntOrNull() ?: 0
                        val bottleQty =
                            etBottleQty.text.toString().toIntOrNull() ?: 0

                        // Remarks only for GOOD + qty > 0 (if your business rule needs it)
                        val shouldShowRemarks =
                            enteredQty > 0 && condPlain == "good"

                        etRemarks.visibility =
                            if (shouldShowRemarks) View.VISIBLE else View.GONE
                        etRemarks.isEnabled = shouldShowRemarks
                        etRemarks.alpha = if (shouldShowRemarks) 1.0f else 0.5f
                        if (!shouldShowRemarks) etRemarks.setText("")

                        saveOrUpdateSelectedItem(
                            item,
                            enteredQty,
                            bottleQty,
                            etRemarks.text.toString(),
                            selectedReason
                        )
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }
        }
    }

    override fun getItemCount(): Int = returnItems.size

    fun getSelectedItems(): List<StockReturnItem> = selectedItems

    private fun isExpired(expiryDateStr: String?): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiryDate = sdf.parse(expiryDateStr ?: return false)
            val today = Calendar.getInstance().time
            expiryDate.before(today)
        } catch (e: Exception) {
            false
        }
    }

    // 🔹 stores bottleQty with a **hard cap** at totalBottles
    private fun saveOrUpdateSelectedItem(
        item: ReturnBatchItem,
        qty: Int,
        bottleQty: Int,
        remarks: String,
        reason: String
    ) {
        val normalizedCond = item.condition.trim().lowercase(Locale.getDefault())
        val condPlain = normalizedCond.replace(" ", "").replace("_", "")

        val isGood = condPlain == "good"
        val isStoreStock = condPlain == "storestock"
        val isExcessive = condPlain == "excessivestock"
        val isOthers = condPlain == "others"
        val isExtra = condPlain == "extrastock" || condPlain == "extra"

        // ✅ only these need user remark
        val isRemarkOnlyStatus = isStoreStock || isExcessive || isOthers || isExtra
        val requireRemarks = isGood || isRemarkOnlyStatus

        val fromGoodReturned = item.fromGoodReturnedMap == true

        val trimmedRemarks = remarks.trim()
        val finalRemarks = if (requireRemarks) trimmedRemarks else "NA"

        // 🔒 Extra safety: bottle qty can never exceed totalBottles
        val maxBottles = item.totalBottles
        val safeBottleQty =
            if (maxBottles > 0 && bottleQty > maxBottles) maxBottles else bottleQty

        selectedItems.removeAll {
            it.batch_no == item.batchNo &&
                    it.product_id == item.productId &&
                    it.condition.equals(normalizedCond, true)
        }

        if (qty > 0 || safeBottleQty > 0) {
            Log.d(
                "BatchList",
                "saveItem batch=${item.batchNo} cond=$normalizedCond qty=$qty bottle=$safeBottleQty remarks='$finalRemarks' requireRemarks=$requireRemarks maxBtl=$maxBottles"
            )

            selectedItems.add(
                StockReturnItem(
                    product_id = item.productId,
                    batch_no = item.batchNo,
                    quantity = qty,
                    bottle_quantity = safeBottleQty,
                    condition = normalizedCond,
                    remarks = finalRemarks,
                    fromGoodReturnedMap = fromGoodReturned
                )
            )
        }
    }
}
