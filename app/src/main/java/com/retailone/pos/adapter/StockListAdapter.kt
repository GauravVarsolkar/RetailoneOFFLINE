package com.retailone.pos.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.databinding.ItemStockProductCardBinding
import com.retailone.pos.models.GoodsToWarehouseModel.ReturnStocks.ReturnBatchItem
import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnItem
import com.retailone.pos.models.GoodsToWarehouseModel.Stocklist.DistributionPack
import com.retailone.pos.models.GoodsToWarehouseModel.Stocklist.Product
import com.retailone.pos.models.GoodsToWarehouseModel.Stocklist.ReturnedItemDetails

class StockListAdapter(
    private val products: List<Product>,
    private val reasonNames: List<String>
) : RecyclerView.Adapter<StockListAdapter.StockViewHolder>() {

    private val batchAdapters = mutableListOf<BatchListAdapter>()

    inner class StockViewHolder(val binding: ItemStockProductCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemStockProductCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StockViewHolder(binding)
    }

    // 🔹 Helpers to read fields from ReturnedItemDetails = Map<String, Int>
    private fun ReturnedItemDetails.totalQty(): Int =
        this["total_quantity"] ?: 0

    private fun ReturnedItemDetails.totalBottles(): Int =
        this["total_bottles"] ?: 0

    // 👇 Same rule the child uses to display tvAvailableQty
    private fun availableQtyFor(item: ReturnBatchItem): Int {
        val isGoodFromReturnedMap =
            !item.isEditable && item.condition.equals("good", ignoreCase = true)

        return when {
            isGoodFromReturnedMap -> item.returnedQty
            item.condition.equals("good", ignoreCase = true) -> item.stockqqty
            else -> item.returnedQty
        }
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val product = products[position]
        val context = holder.itemView.context

        with(holder.binding) {

            tvProductName.text = product.product_name
            tvProductCode.text = "${product.product_id}"

            // 🔹 Total stock = store stock + returned_items + good_returned_items
            val totalQty = product.distribution_pack_data.sumOf { pack ->
                val stock = pack.stock_quatity

                val returnedQty = pack.returned_items
                    ?.values
                    ?.sumOf { details -> details.totalQty() } ?: 0

                val goodReturnedQty = pack.good_returned_items
                    ?.values
                    ?.sumOf { details -> details.totalQty() } ?: 0

                stock + returnedQty + goodReturnedQty
            }

            // 🔹 Non expired store stock + good_returned_items (counted as usable)
            val nonExpiredStockQty = product.distribution_pack_data
                .filterNot { isExpired(it.expiry_date) }
                .sumOf { pack ->
                    val goodReturnedQty = pack.good_returned_items
                        ?.values
                        ?.sumOf { details -> details.totalQty() } ?: 0

                    pack.stock_quatity + goodReturnedQty


                   /* Log.d(
                        "Sellable",
                        "Good Returned Qty: " + goodReturnedQty +
                                ", Stock Quantity: " + pack.stock_quatity

                    );*/


                }

            Log.d(
                "Sellable",
               "Total Quantity: " + nonExpiredStockQty
            );




            //Selleble Stock
            tvstorestockQty.text = "$nonExpiredStockQty units"

            // 🔁 Flatten to the same list the child binds
            val flattenedBatchLists =
                flattenReturnItems(product.product_id, product.distribution_pack_data)

            // ✅ If you want to show total available instead of total stock, you can swap these
            val overallAvailableQty = flattenedBatchLists.sumOf { availableQtyFor(it) }
            // tvStockQty.text = "$overallAvailableQty units"
            ///total stock
            tvStockQty.text = "$totalQty units"

            tvRequestedQty.text = "${product.previous_requested_quantity} units"

            val isStoreStockZero =
                product.distribution_pack_data.sumOf { it.stock_quatity } == 0
            val isTotalStockZero = totalQty == 0
            val isRequestedQtyZero = product.previous_requested_quantity == 0

            // 🔒 Hide expandable layout if everything is zero
            if (isStoreStockZero && isTotalStockZero && isRequestedQtyZero) {
                llExpandable.visibility = View.GONE
                ivExpand.visibility = View.GONE
            } else {
                llExpandable.visibility = View.GONE // Default collapsed
                ivExpand.visibility = View.VISIBLE

                val isExpanded = llExpandable.visibility == View.VISIBLE
                ivExpand.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        if (isExpanded) R.drawable.up else R.drawable.down
                    )
                )

                ivExpand.setOnClickListener {
                    val expanded = llExpandable.visibility == View.VISIBLE
                    llExpandable.visibility = if (expanded) View.GONE else View.VISIBLE

                    val iconRes = if (expanded) R.drawable.down else R.drawable.up
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            iconRes
                        )
                    )
                }
            }

            // ✅ Flatten returned_items + good_returned_items into batches for child adapter
            val flattenedBatchList =
                flattenReturnItems(product.product_id, product.distribution_pack_data)

            val batchAdapter = BatchListAdapter(flattenedBatchList, reasonNames)

            rvBatchList.layoutManager = LinearLayoutManager(context)
            rvBatchList.adapter = batchAdapter

            batchAdapters.add(batchAdapter)
        }
    }

    override fun getItemCount(): Int = products.size

    fun getSelectedItems(): List<StockReturnItem> {
        return batchAdapters.flatMap { it.getSelectedItems() }
    }

    private fun flattenReturnItems(
        productId: Int,
        batches: List<DistributionPack>
    ): List<ReturnBatchItem> {
        val result = mutableListOf<ReturnBatchItem>()

        for (batch in batches) {
            val totalStockQty = batch.stock_quatity

            // ✅ Auto row from actual store stock
            if (totalStockQty > 0) {
                val condition =
                    if (isExpired(batch.expiry_date)) "Expired" else "Store Stock"

                result.add(
                    ReturnBatchItem(
                        productId = productId,
                        stockqqty = totalStockQty,
                        batchNo = batch.batch_no,
                        noOfPacks = batch.no_of_packs,
                        condition = condition,
                        returnedQty = totalStockQty,
                        isEditable = true,
                        expiry_date = batch.expiry_date,
                        fromGoodReturnedMap = false,
                        totalBottles = 0      // no bottle info for raw store stock
                    )
                )
            }

            // ✅ returned_items (e.g. excessive_stock, others, etc.)
            val returnedMap = batch.returned_items ?: emptyMap()
            for ((condition, details) in returnedMap) {
                val qty = details.totalQty()
                val bottles = details.totalBottles()

                if (qty > 0 || bottles > 0) {
                    result.add(
                        ReturnBatchItem(
                            productId = productId,
                            stockqqty = totalStockQty,
                            batchNo = batch.batch_no,
                            noOfPacks = batch.no_of_packs,

                            condition = condition,
                            returnedQty = qty,
                            isEditable = true,
                            expiry_date = batch.expiry_date,
                            fromGoodReturnedMap = false,
                            totalBottles = bottles
                        )
                    )
                }
            }

            // ✅ good_returned_items (e.g. expired/defective/no_sell that are pre-recorded)
            val goodReturnedMap = batch.good_returned_items ?: emptyMap()
            for ((condition, details) in goodReturnedMap) {
                val qty = details.totalQty()
                val bottles = details.totalBottles()

                if (qty > 0 || bottles > 0) {
                    result.add(
                        ReturnBatchItem(
                            productId = productId,
                            stockqqty = totalStockQty,
                            batchNo = batch.batch_no,
                            noOfPacks = batch.no_of_packs,

                            condition = condition,
                            returnedQty = qty,
                            isEditable = true,          // you kept it editable
                            expiry_date = batch.expiry_date,
                            fromGoodReturnedMap = true,
                            totalBottles = bottles
                        )
                    )
                }
            }
        }

        return result
    }

    private fun isExpired(expiryDateStr: String?): Boolean {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val expiryDate = sdf.parse(expiryDateStr ?: return false)
            val today = java.util.Calendar.getInstance().time
            expiryDate.before(today)
        } catch (e: Exception) {
            false
        }
    }
}
