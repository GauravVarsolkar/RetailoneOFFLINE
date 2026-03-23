package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.retailone.pos.databinding.ReplaceItemLayoutBinding
import com.retailone.pos.interfaces.OnReturnQuantityChangeListener
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.ReturnSalesItemModel.BatchReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData
import com.retailone.pos.models.ReturnSalesItemModel.SalesItem
import com.retailone.pos.utils.FunUtils

class ReplaceSalesItemAdapter(
    private val returnitem: List<ReturnItemData>,
    private val context: Context,
    private val onReturnQuantityChangeListener: OnReturnQuantityChangeListener,
    /** Keep parent-checked state by ADAPTER POSITION (IDs can be 0). */
    private val selectedParentPositions: MutableSet<Int>,
    /** Activity callback: (adapterPosition, selected batches snapshot for this product). */
    private val onBatchChange: (adapterPosition: Int, selectedForThisProduct: List<BatchReturnItem>) -> Unit,
    /** Parent checkbox toggle => (productId, checked, adapterPosition) */
    private val onParentToggle: (Int, Boolean, Int) -> Unit,
    /** 🔒 when true, parent & batch checkboxes are read-only (disabled) by default. */
    private val readOnly: Boolean = false

) : RecyclerView.Adapter<ReplaceSalesItemAdapter.StockSearchViewHolder>() {

    private val sharedPrefHelper = SharedPrefHelper(context)
    private val localizationData = LocalizationHelper(context).getLocalizationData()

    class StockSearchViewHolder(val binding: ReplaceItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)


    // productId -> total sale quantity
    private val saleQuantityByProductId: Map<Int, Double> =
        returnitem
            .firstOrNull()
            ?.sales_items
            ?.groupBy { it.product_id }
            ?.mapValues { (_, list) ->
                list.sumOf { it.quantity }
            }
            ?: emptyMap()

    // productId -> total store stock
    private val storeStockByProductId: Map<Int, Int> =
        returnitem
            .firstOrNull()
            ?.sales_items
            ?.groupBy { it.product_id }
            ?.mapValues { (_, list) ->
                list.sumOf { it.store_stock }
            }
            ?: emptyMap()

    // productId -> on_hold (1 if any detailed row for this product is on hold)
    private val onHoldByProductId: Map<Int, Int> =
        returnitem
            .firstOrNull()
            ?.sales_items
            ?.groupBy { it.product_id }
            ?.mapValues { (_, list) ->
                if (list.any { it.on_hold == 1 }) 1 else 0
            }
            ?: emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSearchViewHolder {
        return StockSearchViewHolder(
            ReplaceItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }



    override fun onBindViewHolder(holder: StockSearchViewHolder, position: Int) {


        val gson = Gson()

        val json = gson.toJson(returnitem)   // Convert object to JSON
         gson.toJson(returnitem)
        Log.d("ReplaceAdapterX",json.toString())

        val item: SalesItem = returnitem[0].salesItems[position]

        val totalReplacedAmount = returnitem[0].total_replaced_amount
        val isInvoiceAlreadyReplaced = (totalReplacedAmount ?: 0.0) > 0.0

        val productId = item.product_id
        val productQuantity = item.quantity

        val storeStockForProduct = storeStockByProductId[productId] ?: 0
        val saleQuantity = saleQuantityByProductId[productId] ?: 0.0

        val productOnHold = onHoldByProductId[productId] ?: 0



        Log.d(
            "ReplaceAdapter",
            "productId=$productId, qty=$productQuantity, storeStock=$storeStockForProduct, " +
                    "productOnHold=$productOnHold, totalReplacedAmount=$totalReplacedAmount, " +
                    "isInvoiceAlreadyReplaced=$isInvoiceAlreadyReplaced"
        )

        // 🚩 Special case from your condition:
        // totalreplaced != null && productOnHold == 1 && storeStock > 1
        val shouldForceEnableCheckbox =
            isInvoiceAlreadyReplaced && productOnHold == 1 && storeStockForProduct > 1

        // For this row: treat it as readOnly only if global readOnly is true AND NOT in special case
        val rowReadOnly = readOnly && !shouldForceEnableCheckbox

       // val isOnHold = storeStockForProduct <= 0
        val canShowHold = storeStockForProduct.toDouble() < saleQuantity

        holder.binding.llHold.visibility =
            if (!rowReadOnly && canShowHold) android.view.View.VISIBLE else android.view.View.GONE

        // Header
        holder.binding.itemName.text = item.product.product_name
        holder.binding.itemDesc.text = item.distribution_pack.product_description
        holder.binding.itemUnit.text = "Purchase -  " + FunUtils.DtoString(item.quantity)
        holder.binding.itemPrice.text = "Rate -   " +
                NumberFormatter().formatPrice(item.retail_price.toString(), localizationData)

        // ================== Parent checkbox ==================
        val cb = holder.binding.itemCheckbox
        cb.setOnCheckedChangeListener(null)

        val parentChecked = selectedParentPositions.contains(position)
        cb.isChecked = parentChecked

        // Enable/disable with special-case override
        cb.isEnabled = !rowReadOnly
        cb.isClickable = !rowReadOnly
        holder.binding.itemCheckbox.alpha = if (!rowReadOnly) 1f else 0.6f

        if (!rowReadOnly) {
            cb.setOnCheckedChangeListener { _, checked ->
                val safePos =
                    holder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
                if (checked) selectedParentPositions.add(safePos) else selectedParentPositions.remove(
                    safePos
                )
                Log.d(
                    "ReplaceBind",
                    "Parent checkbox toggled: productId=${item.id}, pos=$safePos, checked=$checked, current=$selectedParentPositions"
                )
                onParentToggle(item.id, checked, safePos)
                notifyItemChanged(safePos)
            }
        } else {
            cb.setOnCheckedChangeListener(null)
        }

        // ================== Batch list UI ==================
        holder.binding.quantLayout.visibility = android.view.View.GONE
        holder.binding.batchLayouts.visibility = android.view.View.VISIBLE
        holder.binding.batchRcv.visibility = android.view.View.VISIBLE

        // Apply read-only only if NOT in special override
        holder.binding.batchLayouts.alpha = if (rowReadOnly) 0.6f else 1f
        if (rowReadOnly) {
            holder.binding.batchRcv.setOnTouchListener { _, _ -> true } // consume touches
        } else {
            holder.binding.batchRcv.setOnTouchListener(null)
        }

        // ================== Child/batch list ==================
        item.batches?.let { batches ->
            holder.binding.batchRcv.apply {
                layoutManager = LinearLayoutManager(holder.itemView.context, RecyclerView.VERTICAL, false)
                setHasFixedSize(true)

                // 🔑 IMPORTANT:
                // - For special-case row, do NOT send "read-only" to child adapter
                //   even if invoice is already replaced.
                val childReadOnly = isInvoiceAlreadyReplaced && !shouldForceEnableCheckbox
                val parentEnabledForChild = parentChecked || shouldForceEnableCheckbox

                adapter = ReplaceSalesItemBatchAdapter(
                    returnitem,
                    context,
                    batches,
                    childReadOnly,              // behaves like "readOnly" inside child
                    parentEnabled = parentEnabledForChild
                ) { selectedForThisProduct ->
                    val safePos =
                        holder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
                    // Allow batch selection in special case too
                    if (!rowReadOnly) {
                        onBatchChange(safePos, selectedForThisProduct)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = returnitem[0].salesItems.size
    override fun getItemViewType(position: Int) = position
    override fun getItemId(position: Int) = position.toLong()
}
