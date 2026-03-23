package com.retailone.pos.adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.PiUpdateItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.InventoryStockHelper
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.network.Constants

class PiUpdateAdapter(
    private val stockSearchRes: MutableList<StoreProData>,
    val context: Context
) : RecyclerView.Adapter<PiUpdateAdapter.PiUpdateViewHolder>() {

    private val inventoryStockHelper = InventoryStockHelper(context)


    class PiUpdateViewHolder(val binding: PiUpdateItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PiUpdateViewHolder {
        return PiUpdateViewHolder(
            PiUpdateItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PiUpdateViewHolder, position: Int) {

        val updateitem = stockSearchRes[position]

        holder.binding.itemName.text = updateitem.product_name
        holder.binding.itemType.text = updateitem.pack_product_description
      ///  holder.binding.itemPrice.text = updateitem.whole_sale_price
      holder.binding.itemPrice.isVisible = false


        holder.binding.quantEdit.text =
            Editable.Factory.getInstance().newEditable(updateitem.cart_quantity.toString())

        cartControl(holder.binding, updateitem)

        Glide.with(context)
            .load(Constants.IMAGE_URL + updateitem.product_photo)
            .centerCrop() // Add center crop
            .placeholder(R.drawable.temp) // Add a placeholder drawable
            .error(R.drawable.temp) // Add an error drawable (if needed)
            .into(holder.binding.productimg)

        holder.binding.deletelayout.setOnClickListener {
            inventoryStockHelper.removeItem(updateitem.product_id.toString(), updateitem.distribution_pack_id)
            removeItemView(updateitem)
        }

    }


    override fun getItemCount(): Int {
        return stockSearchRes.size
    }


    private fun cartControl(binding: PiUpdateItemLayoutBinding, productitem: StoreProData) {

        binding.quantEdit.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //binding.cartProductQuantity.text = s.toString()

                try {
                    val enteredValue = s.toString().toIntOrNull() ?: 0

                    inventoryStockHelper.updateQuantity(
                        productitem.product_id,
                        productitem.distribution_pack_id,
                        enteredValue
                    )
                } catch (e: NumberFormatException) {
                    // Handle the exception if needed
                }

                /*
                      val enteredValue = s.toString().toIntOrNull() ?: 0
                     if (!s.isNullOrBlank()) {
                     inventoryStockHelper.updateQuantity(
                         productitem.product_id,
                         productitem.distribution_pack_id,
                         enteredValue
                     )
                 } else {
                     // Handle the case where the input is empty, e.g., set a default value or show an error message.
                 }*/
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    private fun removeItemView(item: StoreProData) {
        val position = stockSearchRes.indexOf(item)
        if (position != -1) {
            stockSearchRes.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}

