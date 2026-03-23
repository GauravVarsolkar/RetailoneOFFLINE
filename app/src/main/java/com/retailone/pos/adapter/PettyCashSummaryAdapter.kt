package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.PettycashItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.PettycashReportModel.PettyCashData
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.Sale
import com.retailone.pos.ui.Activity.DashboardActivity.SalesPaymentDetailsActivity
import com.retailone.pos.utils.DateTimeFormatting
import com.retailone.pos.utils.FunUtils

class PettyCashSummaryAdapter(private val pcList: List<PettyCashData>, val context: Context

)  : RecyclerView.Adapter<PettyCashSummaryAdapter.PettyCashSummaryViewHolder> (){

    val localizationData = LocalizationHelper(context).getLocalizationData()


    class PettyCashSummaryViewHolder (val binding:PettycashItemLayoutBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PettyCashSummaryViewHolder {
        return PettyCashSummaryViewHolder(PettycashItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: PettyCashSummaryViewHolder, position: Int) {

        val item = pcList[position]
       // val formattedPriceopen = NumberFormatter().formatPrice(item.petty_cash_opening_balance.toString(),localizationData)
       // val formattedPricercv = NumberFormatter().formatPrice(item.txn_amount.toString(),localizationData)



        // holder.binding.txt1.text = attendance_item.month
        holder.binding.txt1.text = item?.txn_date
        holder.binding.txt2.text = FunUtils.formatPrintPrice(item.pettycash_opening_balance.toString())
//        item.created_at?.let {
//            holder.binding.txt3.text = DateTimeFormatting.formatGlobalTime(it,localizationData.timezone)
//        }
       holder.binding.txt3.text = FunUtils.formatPrintPrice(item.total_txn_amount.toString())
        holder.binding.txt4.text = FunUtils.formatPrintPrice(item.total_expenses.toString())
        holder.binding.txt5.text = FunUtils.formatPrintPrice(item.pettycash_closing_balance_actual.toString())


    }


    override fun getItemCount(): Int {
        return pcList.size
    }




}