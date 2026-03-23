package com.retailone.pos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.PiChildItemLayoutBinding
import com.retailone.pos.databinding.PiParentItemLayoutBinding
import com.retailone.pos.models.ProductInventoryModel.PiChildData
import com.retailone.pos.models.ProductInventoryModel.PiResponseModel.Product
import com.retailone.pos.network.Constants

class PiParentAdapter(val context: Context,
                      val productlist: List<Product>,
                      val categoryName: String) : RecyclerView.Adapter<PiParentAdapter.PiChildViewHolder> (){
    private var expandedPosition: Int? = null

    // Maintain a set of expanded items (by position)
    private val expandedItems = mutableSetOf<Int>()

    class PiChildViewHolder (val binding: PiParentItemLayoutBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PiChildViewHolder {
        return PiChildViewHolder(PiParentItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: PiChildViewHolder, position: Int) {
        val product = productlist[position]
        val b = holder.binding
        val isExpanded = expandedItems.contains(position)
        b.itemName.text = product.product_name
        b.txtCategory.text = "Category: $categoryName"

        Glide.with(context)
            .load(Constants.IMAGE_URL + product.photo)
            .centerCrop()
            .placeholder(R.drawable.temp)
            .error(R.drawable.temp)
            .into(b.productimg)

        b.pichildRcv.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = PiChildAdapter(context, product.distribution_pack_data)
        }
        // ✅ Check if any pack contains data in returned_items or good_returned_items
        val hasValidData = product.distribution_pack_data.any { pack ->
            val hasStock = ((pack.stock_quatity ?: 0.0) > 0.0)
            val hasReturned = !pack.returned_items.isNullOrEmpty()
            hasStock || hasReturned
        }
        // Show/hide expandable views
        b.pichildRcv.visibility = if (isExpanded && hasValidData) View.VISIBLE else View.GONE
        b.tableHeader.visibility = if (isExpanded && hasValidData) View.VISIBLE else View.GONE
        // Handle expand/collapse views
        //  b.pichildRcv.visibility = if (isExpanded) View.VISIBLE else View.GONE
        //   b.tableHeader.visibility = if (isExpanded) View.VISIBLE else View.GONE
        // Arrow icon up/down
        b.ivExpandArrow.setImageResource(
            if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
        )
        // Toggle expansion on click
        b.productCard.setOnClickListener {
            if (expandedItems.contains(position)) {
                expandedItems.remove(position) // collapse
            } else {
                expandedItems.add(position)   // expand
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int {
        return productlist.size
    }

}