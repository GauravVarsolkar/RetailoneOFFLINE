package com.retailone.pos.adapter


import NumberFormatter
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.databinding.StockSubmitItemLayoutBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.CommonModel.StockRequsition.SearchResData
import com.retailone.pos.network.Constants

class StockSubmitAdapter(
    private val stockSearchRes: MutableList<SearchResData>, val context: Context
)  : RecyclerView.Adapter<StockSubmitAdapter.StockSubmitViewHolder> (){

   // private var matrcvd = MaterialReceived()

   /* fun setMatRcvdData(matrcvd : MaterialReceived){
        this.matrcvd = matrcvd
        notifyDataSetChanged()
    }*/

    private val sharedPrefHelper = SharedPrefHelper(context)
    private val localizationData = LocalizationHelper(context).getLocalizationData()


    class StockSubmitViewHolder (val binding:StockSubmitItemLayoutBinding) : RecyclerView.ViewHolder(binding.root){

        var textWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSubmitViewHolder {
        return StockSubmitViewHolder(StockSubmitItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: StockSubmitViewHolder, position: Int) {

        val item = stockSearchRes[position]


        holder.binding.quantEdit.removeTextChangedListener(holder.textWatcher)
        // Create a new TextWatcher for the current item
        holder.textWatcher = createTextWatcher(holder, item)
        holder.binding.quantEdit.addTextChangedListener(holder.textWatcher)


        holder.binding.itemName.text = item.product_name
        holder.binding.itemType.text = item.pack_product_description+ "  ( "+(item.uom?:"")+" )"
        holder.binding.itemUom.text = item.uom?:""

//        val formattedPrice = NumberFormatter().formatPrice(item.whole_sale_price,localizationData)
//        holder.binding.itemPrice.text = formattedPrice

        /*  val type = matrcvd.materiallist[position].type

          if(type == "num"){
              holder.binding.plusMinusLayout.visibility = View.VISIBLE
              holder.binding.mllayout.visibility = View.GONE
          }else if(type == "ml"){
              holder.binding.plusMinusLayout.visibility = View.GONE
              holder.binding.mllayout.visibility = View.VISIBLE
     }*/

        // cartControl(holder.binding)

        holder.binding.itemUnit.text = "${item.stock_quantity} Units"
        holder.binding.itemUnit.setTextColor(Color.parseColor("#008000"))

        holder.binding.quantEdit.text = Editable.Factory.getInstance().newEditable( item.cart_quantity)

        holder.binding.deletelayout.setOnClickListener {
            sharedPrefHelper.removeItem(item.product_id,item.distribution_pack_id)
            removeItemView(item)
        }

        Glide.with(context)
            .load(Constants.IMAGE_URL + item.product_photo)
            .centerCrop() // Add center crop
            .placeholder(R.drawable.temp) // Add a placeholder drawable
            .error(R.drawable.temp) // Add an error drawable (if needed)
            .into(holder.binding.productimg)


        //cartControl(holder.binding,item)
    }


    override fun getItemCount(): Int {
        return stockSearchRes.size
    }

    private fun removeItemView(item: SearchResData) {
        val position = stockSearchRes.indexOf(item)
        if (position != -1) {
            stockSearchRes.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemId(position: Int) = position.toLong()

    override fun onViewRecycled(holder: StockSubmitAdapter.StockSubmitViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.quantEdit.removeTextChangedListener(holder.textWatcher)
    }


    private fun createTextWatcher(holder: StockSubmitViewHolder, productitem: SearchResData): TextWatcher? {
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
                    //} else if (enteredValue <= productitem.stock_quantity.toInt()) {
                    } else if (enteredValue != 0) {
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


    /*
        private fun createTextWatcher(holder:StockSubmitViewHolder, productitem: SearchResData): TextWatcher? {
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
                        val enteredValue = s.toString().toIntOrNull() ?: 0

                        //val enteredValue = s.toString().toInt()
                        if (enteredValue <= productitem.stock_quantity.toInt()) {
                            sharedPrefHelper.updateQuantity(
                                productitem.product_id,
                                productitem.distribution_pack_id,
                                enteredValue.toString()
                            )
                        } else {
                            Toast.makeText(context, "Can't add ${productitem.product_name} more than Stocks", Toast.LENGTH_SHORT).show()
                            holder.binding.quantEdit.text = Editable.Factory.getInstance().newEditable(oldnum.toString())
                            // Set the cursor to the last position
                            holder.binding.quantEdit.setSelection(holder.binding.quantEdit.text.length)
                            holder.binding.quantEdit.clearFocus()
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

    /*
        private fun cartControl(binding: StockSubmitItemLayoutBinding, productitem: SearchResData) {
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
                       val enteredValue = s.toString().toIntOrNull() ?: 0

                        //val enteredValue = s.toString().toInt()
                        if (enteredValue <= productitem.stock_quantity.toInt()) {
                            sharedPrefHelper.updateQuantity(
                                productitem.product_id,
                                productitem.distribution_pack_id,
                                enteredValue.toString()
                            )
                        } else {
                            Toast.makeText(context, "Can't add Items more than Stocks", Toast.LENGTH_SHORT).show()
                            binding.quantEdit.text = Editable.Factory.getInstance().newEditable(oldnum.toString())
                            // Set the cursor to the last position
                            binding.quantEdit.setSelection(binding.quantEdit.text.length)
                            // binding.quantEdit.clearFocus()
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
    */



    /*
        private fun cartControl(binding: StockSubmitItemLayoutBinding, productitem: SearchResData) {

            binding.cartPlusImg.setOnClickListener {
                var value = binding.cartProductQuantity.text.toString().toInt()

                if(value < productitem.stock_quantity.toInt()){
                    var newquantity = ++value
                    binding.cartProductQuantity.text = (newquantity).toString()
                    sharedPrefHelper.updateQuantity(productitem.product_id,productitem.distribution_pack_id,newquantity.toString())
                }else{
                    Toast.makeText(context,"Can't Add more item,Limit exceed", Toast.LENGTH_SHORT).show()
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
                    binding.plusMinusLayout.isVisible = false
                    sharedPrefHelper.removeItem(productitem.product_id,productitem.distribution_pack_id)

                    removeItemView(productitem)

                }
            }


        }
    */


}