package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.PosSearchItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.network.Constants
import com.retailone.pos.utils.BatchUtils
import com.retailone.pos.utils.FunUtils

class PosSearchAdapter(
    products: List<StoreProData>,
    private val context: Context,
    private val onItemClick: (StoreProData) -> Unit
) : RecyclerView.Adapter<PosSearchAdapter.StockSearchViewHolder>() {

    private val sharedPrefHelper = SharedPrefHelper(context)
    private val localizationData = LocalizationHelper(context).getLocalizationData()

    // ✅ Final list used by RecyclerView:
    //    ONLY items with total stock > 0
    private val items: List<StoreProData> =
        products.filter { BatchUtils.getTotalPosQuantity(it.batch) > 0 }

    class StockSearchViewHolder(val binding: PosSearchItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSearchViewHolder {
        return StockSearchViewHolder(
            PosSearchItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: StockSearchViewHolder, position: Int) {

        val productitem = items[position]

        // Because of the filter above, this should always be > 0
        val totalStock = BatchUtils.getTotalPosQuantity(productitem.batch)
        val isLooseOil =
            FunUtils.isLooseOil(productitem.category_id, productitem.pack_product_description)

        // make sure whole card is visible and uses WRAP_CONTENT height
        holder.binding.cartLayout.visibility = View.VISIBLE
        val lp = holder.binding.cartLayout.layoutParams
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        holder.binding.cartLayout.layoutParams = lp

        holder.binding.itemName.text = productitem.product_name
        holder.binding.itemDesc.text =
            "${productitem.pack_product_description} (${productitem.uom})"

        val formattedPrice =
            NumberFormatter().formatPrice(productitem.retail_price ?: "-", localizationData)
        holder.binding.itemPrice.text = formattedPrice

        // stock text + add button
        holder.binding.addlayout.isVisible = true
        holder.binding.quantityContainer.isVisible = true

        holder.binding.itemUnit.text =
            "${FunUtils.DtoString(totalStock)} ${if (isLooseOil) "Liters" else "Units"}"
        holder.binding.itemUnit.setTextColor(Color.parseColor("#008000"))

        holder.binding.addcart.setOnClickListener {
            onItemClick(productitem)
        }

        Glide.with(context)
            .load(Constants.IMAGE_URL + productitem.product_photo)
            .centerCrop()
            .placeholder(R.drawable.temp)
            .error(R.drawable.temp)
            .into(holder.binding.productimg)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int) = position
    override fun getItemId(position: Int) = position.toLong()
}
