package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.SalesPaymentItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.Sale
import com.retailone.pos.ui.Activity.DashboardActivity.SalesPaymentDetailsActivity
import com.retailone.pos.utils.DateTimeFormatting

class SalesPaymentAdapter(private val salesList: List<Sale>, val context: Context

)  : RecyclerView.Adapter<SalesPaymentAdapter.SalesPaymentViewHolder> (){

    val localizationData = LocalizationHelper(context).getLocalizationData()


    class SalesPaymentViewHolder (val binding:SalesPaymentItemLayoutBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesPaymentViewHolder {
        return SalesPaymentViewHolder(SalesPaymentItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: SalesPaymentViewHolder, position: Int) {

        val item = salesList[position]
        val roundedTotal = java.math.BigDecimal.valueOf(item.grand_total).setScale(0, java.math.RoundingMode.HALF_UP)
        val formattedPrice = NumberFormatter().formatPrice(roundedTotal.toPlainString(),localizationData)
        // holder.binding.txt1.text = attendance_item.month
        holder.binding.txt1.text = ((item?.invoice_id?:"").toString())
        // Set red color if price is negative
        holder.binding.txt2.text = formattedPrice
        if (item.grand_total < 0) {
            holder.binding.txt2.setTextColor(context.getColor(android.R.color.holo_red_dark))
        } else {
            holder.binding.txt2.setTextColor(context.getColor(android.R.color.black)) // or your default
        }
        //holder.binding.txt2.text =formattedPrice
        item.created_at?.let {
            holder.binding.txt3.text = DateTimeFormatting.formatGlobalTime(it,localizationData.timezone)
        }
        holder.binding.txt4.text = item?.payment_type?:""

        holder.binding.linear.setOnClickListener {
            val intent = Intent(context, SalesPaymentDetailsActivity::class.java)
            intent.putExtra("sale_id",item.id.toString())
           // Toast.makeText(context, item.id, Toast.LENGTH_SHORT).show()
            context.startActivity(intent)
        }
    }


    override fun getItemCount(): Int {
        return salesList.size
    }




}