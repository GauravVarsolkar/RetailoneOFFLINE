package com.retailone.pos.adapter.ExpenseRegisterAdapter

import NumberFormatter
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.google.android.material.button.MaterialButton
import com.retailone.pos.R
import com.retailone.pos.databinding.ExpenseHistoryAdapterLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseHistory.ExpenseHistoryData
import com.retailone.pos.network.Constants
import com.retailone.pos.ui.Activity.MPOSDashboardActivity
import com.retailone.pos.utils.DateTimeFormatting
import com.bumptech.glide.request.target.Target


class ExpenseHistoryAdapter(
    val context: Context,
    val expenseHistoryList: List<ExpenseHistoryData>
) : RecyclerView.Adapter<ExpenseHistoryAdapter.ExpenseHistoryViewHolder>() {

    val localizationData = LocalizationHelper(context).getLocalizationData()



    class ExpenseHistoryViewHolder(val binding: ExpenseHistoryAdapterLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseHistoryViewHolder {
        return ExpenseHistoryViewHolder(
            ExpenseHistoryAdapterLayoutBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ExpenseHistoryViewHolder, position: Int) {

        val item = expenseHistoryList[position]

       // val datemodel = DateFormatter(item.pi_date).formatDateModel()

        val formattedPrice = NumberFormatter().formatPrice(item.total_amount.toString(),localizationData)


        holder.binding.apply {
            name.text = "Name: "+ (item.vendor?.vendor_name?:"-")
            category.text = "Category: "+ item.category?.category_name?:""
            price.text = "Amount: " + formattedPrice

            item.created_at?.let {
                holder.binding.date.text ="Date : "+ DateTimeFormatting.formatOrderdate(it,localizationData.timezone)
            }
           // date.text = datemodel.day.toString()
           // month.text = datemodel.month.toString()
           // itemQuantity.text = "${item.order_items.size} items"

           // setStatusString(item.status, status)
        }

        holder.binding.invoiceimg.setOnClickListener {
            showImageDialog(context,item.invoice)
        }


    }



    private fun showImageDialog(context: Context, invoice: String?) {

        val dialog = Dialog(context)
        dialog.setContentView(R.layout.image_dialog_layout)
        dialog.setCancelable(true)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)

        val imageView = dialog.findViewById<ImageView>(R.id.imageView)
        val closeimg = dialog.findViewById<ImageView>(R.id.closeimg)

        // Show progress dialog
        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        Glide.with(context)
            .load(invoice)
            .centerCrop()
            .placeholder(R.drawable.temp)
            .error(R.drawable.temp)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    // Dismiss progress dialog if loading fails
                    progressDialog.dismiss()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    // Dismiss progress dialog if loading is successful
                    progressDialog.dismiss()
                    return false
                }
            })
            .into(imageView)

        closeimg.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }



    override fun getItemCount(): Int {

        return expenseHistoryList.size
    }

}