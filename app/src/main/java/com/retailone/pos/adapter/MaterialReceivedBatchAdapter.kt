package com.retailone.pos.adapter
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.MaterialreceivedBatchLayoutBinding

import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvItem
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.DispatchBatchDetails
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.OrderItem
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsList
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MaterialReceivedBatchAdapter(
    val context: Context,
    val pastReqDetailsList: PastReqDetailsList,
    val dispatch_date: String?,
    val batchnoList: List<DispatchBatchDetails>,
    val onBatchQuantityChange: (List<DispatchBatchDetails>) -> Unit,

) : RecyclerView.Adapter<MaterialReceivedBatchAdapter.MaterialRcvdViewHolder>() {


    // Global list to maintain received quantities
    private val matReceivedList = mutableListOf<DispatchBatchDetails>()
    private val localizationData = LocalizationHelper(context).getLocalizationData()

    init {
        // Initialize matReceivedList with default values
        matReceivedList.addAll(batchnoList.map { DispatchBatchDetails(it.batch_no,it.quantity,it.expiry_date, 0) })
    }


    class MaterialRcvdViewHolder(val binding: MaterialreceivedBatchLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialRcvdViewHolder {
        return MaterialRcvdViewHolder(
            MaterialreceivedBatchLayoutBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MaterialRcvdViewHolder, position: Int) {

       val item = batchnoList[position]

        cartControlEditText(holder.binding, item)


        holder.binding.apply {
            batchName.text = item.batch_no
            dispatchQty.text = item.quantity.toString()

            holder.binding.actualquantEdit.text = Editable.Factory.getInstance().newEditable(item.quantity.toString())


        }


//        holder.binding.apply {
//            itemName.text = item.product_details.product_name
//            distPack.text = item.distribution_pack_details.product_description
//
//            val formattedPrice = NumberFormatter().formatPrice(item.whole_sale_price,localizationData)
//            itemPrice.text = formattedPrice
//
//            requestValue.text = item.quantity_request.toString()
//            //  approvedValue.text = item.approved_quantity.toString()
//            approvedValue.text = item.dispatch_qty.toString()
//            receivedValue.text = item.received_quantity.toString()
//
//            Glide.with(context)
//                .load(Constants.IMAGE_URL + item.product_details.photo)
//                .centerCrop() // Add center crop
//                .placeholder(R.drawable.temp) // Add a placeholder drawable
//                .error(R.drawable.temp) // Add an error drawable (if needed)
//                .into(productimg)
//
//        }

        // cartControl(holder.binding, item)

//        setStatusWiseQuantity(holder.binding, pastReqDetailsList.status,item.approved_quantity)
      // cartControlEditText(holder.binding, item)
        /*  if(pastReqDetailsList.status =="1"){
              holder.binding.mlEdit.text = Editable.Factory.getInstance().newEditable(item.approved_quantity.toString())
          }*/

//        holder.binding.received.setOnClickListener {
//            holder.binding.received.isVisible = false
//            holder.binding.mllayout.isVisible = true
//            if(pastReqDetailsList.status =="4"){
//                holder.binding.mlEdit.text = Editable.Factory.getInstance().newEditable(item.dispatch_qty.toString())
//            }
//        }

    }



    override fun getItemCount(): Int {
       // return pastReqDetailsList.order_items.size
        return batchnoList.size
    }


    private fun cartControlEditText(binding: MaterialreceivedBatchLayoutBinding, item:  DispatchBatchDetails) {

        var oldnum = 0

        binding.actualquantEdit.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                try {
                    oldnum = s.toString().toInt()
                } catch (e: NumberFormatException) {
                    // Handle the exception if needed
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val enteredValue = s.toString().toIntOrNull() ?: 0

                    if (enteredValue <= item.quantity.toInt()) {
                        updateReceivedQuantity(item, enteredValue)

                    } else {
                        Toast.makeText(
                            context,
                            "The received quantity cannot be more than the dispatch quantity.",
                            Toast.LENGTH_SHORT
                        ).show()

                        binding.actualquantEdit.text = Editable.Factory.getInstance().newEditable(oldnum.toString())
                        // Set the cursor to the last position
                        binding.actualquantEdit.setSelection(binding.actualquantEdit.text.length)
                    }
                } catch (e: NumberFormatException) {
                    // Handle the exception if needed
                }
            }
            override fun afterTextChanged(s: Editable?) {
                // Perform actions after the text has changed if needed
            }

        })
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun isBeforeDispatchDate(dispatchDate: String): Boolean {
        // Define the date format
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // Parse the dispatch date
        val dispatchDateTime = LocalDate.parse(dispatchDate, formatter)

        // Get the current date
        val currentDate = LocalDate.now()

        // Compare the current date with the dispatch date
        return currentDate.isBefore(dispatchDateTime)
    }

    private fun updateReceivedQuantity(item: DispatchBatchDetails, value: Int) {
        // Find the MatReceivedItem for the corresponding OrderItem
        val matRcvItem = matReceivedList.find { it.batch_no == item.batch_no }

        // Update the quantity for the found MatReceivedItem
        matRcvItem?.received_quantity = value ?: 0

        // Call the callback function with the updated list
        onBatchQuantityChange(matReceivedList)
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemId(position: Int) = position.toLong()

    override fun onViewRecycled(holder: MaterialReceivedBatchAdapter.MaterialRcvdViewHolder) {
        super.onViewRecycled(holder)
        //holder.binding.quantEdit.removeTextChangedListener(holder.textWatcher)
    }


}