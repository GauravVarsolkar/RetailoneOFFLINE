package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.retailone.pos.databinding.ReturnItemLayoutBinding
import com.retailone.pos.interfaces.OnReturnQuantityChangeListener
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvItem
import com.retailone.pos.models.ReturnSalesItemModel.BatchReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData
import com.retailone.pos.models.ReturnSalesItemModel.SalesItem
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.DispatchBatchDetails
import com.retailone.pos.utils.FunUtils

class ReturnSalesItemAdapter(
    private val returnitem: List<ReturnItemData>,
    val context: Context,
    private val onReturnQuantityChangeListener: OnReturnQuantityChangeListener,
    val onBatchChange: (List<BatchReturnItem>) -> Unit,
    private val returnReasonName: String = "Not Given",  // ✅ ADD THIS
) : RecyclerView.Adapter<ReturnSalesItemAdapter.StockSearchViewHolder>() {

    /* private var matrcvd = MaterialReceived()

     fun setMatRcvdData(matrcvd: MaterialReceived) {
         this.matrcvd = matrcvd
         notifyDataSetChanged()
     }*/
    private val batchAdapters = mutableMapOf<Int, ReturnSalesItemBatchAdapter>()

    private val sharedPrefHelper = SharedPrefHelper(context)
    val localizationData = LocalizationHelper(context).getLocalizationData()

    private val returnbatchItemList = mutableListOf<BatchReturnItem>()
    private val batchAdaptersMap = mutableMapOf<Int, ReturnSalesItemBatchAdapter>()
//    init {
//        // Initialize matReceivedList with default values
//        returnbatchItemList.addAll(returnitem.map { MatRcvItem(it.id, 0,
//            emptyList()
//        ) })
//    }



    class StockSearchViewHolder(val binding: ReturnItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSearchViewHolder {
        return StockSearchViewHolder(
            ReturnItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: StockSearchViewHolder, position: Int) {
        //  val isInvoiceAlreadyReturned = returnitem[0].total_refunded_amount > 0

        val productitem = returnitem[0].salesItems[position]
        val isInvoiceAlreadyReturned = returnitem[0].total_refunded_amount > 0
        val isLooseOil = FunUtils.isLooseOil(productitem.product.id, productitem.distribution_pack.product_description)

        holder.binding.itemName.text = productitem.product.product_name
        holder.binding.itemDesc.text = productitem.distribution_pack.product_description
        holder.binding.itemUnit.text = "Purchase -  " + FunUtils.DtoString(productitem.quantity)

        val formattedPrice = NumberFormatter().formatPrice(productitem.retail_price.toString() ?: "-", localizationData)
        holder.binding.itemPrice.text = "Rate -   " + formattedPrice

        val returnQty = productitem.return_quantity

        val refundPrice = productitem.return_quantity * productitem.retail_price
        val formattedRefundPrice = NumberFormatter().formatPrice(refundPrice.toString() ?: "-", localizationData)
        holder.binding.refundPrice.text = "Refund -   " + formattedRefundPrice

        val position = holder.adapterPosition

        // ✅ If invoice already returned
        if (isInvoiceAlreadyReturned) {
            holder.binding.addcart.isEnabled = false
            holder.binding.quantEdit.isEnabled = false
            holder.binding.addcart.isVisible = false
            holder.binding.quantLayout.isVisible = false
            // holder.binding.returnreason.isVisible = true
            holder.binding.batchRcv.isVisible = true
            holder.binding.batchLayouts.isVisible = true
            holder.binding.quantEdit.setText(returnQty.toString())

            productitem.batches?.let {
                holder.binding.batchRcv.apply {
                    layoutManager = LinearLayoutManager(holder.itemView.context, RecyclerView.VERTICAL, false)
                    setHasFixedSize(true)
                    adapter = ReturnSalesItemBatchAdapter(
                        returnitem,
                        context,
                        productitem.batches,
                        isInvoiceAlreadyReturned,
                        returnReasonName
                    ) { updatedBatches ->
                        updateReceivedQuantityX(updatedBatches, position)
                    }
                    // it,
                    // onBatchChange = {} // Read-only, so no update needed

                }
            }

        } else {
            // ✅ Allow normal editing
            holder.binding.addcart.setOnClickListener {
                if (!isLooseOil) {
                    holder.binding.addlayout.isVisible = false
                    // holder.binding.returnreason.isVisible = false
                    productitem.batches?.let {
                        holder.binding.batchRcv.isVisible = true
                        holder.binding.batchLayout.isVisible = true

                        holder.binding.batchRcv.apply {
                            layoutManager = LinearLayoutManager(holder.itemView.context, RecyclerView.VERTICAL, false)
                            setHasFixedSize(true)
                            adapter = ReturnSalesItemBatchAdapter(
                                returnitem,
                                context,

                                productitem.batches,
                                isInvoiceAlreadyReturned,
                                "Not Given",  // Pass the return reason name here
                            ) { updatedBatches ->
                               updateReceivedQuantityX(updatedBatches, position)

                            }
                             /*it,
                             onBatchChange = {} */// Read-only, so no update needed

                        }
                    }
                } else {
                    Toast.makeText(context, "Can't Return Loose Oils", Toast.LENGTH_SHORT).show()
                }
            }

            // Quantity editing only for non-returned invoices
            cartControl(holder.binding, productitem, position)
        }
    }



    override fun getItemCount(): Int {
        return returnitem[0].salesItems.size
    }


    override fun getItemViewType(position: Int) = position

    override fun getItemId(position: Int) = position.toLong()

    override fun onViewRecycled(holder: ReturnSalesItemAdapter.StockSearchViewHolder) {
        super.onViewRecycled(holder)
        //holder.binding.quantEdit.removeTextChangedListener(holder.textWatcher)
    }



  private fun updateReceivedQuantityX(batch_list: List<BatchReturnItem>, position: Int) {
      for (item in batch_list) {
          val batchKey = item.batch?.trim()?.lowercase() ?: ""
          val existingItemIndex = returnbatchItemList.indexOfFirst {
              it.sales_item_id == item.sales_item_id &&
                      (it.batch?.trim()?.lowercase() ?: "") == batchKey
          }

          if (item.batch_return_quantity > 0) {
              if (existingItemIndex != -1) {
                  returnbatchItemList[existingItemIndex] = item
              } else {
                  returnbatchItemList.add(item)
              }
          } else {
              if (existingItemIndex != -1) {
                  returnbatchItemList.removeAt(existingItemIndex)
                  Log.d("ReturnAdapter", "Removed from returnbatchItemList: ${item.batch}")
              }
          }
      }

      // Ensure only items with non-zero quantity are passed
      val filteredList = returnbatchItemList.filter { it.batch_return_quantity > 0 }
      Log.d("ReturnAdapter", "Final returnbatchItemList: ${filteredList.map { it.batch to it.batch_return_quantity }}")

      // ✅ This filtered list will only include valid return entries
      onBatchChange(filteredList)
  }




    private fun cartControl(
        binding: ReturnItemLayoutBinding,
        productitem: SalesItem,
        position: Int
    ) {
        var oldnum = 0
        var isProgrammaticChange = false

        binding.quantEdit.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isProgrammaticChange) {
                    try {
                        oldnum = s.toString().toInt()
                    } catch (e: NumberFormatException) {
                        // Handle the exception if needed
                    }
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticChange) return

                try {
                    val enteredValue = s.toString().toIntOrNull() ?: 0

                    if (enteredValue == 0) {
                        isProgrammaticChange = true
                        binding.quantEdit.text = Editable.Factory.getInstance().newEditable("")
                        Toast.makeText(context, "Please add return quantity", Toast.LENGTH_SHORT).show()
                        onReturnQuantityChangeListener.onReturnQuantityChange(position, 0)

                        val refundPrice =enteredValue * productitem.retail_price
                        val formattedRefundPrice = NumberFormatter().formatPrice(refundPrice.toString()?:"-",localizationData)
                        binding.refundPrice.text = "Refund -   "+formattedRefundPrice

                    } else if (enteredValue.toString().startsWith("00")) {
                        isProgrammaticChange = true
                        binding.quantEdit.text = Editable.Factory.getInstance().newEditable("0")
                        Toast.makeText(context, "Leading zeros replaced with 0", Toast.LENGTH_SHORT).show()
                        onReturnQuantityChangeListener.onReturnQuantityChange(position, 0)

                        val refundPrice =enteredValue * productitem.retail_price
                        val formattedRefundPrice = NumberFormatter().formatPrice(refundPrice.toString()?:"-",localizationData)
                        binding.refundPrice.text = "Refund -   "+formattedRefundPrice

                    } else if (enteredValue <= productitem.quantity.toInt()) {
                        onReturnQuantityChangeListener.onReturnQuantityChange(position, enteredValue)

                        val refundPrice =enteredValue * productitem.retail_price
                        val formattedRefundPrice = NumberFormatter().formatPrice(refundPrice.toString()?:"-",localizationData)
                        binding.refundPrice.text = "Refund -   "+formattedRefundPrice

                    } else {
                        Toast.makeText(context, "Can't return items more than purchase quantity", Toast.LENGTH_SHORT).show()
                        isProgrammaticChange = true

                        //binding.quantEdit.text = Editable.Factory.getInstance().newEditable(oldnum.toString())

                        if(oldnum>0){
                            binding.quantEdit.text = Editable.Factory.getInstance().newEditable(oldnum.toString())
                        }else{
                            binding.quantEdit.text = Editable.Factory.getInstance().newEditable("")
                        }
                        binding.quantEdit.setSelection(binding.quantEdit.text.length)

                    }
                } catch (e: NumberFormatException) {
                    // Handle the exception if needed
                } finally {
                    isProgrammaticChange = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Perform actions after the text has changed if needed
            }
        })
    }



//    private fun cartControl(
//        binding: ReturnItemLayoutBinding,
//        productitem: SalesItem,
//        position: Int
//    ) {
//        var oldnum = 0
//        binding.quantEdit.addTextChangedListener(object : TextWatcher {
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                try {
//                    oldnum = s.toString().toInt()
//                } catch (e: NumberFormatException) {
//                    // Handle the exception if needed
//                }
//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                try {
//                    val enteredValue = s.toString().toIntOrNull() ?: 0
//
//                    //val enteredValue = s.toString().toInt()
//                    if (enteredValue.toString().startsWith("00")) {
//                        binding.quantEdit.text = Editable.Factory.getInstance().newEditable("0")
//                        Toast.makeText(context, "Leading zeros replaced with 0", Toast.LENGTH_SHORT).show()
//
//                        onReturnQuantityChangeListener.onReturnQuantityChange(position, 0)
//
//                        val refundPrice =productitem.return_quantity * productitem.retail_price
//                        val formattedRefundPrice = NumberFormatter().formatPrice(refundPrice.toString()?:"-",localizationData)
//                        binding.refundPrice.text = "Refund -   "+formattedRefundPrice
//
//                    } else if (enteredValue == 0) {
//                        //binding.quantEdit.text = Editable.Factory.getInstance().newEditable("")
//
//
//                        // Check if the entered value is exactly 0
//                       // Toast.makeText(context, "Product Quantity can't be zero", Toast.LENGTH_SHORT).show()
//                        // Uncomment the line below if you want to clear the input when it's exactly 0
//                        // binding.quantEdit.text = Editable.Factory.getInstance().newEditable("")
//
//                        onReturnQuantityChangeListener.onReturnQuantityChange(position, 0)
//
//
//                        val refundPrice =productitem.return_quantity * productitem.retail_price
//                        val formattedRefundPrice = NumberFormatter().formatPrice(refundPrice.toString()?:"-",localizationData)
//                        binding.refundPrice.text = "Refund -   "+formattedRefundPrice
//
//
//                    } else if(enteredValue <= productitem.quantity.toInt()) {
//
//                        /// notifyDataSetChanged()
//                        onReturnQuantityChangeListener.onReturnQuantityChange(position, enteredValue)
//
//                        val refundPrice =productitem.return_quantity * productitem.retail_price
//                        val formattedRefundPrice = NumberFormatter().formatPrice(refundPrice.toString()?:"-",localizationData)
//                        binding.refundPrice.text = "Refund -   "+formattedRefundPrice
//
//                        /*sharedPrefHelper.updateQuantity(
//                            productitem.product_id,
//                            productitem.distribution_pack_id,
//                            enteredValue.toString()
//                        )*/
//                    } else {
//                        Toast.makeText(context, "Can't return items more than purchase quantity", Toast.LENGTH_SHORT).show()
//                        binding.quantEdit.text = Editable.Factory.getInstance().newEditable(oldnum.toString())
//                        // Set the cursor to the last position
//                        binding.quantEdit.setSelection(binding.quantEdit.text.length)
//                        // binding.quantEdit.clearFocus()
//                    }
//                } catch (e: NumberFormatException) {
//                    // Handle the exception if needed
//                }
//            }
//
//            override fun afterTextChanged(s: Editable?) {
//                // Perform actions after the text has changed if needed
//            }
//        })
//
//    }




}

