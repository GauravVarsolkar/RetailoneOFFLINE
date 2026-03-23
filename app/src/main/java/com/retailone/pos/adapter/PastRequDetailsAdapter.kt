package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.PastReqDetailItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsList
import com.retailone.pos.network.Constants

class PastRequDetailsAdapter(
    val context: Context,
    val pastReqDetailsList: PastReqDetailsList
) : RecyclerView.Adapter<PastRequDetailsAdapter.PastRequisitionViewHolder>() {

    val localizationData = LocalizationHelper(context).getLocalizationData()


    class PastRequisitionViewHolder(val binding: PastReqDetailItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PastRequisitionViewHolder {
        return PastRequisitionViewHolder(
            PastReqDetailItemLayoutBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: PastRequisitionViewHolder, position: Int) {

        val item = pastReqDetailsList.order_items[position]

        holder.binding.apply {
            itemName.text = item.product_details.product_name
            itemType.text = item.distribution_pack_details.product_description
            itemUom.text = item.product_details.uom?:""

            val formattedPrice = NumberFormatter().formatPrice(item.whole_sale_price,localizationData)
            itemPrice.text = formattedPrice

            requestValue.text = item.quantity_request.toString()
          //  approvedValue.text = item.approved_quantity.toString()
            approvedValue.text = item.dispatch_qty.toString()
            receivedValue.text = item.received_quantity.toString()

            Glide.with(context)
                .load(Constants.IMAGE_URL + item.product_details.photo)
                .centerCrop() // Add center crop
                .placeholder(R.drawable.temp) // Add a placeholder drawable
                .error(R.drawable.temp) // Add an error drawable (if needed)
                .into(productimg)
        }

        setStatusWiseQuantity(holder.binding, pastReqDetailsList.status,item.approved_quantity)
    }

    private fun setStatusWiseQuantity(
        binding: PastReqDetailItemLayoutBinding,
        status: String,
        approvedQuantity: Int
    ) {

        when (status) {
            "0" -> {
                //textview.text = "Pending"

                binding.requestLayout.isVisible = true
                binding.approvedLayout.isVisible = false
                binding.receivedLayout.isVisible = false

            }

            "1" -> {
                //textview.text = "Approved"
                binding.requestLayout.isVisible = true
                //binding.approvedLayout.isVisible = true
                binding.approvedLayout.isVisible = false
                binding.receivedLayout.isVisible = false

                if(approvedQuantity==0){
                    binding.approvedLayout.isVisible = false
                    binding.notapprovedLayout.isVisible = true
                }
            }

            "4" -> {
                //textview.text = "Dispatched"
                binding.requestLayout.isVisible = true
                binding.approvedLayout.isVisible = true
                binding.receivedLayout.isVisible = false
                if(approvedQuantity==0){
                    binding.approvedLayout.isVisible = false
                    binding.notapprovedLayout.isVisible = true
                }
            }

            "2" -> {
                //textview.text = "Cancelled"
                binding.requestLayout.isVisible = true
                binding.approvedLayout.isVisible = false
                binding.receivedLayout.isVisible = false
            }

            "3" -> {
                //textview.text = "Received"
                binding.requestLayout.isVisible = true
                binding.approvedLayout.isVisible = true
                binding.receivedLayout.isVisible = true
                if(approvedQuantity==0){
                    binding.approvedLayout.isVisible = false
                    binding.receivedLayout.isVisible = false
                    binding.notapprovedLayout.isVisible = true
                }
            }

            else -> {
                // textview.text = "."
                binding.requestLayout.isVisible = false
                binding.approvedLayout.isVisible = false
                binding.receivedLayout.isVisible = false
            }
        }

    }


    override fun getItemCount(): Int {
        return pastReqDetailsList.order_items.size
    }

}