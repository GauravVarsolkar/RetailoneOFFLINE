//package com.retailone.pos.adapter
//
//import android.content.Context
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.core.view.isVisible
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.retailone.pos.R
//import com.retailone.pos.databinding.PiCategoryItemLayoutBinding
//import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.CategoryData
//
//
//class PiCategoryAdapter (
//    private val categorylist: List<CategoryData>, val context: Context
//) : RecyclerView.Adapter<PiCategoryAdapter.PiParentViewHolder> (){
//
//    private var selectedItemPosition = -1
//
//
//
//
//
//    class PiParentViewHolder (val binding: PiCategoryItemLayoutBinding) : RecyclerView.ViewHolder(binding.root){
//
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PiParentViewHolder {
//        return PiParentViewHolder(PiCategoryItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
//    }
//    override fun onBindViewHolder(holder: PiParentViewHolder, position: Int) {
//        val categoryItem = categorylist[position]
//
//        // ✅ Filter out products that have all zero values
////        val filteredProducts = categoryItem.products.filter { product ->
////            product.distribution_pack_data.any { pack ->
////                val stockQty = pack.stock_quatity ?: 0.0  // Double
////                val returnedItemsQty = pack.returned_items?.values?.sum() ?: 0  // Int
////                val goodReturnedItemsQty = pack.good_returned_items?.values?.sum() ?: 0  // Int
////
////                stockQty > 0.0 || returnedItemsQty > 0 || goodReturnedItemsQty > 0
////            }
////        }
//
//        val filteredProducts = categoryItem.products.filter { product ->
//            product.distribution_pack_data.any { pack ->
//                val stockQty = pack.stock_quatity ?: 0.0  // Double
//
//                // Now each value is ReturnedItemDetails, so sum total_quantity
//                val returnedItemsQty = pack.returned_items
//                    ?.values
//                    ?.sumOf { it.total_quantity } ?: 0
//
//                val goodReturnedItemsQty = pack.good_returned_items
//                    ?.values
//                    ?.sumOf { it.total_quantity } ?: 0
//
//                stockQty > 0.0 || returnedItemsQty > 0 || goodReturnedItemsQty > 0
//            }
//        }
//
//
//        // ❌ If no products are left after filtering, hide the entire category card
//        if (filteredProducts.isEmpty()) {
//            holder.binding.root.visibility = View.GONE
//            holder.binding.root.layoutParams = RecyclerView.LayoutParams(0, 0) // Also remove space
//            return
//        }
//
//        // ✅ Bind data to views only if products are valid
//        holder.binding.categoryRcv.apply {
//            layoutManager = LinearLayoutManager(holder.itemView.context, RecyclerView.VERTICAL, false)
//            setHasFixedSize(false)
//            isNestedScrollingEnabled = false
//            overScrollMode = View.OVER_SCROLL_NEVER
//            adapter = PiParentAdapter(context, filteredProducts, categoryItem.category_name)
//        }
//
//        holder.binding.serviceName.text = " Category : ${categoryItem.category_name}"
//
//        holder.binding.categoryHead.setOnClickListener {
//            if (!holder.binding.categoryRcv.isVisible) {
//                holder.binding.categoryRcv.visibility = View.VISIBLE
//                holder.binding.arrowImage.setImageResource(R.drawable.svg_up_arrow)
//            } else {
//                holder.binding.categoryRcv.visibility = View.GONE
//                holder.binding.arrowImage.setImageResource(R.drawable.svg_down_arrow)
//            }
//        }
//    }
//
//
//
//    override fun getItemCount(): Int {
//        return categorylist.size
//    }
//
//}


package com.retailone.pos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.databinding.PiCategoryItemLayoutBinding
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.CategoryData

class PiCategoryAdapter(
    private val categorylist: List<CategoryData>,
    val context: Context
) : RecyclerView.Adapter<PiCategoryAdapter.PiParentViewHolder>() {

    class PiParentViewHolder(val binding: PiCategoryItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PiParentViewHolder {
        return PiParentViewHolder(
            PiCategoryItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PiParentViewHolder, position: Int) {
        val categoryItem = categorylist[position]

        // ✅ FIX: Added good_returned_items to filter logic
        val filteredProducts = categoryItem.products.filter { product ->
            product.distribution_pack_data.any { pack ->
                val stockQty = pack.stock_quatity ?: 0.0

                // ✅ Better null-safe handling of returned_items
                val returnedItemsQty = pack.returned_items
                    ?.values
                    ?.sumOf { detailsMap ->
                        detailsMap["total_quantity"] ?: detailsMap.values.sum()
                    } ?: 0

                // ✅ ADDED: Check good_returned_items
                val goodReturnedItemsQty = pack.good_returned_items
                    ?.values
                    ?.sumOf { detailsMap ->
                        detailsMap["total_quantity"] ?: detailsMap.values.sum()
                    } ?: 0

                stockQty > 0.0 || returnedItemsQty > 0 || goodReturnedItemsQty > 0
            }
        }

        // If no products after filtering, hide category
        if (filteredProducts.isEmpty()) {
            holder.binding.root.visibility = View.GONE
            holder.binding.root.layoutParams = RecyclerView.LayoutParams(0, 0)
            return
        }

        // Bind data
        holder.binding.categoryRcv.apply {
            layoutManager = LinearLayoutManager(
                holder.itemView.context,
                RecyclerView.VERTICAL,
                false
            )
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            adapter = PiParentAdapter(context, filteredProducts, categoryItem.category_name)
        }

        holder.binding.serviceName.text = " Category : ${categoryItem.category_name}"

        holder.binding.categoryHead.setOnClickListener {
            if (!holder.binding.categoryRcv.isVisible) {
                holder.binding.categoryRcv.visibility = View.VISIBLE
                holder.binding.arrowImage.setImageResource(R.drawable.svg_up_arrow)
            } else {
                holder.binding.categoryRcv.visibility = View.GONE
                holder.binding.arrowImage.setImageResource(R.drawable.svg_down_arrow)
            }
        }
    }

    override fun getItemCount(): Int = categorylist.size
}
