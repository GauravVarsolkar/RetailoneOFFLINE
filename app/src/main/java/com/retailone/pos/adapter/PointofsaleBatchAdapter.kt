package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.retailone.pos.databinding.PointofsaleBatchLayoutBinding
import com.retailone.pos.interfaces.OnDeleteItemClickListener
import com.retailone.pos.interfaces.OnQuantityChangeListener
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.utils.FunUtils

class PointofsaleBatchAdapter(
    val context: Context,
    private val posItemList: List<StoreProData>,
    @Suppress("unused") private val onDeleteItemClickListener: OnDeleteItemClickListener,
    @Suppress("unused") private val onQuantityChangeListener: OnQuantityChangeListener,
    private val batchList: List<PosSaleBatch>,
    private val parentposition: Int,
    private val onBatchQuantityChange: (List<PosSaleBatch>) -> Unit,
) : RecyclerView.Adapter<PointofsaleBatchAdapter.POSItemViewHolder>() {

    private val localizationData = LocalizationHelper(context).getLocalizationData()

    // UI list (hide zero-stock rows only so UI doesn’t look empty)
    private val filtered = batchList.filter { FunUtils.DtoString(it.quantity) != "0" }
    private val displayList = if (filtered.isEmpty()) batchList else filtered

    // Working map of batch_no -> entered qty (Double)
    // Initialize with existing cart quantities so we don’t lose state.
    private val enteredQty = batchList.associate { it.batch_no to (it.batch_cart_quantity) }.toMutableMap()





    class POSItemViewHolder(val binding: PointofsaleBatchLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POSItemViewHolder {
        return POSItemViewHolder(
            PointofsaleBatchLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: POSItemViewHolder, position: Int) {

        /*val gson = Gson()
        val json = gson.toJson(batchList)
        Log.d("XXXW",json)*/

        val row = displayList[position]
        val parentItem = posItemList[parentposition]
        val isLooseOil = FunUtils.isLooseOil(parentItem.category_id, parentItem.pack_product_description)

        val priceStr   = NumberFormatter().formatPrice(row.price.toString() ?: "-", localizationData)
        val stockStr   = FunUtils.DtoString(row.quantity)
        val stockLimit = FunUtils.stringToDouble(stockStr)

        val et = holder.binding.saleQuantity

        // Remove any previous watcher
        (et.tag as? TextWatcher)?.let { et.removeTextChangedListener(it) }

        holder.binding.batchName.text  = row.batch_no
        holder.binding.batchPrice.text = priceStr
        holder.binding.batchStock.text = stockStr

        // Input rules
        if (isLooseOil) {
            // loose oil: handled elsewhere; keep read-only to avoid cursor fights
            et.inputType = InputType.TYPE_NULL
            et.isFocusable = false
            et.isClickable = false
            et.isCursorVisible = false
        } else {
            et.inputType = InputType.TYPE_CLASS_NUMBER
            et.isFocusable = true
            et.isClickable = true
            et.isCursorVisible = true
        }

        // Prefill (don’t move cursor if user is typing)
        val currentQty = enteredQty[row.batch_no] ?: row.batch_cart_quantity

       /* Log.d("XXX Z",row.batch_no.toString())
        Log.d("XXX X",enteredQty[row.batch_no].toString())
        Log.d("XXX Y",row.batch_cart_quantity.toString())*/

        val textToShow = if (currentQty == 0.0) "" else FunUtils.DtoString(currentQty)
        if (!et.hasFocus()) {
            val now = et.text?.toString() ?: ""
            if (now != textToShow) {
                et.setText(textToShow)
                et.setSelection(et.text?.length ?: 0)
            }
        }

        // Disable row if out of stock
        val enabled = stockStr != "0"
        et.isEnabled = enabled
        holder.binding.root.alpha = if (enabled) 1f else 0.5f

        // Attach safe watcher (no cursor jump)
        val watcher = object : TextWatcher {
            var suppress = false
            var beforeVal = currentQty
            var beforeSel = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (suppress) return
                beforeVal = FunUtils.stringToDouble(s?.toString() ?: "")
                beforeSel = et.selectionStart.coerceAtLeast(0)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (suppress) return

                val raw = s?.toString() ?: ""
                val entered = if (raw.isEmpty()) 0.0 else FunUtils.stringToDouble(raw)

                // ✅ NEW: Check available stock (consider what's already in other batches)
                val availableStock = row.quantity

                if (entered <= availableStock) {
                    // Update working map
                    enteredQty[row.batch_no] = entered
                    // Send FULL list back (no .copy(), no dropping zero rows)
                    onBatchQuantityChange(buildFullListWithEntered())
                } else {
                    // ✅ UPDATED: Better error message showing available stock
                    Toast.makeText(
                        context,
                        "Cannot exceed available stock (${FunUtils.DtoString(availableStock)} available)",
                        Toast.LENGTH_SHORT
                    ).show()
                    suppress = true
                    val revertText = if (beforeVal == 0.0) "" else FunUtils.DtoString(beforeVal)
                    et.setText(revertText)
                    val newLen = et.text?.length ?: 0
                    et.setSelection(beforeSel.coerceAtMost(newLen))
                    suppress = false
                }
            }


            override fun afterTextChanged(s: Editable?) {}
        }

        et.addTextChangedListener(watcher)
        et.tag = watcher
    }

    /**
     * Build the FULL batch list to send upstream, using the minimal constructor you
     * already use elsewhere:
     *   PosSaleBatch(batch_no, quantity, price, batch_cart_quantity)
     *
     * This avoids calling data-class copy() and touching any new non-null fields.
     */
    private fun buildFullListWithEntered(): List<PosSaleBatch> {
        return batchList.map { src ->
            val q = enteredQty[src.batch_no] ?: src.batch_cart_quantity
            PosSaleBatch(src.batch_no, src.quantity, src.price, tax = src.tax, batch_cart_quantity = q)
        }
    }

    override fun getItemCount(): Int = displayList.size
    override fun getItemViewType(position: Int) = 0
    override fun getItemId(position: Int) = position.toLong()
}

/*
package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.PointofsaleBatchLayoutBinding
import com.retailone.pos.interfaces.OnDeleteItemClickListener
import com.retailone.pos.interfaces.OnQuantityChangeListener
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.utils.FunUtils

class PointofsaleBatchAdapter(
    val context: Context,
    private val posItemList: List<StoreProData>,
    @Suppress("unused") private val onDeleteItemClickListener: OnDeleteItemClickListener,
    @Suppress("unused") private val onQuantityChangeListener: OnQuantityChangeListener,
    private val batchList: List<PosSaleBatch>,
    private val parentposition: Int,
    private val onBatchQuantityChange: (List<PosSaleBatch>) -> Unit,
) : RecyclerView.Adapter<PointofsaleBatchAdapter.POSItemViewHolder>() {

    private val localizationData = LocalizationHelper(context).getLocalizationData()

    // Keep non-zero stock rows; if none, show all (so UI isn’t blank)
    private val filtered = batchList.filter { FunUtils.DtoString(it.quantity) != "0" }
    private val displayList = if (filtered.isEmpty()) batchList else filtered

    // Working list of current (>0) quantities that we send upward
    private val batchCartList = mutableListOf<PosSaleBatch>().apply {
        addAll(
            displayList.map {
                PosSaleBatch(
                    it.batch_no,
                    it.quantity,
                    it.price,
                    it.batch_cart_quantity // keep whatever was there
                )
            }
        )
    }

    class POSItemViewHolder(val binding: PointofsaleBatchLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POSItemViewHolder {
        return POSItemViewHolder(
            PointofsaleBatchLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: POSItemViewHolder, position: Int) {
        val row = displayList[position]
        val parentItem = posItemList[parentposition]
        val isLooseOil = FunUtils.isLooseOil(parentItem.category_id, parentItem.pack_product_description)

        val priceStr = NumberFormatter().formatPrice(row.price.toString() ?: "-", localizationData)
        val stockQtyStr = FunUtils.DtoString(row.quantity) // normalize
        val stockLimit = FunUtils.stringToDouble(stockQtyStr)

        val et = holder.binding.saleQuantity

        // 1) Remove any old watcher to avoid stacking
        (et.tag as? TextWatcher)?.let { et.removeTextChangedListener(it) }

        holder.binding.batchName.text = row.batch_no
        holder.binding.batchPrice.text = priceStr
        holder.binding.batchStock.text = stockQtyStr

        // 2) Input rules
        if (isLooseOil) {
            // read-only for loose oil
            et.inputType = InputType.TYPE_NULL
            et.isFocusable = false
            et.isClickable = false
            et.isCursorVisible = false
        } else {
            et.inputType = InputType.TYPE_CLASS_NUMBER
            et.isFocusable = true
            et.isClickable = true
            et.isCursorVisible = true
        }

        // 3) Prefill only if needed and only when not focused to avoid cursor jumps
        val dispQty =
            if (isLooseOil) FunUtils.DtoDouble(row.batch_cart_quantity) else row.batch_cart_quantity
        val textToShow = if (dispQty == 0.0) "" else FunUtils.DtoString(dispQty)

        if (!et.hasFocus()) {
            val current = et.text?.toString() ?: ""
            if (current != textToShow) {
                et.setText(textToShow)
                et.setSelection(et.text?.length ?: 0)
            }
        }

        // Disable whole row if out of stock
        val enabled = stockQtyStr != "0"
        et.isEnabled = enabled
        holder.binding.root.alpha = if (enabled) 1f else 0.5f

        // 4) Attach a safe watcher that won’t fight the user while typing
        val watcher = object : TextWatcher {
            var suppress = false
            var beforeVal = 0.0
            var beforeSelStart = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (suppress) return
                beforeVal = FunUtils.stringToDouble(s?.toString() ?: "")
                beforeSelStart = et.selectionStart.coerceAtLeast(0)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (suppress) return

                // Let the user clear the field; treat as 0 but don’t auto-rewrite text
                val raw = s?.toString() ?: ""
                val entered = if (raw.isEmpty()) 0.0 else FunUtils.stringToDouble(raw)

                if (entered <= stockLimit) {
                    // Update working list and notify upward with non-zero items only
                    updateReceivedQuantity(row, entered)
                } else {
                    // Over limit → gently revert to the previous value and restore cursor
                    Toast.makeText(context, "The quantity cannot be more than the stock.", Toast.LENGTH_SHORT).show()
                    suppress = true
                    val revertText = if (beforeVal == 0.0) "" else FunUtils.DtoString(beforeVal)
                    et.setText(revertText)
                    // Restore cursor close to where user was typing
                    val newLen = et.text?.length ?: 0
                    val newSel = beforeSelStart.coerceAtMost(newLen)
                    et.setSelection(newSel)
                    suppress = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // No-op — all logic in onTextChanged
            }
        }

        et.addTextChangedListener(watcher)
        et.tag = watcher
    }

    private fun updateReceivedQuantity(item: PosSaleBatch, value: Double) {
        val idx = batchCartList.indexOfFirst { it.batch_no == item.batch_no }
        if (value > 0) {
            if (idx >= 0) {
                batchCartList[idx] = batchCartList[idx].copy(batch_cart_quantity = value)
            } else {
                batchCartList.add(PosSaleBatch(item.batch_no, item.quantity, item.price, value))
            }
        } else if (idx >= 0) {
            batchCartList.removeAt(idx)
        }
        // send only non-zero to parent
        onBatchQuantityChange(batchCartList.filter { it.batch_cart_quantity > 0 })
    }

    override fun getItemCount(): Int = displayList.size
    override fun getItemViewType(position: Int) = 0
    override fun getItemId(position: Int) = position.toLong()
}
*/
