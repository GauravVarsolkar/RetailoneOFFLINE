package com.retailone.pos.adapter

import ReturnedProduct
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.R
import com.retailone.pos.databinding.ItemProductReturnBinding

class ConfirmReturnProductAdapter(
    private val items: List<ReturnedProduct>
) : RecyclerView.Adapter<ConfirmReturnProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ItemProductReturnBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductReturnBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvProductName.text = item.product?.product_name ?: "Unnamed"
        holder.binding.tvCondition.text = "Condition: ${item.condition}"
        holder.binding.tvQuantity.text = "Qty: ${item.approved_quantity}"

        // Check if item is rejected and set red background
            if (item.rejected == 1) {
            holder.binding.root.setCardBackgroundColor(holder.binding.root.context.getColor(R.color.primary))
                holder.binding.tvQuantity.setTextColor(holder.binding.root.context.getColor(R.color.white))
                holder.binding.tvCondition.setTextColor(holder.binding.root.context.getColor(R.color.white))
                holder.binding.tvProductName.setTextColor(holder.binding.root.context.getColor(R.color.white))

            // You must define `red` in colors.xml
        } else {
            holder.binding.root.setCardBackgroundColor(holder.binding.root.context.getColor(R.color.white))
                holder.binding.tvQuantity.setTextColor(holder.binding.root.context.getColor(R.color.black))
                holder.binding.tvCondition.setTextColor(holder.binding.root.context.getColor(R.color.black))
                holder.binding.tvProductName.setTextColor(holder.binding.root.context.getColor(R.color.black))

            }
    }

    override fun getItemCount(): Int = items.size
}
