package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.MaterialreceivedItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvItem
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.DispatchBatchDetails
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.OrderItem
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsList
import com.retailone.pos.network.Constants
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MaterialReceivedAdapter(
    val context: Context,
    val pastReqDetailsList: PastReqDetailsList,
    val dispatch_date:String?,
    val onQuantityChange: (List<MatRcvItem>) -> Unit

) : RecyclerView.Adapter<MaterialReceivedAdapter.MaterialRcvdViewHolder>() {


    // Global list to maintain received quantities
    private val matReceivedList = mutableListOf<MatRcvItem>()
    private val localizationData = LocalizationHelper(context).getLocalizationData()

    init {
        // Initialize matReceivedList with default values
        matReceivedList.addAll(pastReqDetailsList.order_items.map { MatRcvItem(it.id, 0,
            emptyList()
        ) })
    }


    class MaterialRcvdViewHolder(val binding: MaterialreceivedItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialRcvdViewHolder {
        return MaterialRcvdViewHolder(
            MaterialreceivedItemLayoutBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MaterialRcvdViewHolder, position: Int) {

        val item = pastReqDetailsList.order_items[position]



//        holder.binding.batchRcv.apply {
//            layoutManager = LinearLayoutManager(holder.itemView.context,RecyclerView.VERTICAL,false)
//            setHasFixedSize(true)
//            adapter = MaterialReceivedBatchAdapter(context,pastReqDetailsList,dispatch_date,item.batch_no ){
//                Log.d("batch",position.toString()+" "+it.toString())
//
//                updateReceivedQuantityX(it,position)
//            }
//        }

        holder.binding.apply {
            itemName.text = item.product_details.product_name
            distPack.text = item.distribution_pack_details.product_description

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

        // cartControl(holder.binding, item)

        setStatusWiseQuantity(holder.binding, pastReqDetailsList.status,item.approved_quantity)
        cartControlEditText(holder.binding, item)
      /*  if(pastReqDetailsList.status =="1"){
            holder.binding.mlEdit.text = Editable.Factory.getInstance().newEditable(item.approved_quantity.toString())
        }*/

        holder.binding.received.setOnClickListener {
            holder.binding.received.isVisible = false


            ///holder.binding.mllayout.isVisible = true
            holder.binding.mllayout.isVisible = false
            if(pastReqDetailsList.status =="4"){
                holder.binding.mlEdit.text = Editable.Factory.getInstance().newEditable(item.dispatch_qty.toString())
            }


            item.batch_no?.let {
                holder.binding.batchRcv.isVisible = true
                holder.binding.batchLayout.isVisible = true

                holder.binding.batchRcv.apply {
                    layoutManager = LinearLayoutManager(holder.itemView.context,RecyclerView.VERTICAL,false)
                    setHasFixedSize(true)
                    adapter = MaterialReceivedBatchAdapter(context,pastReqDetailsList,dispatch_date,item.batch_no ){
                        Log.d("batch",position.toString()+" "+it.toString())

                        updateReceivedQuantityX(it,position)
                    }
                }
            }

        }

    }


    private fun updateReceivedQuantityX(batch_list: List<DispatchBatchDetails>, position: Int) {
        // Find the MatReceivedItem for the corresponding OrderItem
        //val matRcvItem = matReceivedList.find { it.batch_no == item.batch_no }
        val matRcvItem = matReceivedList[position]

        // Update the quantity for the found MatReceivedItem
       // matRcvItem?.batch_list = value ?: 0
        matRcvItem?.batch_list = batch_list

        // Call the callback function with the updated list
        onQuantityChange(matReceivedList)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setStatusWiseQuantity(
        binding: MaterialreceivedItemLayoutBinding,
        status: String,
        approvedQuantity: Int
    ) {

        when (status) {
            "0" -> {
                //textview.text = "Pending"

                binding.requestLayout.isVisible = true
                binding.approvedLayout.isVisible = false
                binding.receivedLayout.isVisible = false
                binding.quantityContainer.isVisible = false

            }

            "1" -> {
                //textview.text = "Approved"
                //holder.binding.received.setOnClickListener
                binding.requestLayout.isVisible = true
                //binding.approvedLayout.isVisible = true
                binding.approvedLayout.isVisible = false
                binding.receivedLayout.isVisible = false
                binding.quantityContainer.isVisible = false

                if(approvedQuantity==0){
                    binding.approvedLayout.isVisible = false
                    binding.receivedLayout.isVisible = false
                    binding.notapprovedLayout.isVisible = true
                }


            }

            "4" -> {
                //textview.text = "Dispatched"

                if(isBeforeDispatchDate(dispatch_date!!)){
                    binding.requestLayout.isVisible = true
                    binding.approvedLayout.isVisible = true
                    binding.receivedLayout.isVisible = false
                    binding.quantityContainer.isVisible = true

                    binding.addlayout.isVisible = false
                }else{
                    binding.requestLayout.isVisible = true
                    binding.approvedLayout.isVisible = true
                    binding.receivedLayout.isVisible = false
                    binding.quantityContainer.isVisible = true

                    binding.addlayout.isVisible = true
                }


                if(approvedQuantity==0){
                    binding.approvedLayout.isVisible = false
                    binding.receivedLayout.isVisible = false
                    binding.quantityContainer.isVisible = false
                    binding.addlayout.isVisible = false
                    binding.notapprovedLayout.isVisible = true
                }



            }

            "2" -> {
                //textview.text = "Cancelled"
                binding.requestLayout.isVisible = true
                binding.approvedLayout.isVisible = false
                binding.receivedLayout.isVisible = false
                binding.quantityContainer.isVisible = false

            }

            "3" -> {
                //textview.text = "Received"
                binding.requestLayout.isVisible = true
                binding.approvedLayout.isVisible = true
                binding.receivedLayout.isVisible = true
                binding.quantityContainer.isVisible = false

                if(approvedQuantity==0){
                    binding.approvedLayout.isVisible = false
                    binding.receivedLayout.isVisible = false
                    binding.quantityContainer.isVisible = false
                    binding.addlayout.isVisible = false
                    binding.notapprovedLayout.isVisible = true
                }

            }

            else -> {
                // textview.text = "."
                binding.requestLayout.isVisible = false
                binding.approvedLayout.isVisible = false
                binding.receivedLayout.isVisible = false
                binding.quantityContainer.isVisible = false

            }
        }

    }


    override fun getItemCount(): Int {
        return pastReqDetailsList.order_items.size
    }


    private fun cartControlEditText(binding: MaterialreceivedItemLayoutBinding, item: OrderItem) {

        var oldnum = 0

        binding.mlEdit.addTextChangedListener(object : TextWatcher {

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

                    if (enteredValue <= item.dispatch_qty.toInt()) {
                        updateReceivedQuantity(item, enteredValue)
                    } else {
                        Toast.makeText(
                            context,
                            "The received quantity cannot be more than the dispatch quantity.",
                            Toast.LENGTH_SHORT
                        ).show()

                        binding.mlEdit.text = Editable.Factory.getInstance().newEditable(oldnum.toString())
                        // Set the cursor to the last position
                        binding.mlEdit.setSelection(binding.mlEdit.text.length)
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



    private fun cartControl(binding: MaterialreceivedItemLayoutBinding, item: OrderItem) {

        binding.cartPlusImg.setOnClickListener {
            var value = binding.cartProductQuantity.text.toString().toInt()
            binding.cartProductQuantity.text = (++value).toString()

            updateReceivedQuantity(item, value)


        }

        binding.cartMinusImg.setOnClickListener {
            var value = binding.cartProductQuantity.text.toString().toInt()

            if (value > 1) {
                binding.cartProductQuantity.text = (--value).toString()
            }
            updateReceivedQuantity(item, value)
        }

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

    private fun updateReceivedQuantity(item: OrderItem, value: Int) {
        // Find the MatReceivedItem for the corresponding OrderItem
        val matRcvItem = matReceivedList.find { it.id == item.id }

        // Update the quantity for the found MatReceivedItem
        matRcvItem?.received_quantity = value ?: 0

        // Call the callback function with the updated list
        onQuantityChange(matReceivedList)
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemId(position: Int) = position.toLong()

    override fun onViewRecycled(holder: MaterialReceivedAdapter.MaterialRcvdViewHolder) {
        super.onViewRecycled(holder)
        //holder.binding.quantEdit.removeTextChangedListener(holder.textWatcher)
    }


}