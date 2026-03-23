package com.retailone.pos.adapter

import NumberFormatter
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.StockSearchItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.CommonModel.StockRequsition.SearchResData
import com.retailone.pos.network.Constants

class StockSearchAdapter(
    private val stockSearchRes: List<SearchResData>, val context: Context,val onCartTotalChanged: (Double) -> Unit
) : RecyclerView.Adapter<StockSearchAdapter.StockSearchViewHolder>() {

    /* private var matrcvd = MaterialReceived()

     fun setMatRcvdData(matrcvd: MaterialReceived) {
         this.matrcvd = matrcvd
         notifyDataSetChanged()
     }*/

    private val sharedPrefHelper = SharedPrefHelper(context)
    private val localizationData = LocalizationHelper(context).getLocalizationData()


    class StockSearchViewHolder(val binding: StockSearchItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

            var textWatcher: TextWatcher? = null

        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSearchViewHolder {
        return StockSearchViewHolder(
            StockSearchItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: StockSearchViewHolder, position: Int) {

      //  holder.setIsRecyclable(false)

        // val productitem = stockSearchRes[position]
        val productitem = stockSearchRes[holder.adapterPosition]

      //  val unit_price = productitem.whole_sale_price.toDoubleOrNull() ?: 0.0

        holder.binding.quantEdit.removeTextChangedListener(holder.textWatcher)

        // Create a new TextWatcher for the current item
        holder.textWatcher = createTextWatcher(holder, productitem)
        holder.binding.quantEdit.addTextChangedListener(holder.textWatcher)



/*  holder.binding.quantEdit.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
          // Your implementation
      }

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
          try {
              val enteredValue = s.toString().toInt()
              if (enteredValue <= productitem.stock_quantity.toInt()) {
                  sharedPrefHelper.updateQuantity(
                      productitem.product_id,
                      productitem.distribution_pack_id,
                      enteredValue.toString()
                  )
              } else {
                  Toast.makeText(
                      context,
                      "Can't add Items more than Stocks- ${productitem.product_name}",
                      Toast.LENGTH_SHORT
                  ).show()
              }
          } catch (e: NumberFormatException) {
              // Handle the exception if needed
          }
      }

      override fun afterTextChanged(s: Editable?) {
          // Perform actions after the text has changed if needed
      }
  })*/

        holder.binding.itemName.text = productitem.product_name
        holder.binding.itemType.text = productitem.pack_product_description+ "  ( "+(productitem.uom?:"")+" )"
        holder.binding.itemUom.text = productitem.uom?:""
       /* if(productitem.stock_quantity <= productitem.reorder_level){
            //show
            holder.binding.itemUnit.isVisible
            holder.binding.itemUnit.text = productitem.stock_quantity
            holder.binding.itemPrice.isVisible

         //   holder.binding.itemPrice.text = productitem.product_name

        }else {
            holder.binding.itemUnit.isGone
            holder.binding.itemUnit.isGone
        }*/
        val stockQty = productitem.stock_quantity.toIntOrNull() ?: 0
        val reorderLevel = productitem.reorder_level.toIntOrNull() ?: 0

        if (stockQty <= reorderLevel) {
            // Show both fields
            holder.binding.itemUnit.isVisible = true
            holder.binding.itemPrice.isVisible = true

            holder.binding.itemUnit.text = "Stock Quantity: $stockQty"
            holder.binding.itemPrice.text = "Reorder Level: ${reorderLevel}"
        } else {
            holder.binding.itemUnit.isGone = true
            holder.binding.itemPrice.isGone = true
        }

//        val formattedPrice =
//            NumberFormatter().formatPrice(productitem.whole_sale_price, localizationData)
//        holder.binding.itemPrice.text = formattedPrice

       // if (productitem.stock_quantity.toInt() > 0) {
        if (true) {
            holder.binding.quantityContainer.isVisible = true

            //manage visibility of add button
            if (sharedPrefHelper.isProductAdded(
                    productitem.product_id,
                    productitem.distribution_pack_id
                )
            ) {
                holder.binding.addlayout.isVisible = false // Product is already added, hide the add button
                holder.binding.quantLayout.isVisible = true // Show the quantity EditText
                holder.binding.quantEdit.text = Editable.Factory.getInstance().newEditable(
                    sharedPrefHelper.getQuantity(
                        productitem.product_id,
                        productitem.distribution_pack_id
                    )
                )
            } else {
                holder.binding.addlayout.isVisible = true // Show the add button
                holder.binding.quantLayout.isVisible = false // Hide the quantity EditText
            }

            //holder.binding.addlayout.isVisible = true
            // holder.binding.quantLayout.isVisible = false

            holder.binding.itemUnit.text = "${productitem.stock_quantity} Units"
            holder.binding.itemUnit.setTextColor(Color.RED)

            holder.binding.addcart.setOnClickListener {
                val total = sharedPrefHelper.getTotalCartValue()
                onCartTotalChanged(total)
                sharedPrefHelper.saveSearchItem(productitem)
                //initial quantity ad
                sharedPrefHelper.updateQuantity(
                    productitem.product_id,
                    productitem.distribution_pack_id,
                    "1"
                )

                holder.binding.addlayout.isVisible = false
                // holder.binding.plusMinusLayout.isVisible = true
                //holder.binding.cartProductQuantity.text = "1"
                holder.binding.quantEdit.text = Editable.Factory.getInstance().newEditable("1")
                holder.binding.quantLayout.isVisible = true
                holder.binding.quantEdit.requestFocus()

            }

        } else {
            holder.binding.quantityContainer.isVisible = false
            holder.binding.itemUnit.text = "Out Of Stock"
            holder.binding.itemUnit.setTextColor(Color.parseColor("#FF0000"))
        }


       /*holder.binding.quantEdit.addTextChangedListener(object : TextWatcher {

           val product_item = stockSearchRes[holder.adapterPosition]

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val enteredValue = s.toString().toInt()
                    if (enteredValue <= product_item.stock_quantity.toInt()) {

                        sharedPrefHelper.updateQuantity(
                            product_item.product_id,
                            product_item.distribution_pack_id,
                            enteredValue.toString()
                        )
                    } else {

                        Toast.makeText(context, "Can't add Items more than Stocks- ${product_item.product_name}", Toast.LENGTH_SHORT).show()

                    }
                } catch (e: NumberFormatException) {
                    // Handle the exception if needed
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Perform actions after the text has changed if needed
            }
        })*/

        //cartControl(holder.binding, productitem)

        Glide.with(context)
            .load(Constants.IMAGE_URL + productitem.product_photo)
            .centerCrop() // Add center crop
            .placeholder(R.drawable.temp) // Add a placeholder drawable
            .error(R.drawable.temp) // Add an error drawable (if needed)
            .into(holder.binding.productimg)


    }

    private fun createTextWatcher(holder: StockSearchAdapter.StockSearchViewHolder, productitem: SearchResData): TextWatcher? {
        var oldnum = 0
        var isProgrammaticChange = false

        return object : TextWatcher {
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
                        Toast.makeText(context, "Product Quantity can't be zero", Toast.LENGTH_SHORT).show()

                        isProgrammaticChange = true
                        holder.binding.quantEdit.text = Editable.Factory.getInstance().newEditable("")

                        sharedPrefHelper.updateQuantity(
                            productitem.product_id,
                            productitem.distribution_pack_id,
                            "0"
                        )
                        val total = sharedPrefHelper.getTotalCartValue()
                        onCartTotalChanged(total)
                   // } else if (enteredValue <= productitem.stock_quantity.toInt()) {
                    } else if (enteredValue !=0) {
                        sharedPrefHelper.updateQuantity(
                            productitem.product_id,
                            productitem.distribution_pack_id,
                            enteredValue.toString()
                        )
                    } else {
                        Toast.makeText(context, "Can't add ${productitem.product_name} more than Stocks", Toast.LENGTH_SHORT).show()

                        isProgrammaticChange = true
                        if (oldnum > 0) {
                            holder.binding.quantEdit.text = Editable.Factory.getInstance().newEditable(oldnum.toString())
                        } else {
                            holder.binding.quantEdit.text = Editable.Factory.getInstance().newEditable("")
                        }
                        holder.binding.quantEdit.setSelection(holder.binding.quantEdit.text.length)
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
        }
    }


    /* private fun createTextWatcher(holder: StockSearchAdapter.StockSearchViewHolder, productitem: SearchResData): TextWatcher? {
        var oldnum = 0
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                try {
                    oldnum = s.toString().toInt()
                } catch (e: NumberFormatException) {
                    // Handle the exception if needed
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val enteredValue = s.toString().toInt()
                    if (enteredValue <= productitem.stock_quantity.toInt()) {
                        sharedPrefHelper.updateQuantity(
                            productitem.product_id,
                            productitem.distribution_pack_id,
                            enteredValue.toString()
                        )
                    } else {
                        Toast.makeText(context, "Can't add ${productitem.product_name} more than Stocks", Toast.LENGTH_SHORT).show()
                        holder.binding.quantEdit.text =
                            Editable.Factory.getInstance().newEditable(oldnum.toString())
                        // Set the cursor to the last position
                        holder.binding.quantEdit.setSelection(holder.binding.quantEdit.text.length)
                        holder.binding.quantEdit.clearFocus()

                        // binding.addlayout.isVisible = false // Hide the add button
                    }
                } catch (e: NumberFormatException) {
                    // Handle the exception if needed
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Perform actions after the text has changed if needed
            }

        }


    }
*/
    fun syncInputs() {
        // This will force a re-bind to update all quantities from visible EditTexts
        notifyDataSetChanged()
    }

    private fun cartControl(binding: StockSearchItemLayoutBinding, productitem: SearchResData) {
        var oldnum = 0

        binding.quantEdit.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                try {
                    oldnum = s.toString().toInt()
                } catch (e: NumberFormatException) {
                    // Handle the exception if needed
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val enteredValue = s.toString().toInt()
                    if (enteredValue <= productitem.stock_quantity.toInt()) {

                        sharedPrefHelper.updateQuantity(
                            productitem.product_id,
                            productitem.distribution_pack_id,
                            enteredValue.toString()
                        )
                    } else {

                        Toast.makeText(context, "Can't add Items more than Stocks", Toast.LENGTH_SHORT).show()

                        binding.quantEdit.text =
                            Editable.Factory.getInstance().newEditable(oldnum.toString())
                        // Set the cursor to the last position
                        binding.quantEdit.setSelection(binding.quantEdit.text.length)
                        // binding.quantEdit.clearFocus()

                        // binding.addlayout.isVisible = false // Hide the add button

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
    /*
        private fun cartControl(binding: StockSearchItemLayoutBinding, productitem: SearchResData) {

            binding.cartPlusImg.setOnClickListener {
                var value = binding.cartProductQuantity.text.toString().toInt()

                if(value < productitem.stock_quantity.toInt()){
                    var newquantity = ++value
                    binding.cartProductQuantity.text = (newquantity).toString()
                    sharedPrefHelper.updateQuantity(productitem.product_id,productitem.distribution_pack_id,newquantity.toString())
                }else{
                    Toast.makeText(context,"Can't Add more item,Limit exceed",Toast.LENGTH_SHORT).show()
                }
            }

            binding.cartMinusImg.setOnClickListener {
                var value = binding.cartProductQuantity.text.toString().toInt()

                if (value>1){
                    var newquantity = --value

                    binding.cartProductQuantity.text = (newquantity).toString()
                    sharedPrefHelper.updateQuantity(productitem.product_id,
                        productitem.distribution_pack_id,(newquantity).toString())
                }else if(value ==1){
                    binding.addlayout.isVisible = true
                    binding.plusMinusLayout.isVisible = false
                    sharedPrefHelper.removeItem(productitem.product_id,productitem.distribution_pack_id)

                }
            }

        }
    */


    override fun getItemCount(): Int {
        return stockSearchRes.size
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemId(position: Int) = position.toLong()

    override fun onViewRecycled(holder: StockSearchViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.quantEdit.removeTextChangedListener(holder.textWatcher)
    }
}



