package com.retailone.pos.ui.Activity.DashboardActivity

import NumberFormatter
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.retailone.pos.R
import com.retailone.pos.adapter.ReturnReasonAdapter
import com.retailone.pos.adapter.ReturnSalesItemAdapter
import com.retailone.pos.databinding.ActivityReturnSaleBinding
import com.retailone.pos.databinding.ActivitySaleCorrectionBinding
import com.retailone.pos.interfaces.OnReturnQuantityChangeListener
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.ReturnSalesItemModel.BatchReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnSaleReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnedItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnSaleRes
import com.retailone.pos.models.ReturnSalesItemModel.SalesItem
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData
import com.retailone.pos.ui.Activity.MPOSDashboardActivity
import com.retailone.pos.utils.PrinterUtil
import com.retailone.pos.viewmodels.DashboardViewodel.ReturnSalesDetailsViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SaleCorrectionActivity : AppCompatActivity(), OnReturnQuantityChangeListener {

    lateinit var  binding : ActivitySaleCorrectionBinding
    lateinit var returnsale_viewmodel: ReturnSalesDetailsViewmodel
    lateinit var  returnSalesItemAdapter : ReturnSalesItemAdapter
    var returnItemList = mutableListOf<SalesItem>()
    var returnReasonList: MutableList<ReturnReasonData> = mutableListOf()

    lateinit var returnItemData: ReturnItemData
    var reasonid = -1

    var storeid = 0
    var store_manager_id = "0"
    lateinit var localizationData: LocalizationData

    private var printerUtil: PrinterUtil? = null

    private var returnbatchItemList = mutableListOf<BatchReturnItem>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaleCorrectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.relativeLayout.isVisible = false
        binding.relativeLayout2.isVisible = false



        returnsale_viewmodel = ViewModelProvider(this)[ReturnSalesDetailsViewmodel::class.java]
        localizationData = LocalizationHelper(this).getLocalizationData()

        val loginSession= LoginSession.getInstance(this)
        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first().toInt()
            store_manager_id = loginSession.getStoreManagerID().first().toString()
            //default search
            // returnsale_viewmodel.callReturnSalesDetailsApi(ReturnItemReq(invoice_id = "#241858938"),this@SaleCorrectionActivity)

        }

        binding.addcart.setOnClickListener {
            returnbatchItemList.clear()
            // clear data

            if(binding.searchBar.query.toString().trim()==""){
                showMessage("Enter a valid Invoice ID")
            }else{
                returnsale_viewmodel.callReturnSalesDetailsApi(ReturnItemReq(invoice_id = binding.searchBar.query.toString().trim()),this@SaleCorrectionActivity)

            }
        }

        binding.addproductLayout.setOnClickListener {
            showMessage("Enter Invoice ID and Search")
        }

        returnsale_viewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress
            if(it.isMessage)
                showMessage(it.message)
        }

        printerUtil = PrinterUtil(this)



        enableBackButton()

        preparePositemRCV()
        returnsale_viewmodel.callSaleReturnReasonApi(this)


        returnsale_viewmodel.returnitem_liveData.observe(this){
            if(it.data.isNotEmpty()){


                if(it.data[0].total_refunded_amount>0){
                    showMessage("This invoice has already been returned and cannot be returned again.")

                    binding.positemRcv.isVisible = false
                    binding.addproductLayout.isVisible = true
                    binding.reasonLayout.isVisible = false

                    binding.relativeLayout.isVisible = false
                    binding.relativeLayout2.isVisible = false
                } else{
                    returnItemData=it.data[0]

                    returnItemList = it.data[0].salesItems?.toMutableList() ?: mutableListOf()




                    returnSalesItemAdapter = ReturnSalesItemAdapter(
                        returnitem = it.data,
                        context = this@SaleCorrectionActivity,
                        onReturnQuantityChangeListener = this,
                        returnReasonName = "Not Given",  // ✅ Sale correction doesn't need reason display
                        onBatchChange = {
                            Log.d("rtn", it.toString())
                            returnbatchItemList = it.toMutableList()
                        }
                    )


                    binding.positemRcv.adapter = returnSalesItemAdapter
                    binding.positemRcv.isVisible = true
                    binding.addproductLayout.isVisible = false
                    binding.reasonLayout.isVisible = true

                    binding.relativeLayout.isVisible = true
                    binding.relativeLayout2.isVisible = true
                }

            }else{
                showMessage("No Invoice Found")

                binding.positemRcv.isVisible = false
                binding.addproductLayout.isVisible = true
                binding.reasonLayout.isVisible = false

                binding.relativeLayout.isVisible = false
                binding.relativeLayout2.isVisible = false

            }
        }


        returnsale_viewmodel.returnsalesubmit_liveData.observe(this){

            if(it.status==1){
                //showMessage(it.message)
                showSucessDialog(it.message,it)
            }else{
                showMessage(it.message)
            }
        }

        returnsale_viewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }

        setToolbarImage()

        returnsale_viewmodel.salesreturnreason_liveData.observe(this) {
            //val default_data = ExpenseCategoryData("Others", "", 0, 1, "")
            // val categorylist = categorydata?.data?.toMutableList()
            // categoryList.clear()
            // categoryList = it?.data?.toMutableList() ?: mutableListOf()
            // categoryList.add(default_data)

            returnReasonList = it.data.toMutableList()

            binding.reasonInput.setAdapter(ReturnReasonAdapter(this, 0, returnReasonList))

        }

        binding.reasonInput.setOnClickListener {
            if(returnReasonList.isEmpty()){
                showMessage("Return reason not found, try after sometime")
            }
        }

        binding.reasonInput.setOnItemClickListener { parent, view, position, id ->

            binding.reasonInput.setText(returnReasonList[position].reason_name, false)

            reasonid = returnReasonList[position].id
            // vendorRegisterViewModel.getNewDistrictData(it.states.get(position).id)
            // binding.districtEdit.setText("",false)
            //binding.othCategoryLayout.isVisible = categoryList[position].category_name=="Others"

//            if (categoryList[position].category_name == "Others") {
//                binding.othCategoryLayout.isVisible = true
//                catType = "OTH"
//                catValue = "" // value from edit text
//            } else {
//                binding.othCategoryLayout.isVisible = false
//                catType = "NORMAL"
//                catValue = categoryList[position].category_name
//
//            }
        }


        binding.nextlayout.setOnClickListener {

//            Log.d("sdf",returnItemList.toString())
//
//            if (returnItemList.isNotEmpty()) {
//                if (!returnItemList.all { it.return_quantity.toInt() == 0 }) {
//                    // val intent = Intent(this@PointOfSaleActivity,PointofSaleDetailsActivity::class.java)
//                    //  intent.putExtra("saleitem",addtocartres)
//                    // startActivity(intent)
//                   callReturnAPI()
//                }else{
//                    showMessage("You haven't Return anything")
//                }
//            }else{
//                showMessage("No data to return")
//            }


            if(returnbatchItemList.isNotEmpty()){
                callReturnAPI(returnbatchItemList)
            }else{
                showMessage("You haven't Return anything")
            }

        }




    }

    private fun callReturnAPI(returnbatchItemList: MutableList<BatchReturnItem>) {

        //  if(posItemList.isNotEmpty()){
        // val cartitemlist = mutableListOf<ReturnedItem>()
        val cartitemlist = mutableListOf<ReturnedItem>()

        returnbatchItemList.forEach {
            if ((it.batch_return_quantity ?: 0) != 0) {
                cartitemlist.add(
                    ReturnedItem(id = it.sales_item_id ?: 0,
                        return_quantity = it.batch_return_quantity ?: 0,
                    )
                )
            }
        }

        if(reasonid != -1){
            val return_data = ReturnSaleReq(store_id = storeid, store_manager_id = store_manager_id.toInt(),reason_id = reasonid, sales_id = returnItemData.id, returned_items =cartitemlist.toList()
               /* return_date_time = getReturnDateTime()*/)

            //pos_viewmodel.callAddtoCartPosApi(cart_data,this@PointOfSaleActivity)

            //showMessage("bef${posItemList.size} at${cartitemlist.size}")

            returnsale_viewmodel.callReturnSalesSubmitApi(return_data,this@SaleCorrectionActivity)

            val gson = Gson()
            val json = gson.toJson(return_data)
            Log.d("req",json)
        }else{
            showMessage("please select any reason for return")
        }



    }


    private fun getReturnDateTime():String {

        val zone = localizationData.timezone
        lateinit var timezone :String

        if (zone == "IST"){
            timezone = "Asia/Kolkata"
        }else if(zone == "CAT"){
            timezone = "Africa/Lusaka"
        }else{
            timezone = "Africa/Lusaka"
        }


//        val calendar = Calendar.getInstance()
//
//        val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
//        val currentDateTime = calendar.time
//        val formattedDateTime = dateFormat.format(currentDateTime)
//
//        return formattedDateTime



        val calendar = Calendar.getInstance()

        // Set the time zone to Zambia (Africa/Lusaka)
        val zambiaTimeZone = TimeZone.getTimeZone(timezone)
        calendar.timeZone = zambiaTimeZone

        val currentDateTime = calendar.time

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        dateFormat.timeZone = zambiaTimeZone

        val formattedDateTime = dateFormat.format(currentDateTime)

        return formattedDateTime

    }

    private fun setToolbarImage() {
        val organisation_data = OrganisationDetailsHelper(this).getOrganisationData()

        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.fabicon)
            .fitCenter() // Add center crop
            .placeholder(R.drawable.mlogo) // Add a placeholder drawable
            .error(R.drawable.mlogo) // Add an error drawable (if needed)
            .into(binding.image)
    }



    private fun preparePositemRCV() {

        binding.positemRcv.apply {
            layoutManager = LinearLayoutManager(this@SaleCorrectionActivity,
                RecyclerView.VERTICAL,false)
        }
    }

    private fun enableBackButton() {
        setSupportActionBar(binding.toolbar)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "New Activity"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.svg_back_arrow_white)
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@SaleCorrectionActivity, msg, Toast.LENGTH_SHORT).show()
    }


    override fun onReturnQuantityChange(position: Int, newQuantity: Int) {
        returnItemList[position].return_quantity= newQuantity
        returnItemList[position].refund_amount= newQuantity*returnItemList[position].retail_price

        // showMessage( newQuantity.toString())

        var refundTotal = 0.0

        returnItemList.forEach {
            if (it.refund_amount != 0.0) {
                refundTotal += it.refund_amount
            }
        }


        binding.rlPrice2.text =NumberFormatter().formatPrice(refundTotal.toString()?:"-",localizationData)

    }


    private fun showSucessDialog(msg: String, returnSaleRes: ReturnSaleRes) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.pos_sucess_dialog)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(false)

        val confirm = dialog.findViewById<MaterialButton>(R.id.prefer_confirm)
        val logoutMsg = dialog.findViewById<TextView>(R.id.logout_msg)
        val logoutImg = dialog.findViewById<ImageView>(R.id.dialog_logo)
        val print_receipt = dialog.findViewById<MaterialButton>(R.id.print_receipt)

        logoutMsg.text = msg
        logoutMsg.textSize = 16F
        //logoutImg.setImageResource(R.drawable.svg_off)
        //logoutImg.scaleType = ImageView.ScaleType.FIT_CENTER

        // printerUtil?.printReceipt(pos_sale_data)


        confirm.setOnClickListener {
            dialog.dismiss()

            val intent = Intent(this@SaleCorrectionActivity, MPOSDashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        print_receipt.setOnClickListener {

            printerUtil?.printReturnReceiptData(returnSaleRes)

        }
        dialog.show()

    }




    fun dismissKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        printerUtil?.registerBatteryReceiver()
    }

    override fun onPause() {
        super.onPause()
        printerUtil?.unregisterBatteryReceiver()
    }
}