package com.retailone.pos.ui.Activity.DashboardActivity

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
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
import com.retailone.pos.adapter.StockSubmitAdapter
import com.retailone.pos.databinding.ActivityStockRequisitionSubmitBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel.PurchaseItem
import com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel.SubmitStockRequest
import com.retailone.pos.viewmodels.DashboardViewodel.StockRequisitionSubmitViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class StockRequisitionSubmitActivity : AppCompatActivity() {
    lateinit var binding: ActivityStockRequisitionSubmitBinding
    lateinit var stockSubmitViewmodel: StockRequisitionSubmitViewmodel
    lateinit var stockSubmitAdapter: StockSubmitAdapter
    lateinit var sharedPrefHelper: SharedPrefHelper
    private var storeid = ""
    private var totalExposerstore = ""
    private var actualStoreExposure: Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockRequisitionSubmitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stockSubmitViewmodel = ViewModelProvider(this)[StockRequisitionSubmitViewmodel::class.java]
        sharedPrefHelper = SharedPrefHelper(this@StockRequisitionSubmitActivity)

        enableBackButton()

        prepareStockitemRCV()


        val loginSession = LoginSession.getInstance(this)

        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first()
        }

        val sharedPrefHelper = SharedPrefHelper(this)
       // totalExposerstore = sharedPrefHelper.getFinalExposure().toString()

      //  Log.d("Exposure", "Final Exposure Value from Session: $totalExposerstore")
        /* stockSubmitViewmodel.getStockSubmitData()
         stockSubmitViewmodel.stocksubmit_livedata.observe(this){
             stockSubmitAdapter.setMatRcvdData(it)
             }*/

        binding.barcodecard.setOnClickListener {

            val intent = Intent(this, StockRequsitionSearchActivity::class.java)
            startActivityForResult(intent, 100)

        }

        stockSubmitViewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            binding.nextlayout.isClickable = !it.isProgress
            if (it.isMessage)
                showMessage(it.message)
        }

        stockSubmitViewmodel.stockreq_submit_livedata.observe(this) {

            if (it.status == 1) {
                //showMessage(it.message)
                sharedPrefHelper.clearStockList()
                refreshAdapter()
                showSucessDialog(it.message)
            } else {
                showMessage("Something Went Wrong ,Try again")
            }
        }
        binding.nextlayout.setOnClickListener {
            val updatedList = sharedPrefHelper.getSearchResultsList().toMutableList()

            if (updatedList.isNotEmpty()) {
                if (!updatedList.any { it.cart_quantity.toInt() == 0 }) {
                    //val finalExposure = totalExposerstore.toDoubleOrNull() ?: 0.0
                    val finalExposure = calculateFinalExposure()

                    Log.d("submitActualAndStore",finalExposure.toString()+"="+"Actual"+actualStoreExposure)
                    if (finalExposure > actualStoreExposure) {
                       // Toast.makeText(this, "❌ Total exceeds actual store exposure!", Toast.LENGTH_LONG).show()
                        AlertDialog.Builder(this)
                            .setTitle("❌ Limit Exceeded")
                            .setMessage("Total exposure ₹$finalExposure exceeds allowed limit ₹$actualStoreExposure.")
                            .setPositiveButton("OK") {dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                        return@setOnClickListener
                    }

                    val purchseitem_list = mutableListOf<PurchaseItem>()
                    updatedList.forEach {
                        purchseitem_list.add(
                            PurchaseItem(
                                category_id = it.category_id.toInt(),
                                distribution_pack_id = it.distribution_pack_id,
                                product_id = it.product_id.toInt(),
                                requested_quantity = it.cart_quantity.toInt(),
                                whole_sale_price = "",
                                retail_price = ""
                            )
                        )
                    }

                    val submit_data = SubmitStockRequest(purchseitem_list, storeid)

                    stockSubmitViewmodel.callStockReqSubmitApi(submit_data, this@StockRequisitionSubmitActivity)

                    val gson = Gson()
                    val jsonString = gson.toJson(submit_data)
                    Log.d("SubmitPayload", jsonString)
                } else {
                    showMessage("Product Quantity can't be empty or zero")
                }
            } else {
                showMessage("Please add at least one product in cart")
            }
        }


        /*binding.nextlayout.setOnClickListener {

            val updatedList = sharedPrefHelper.getSearchResultsList().toMutableList()

                if (updatedList.isNotEmpty()) {

                    if (!updatedList.any { it.cart_quantity.toInt() == 0 }) {

                        val purchseitem_list = mutableListOf<PurchaseItem>()

                        updatedList.forEach {
                            purchseitem_list.add(
                                PurchaseItem(
                                    category_id = it.category_id.toInt(),
                                    distribution_pack_id = it.distribution_pack_id,
                                    product_id = it.product_id.toInt(),
                                    requested_quantity = it.cart_quantity.toInt(),
                                    //whole_sale_price = it.whole_sale_price,
                                    whole_sale_price ="",
                                    // retail_price = it.retail_price
                                    retail_price = ""
                                )
                            )
                        }
                        val submit_data = SubmitStockRequest(purchseitem_list, storeid)
                        *//*if(totalExposerstore > ){
                            Toast.makeText(this, "❌ Total exceeds actual store exposure!", Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        else{
                            stockSubmitViewmodel.callStockReqSubmitApi(
                                submit_data,
                                this@StockRequisitionSubmitActivity
                            )

                            val gson = Gson()
                            val jsonString = gson.toJson(submit_data)

                            Log.d("xyz", jsonString)
                        }*//*



                    } else {
                        showMessage("Product Quantity can't be empty or zero")
                    }

                }else{
                    showMessage("Please add atleast one product in cart")

                }



        }*/
        

        binding.noDataFound.setOnClickListener {
            val intent = Intent(this, StockRequsitionSearchActivity::class.java)
            startActivityForResult(intent, 100)
        }

        refreshAdapter()

        showToolbarImage()

    }

    private fun showToolbarImage() {
        val organisation_data = OrganisationDetailsHelper(this).getOrganisationData()

        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.fabicon)
            .fitCenter() // Add center crop
            .placeholder(R.drawable.mlogo) // Add a placeholder drawable
            .error(R.drawable.mlogo) // Add an error drawable (if needed)
            .into(binding.image)
    }

    private fun refreshAdapter() {

        val updatedList = sharedPrefHelper.getSearchResultsList().toMutableList()

        if (updatedList.isNotEmpty()) {
            //Toast.makeText(this,updatedList.size.toString(),Toast.LENGTH_SHORT).show()
            stockSubmitAdapter = StockSubmitAdapter(updatedList, this)
            binding.stocksubmitRcv.adapter = stockSubmitAdapter
            binding.stocksubmitRcv.isVisible = true
            binding.noDataFound.isVisible = false
            binding.relativeLayout.isVisible = true

        } else {
            binding.stocksubmitRcv.isVisible = false
            binding.noDataFound.isVisible = true
            binding.relativeLayout.isVisible = false
        }

    }

    private fun prepareStockitemRCV() {

        binding.stocksubmitRcv.apply {
            layoutManager = LinearLayoutManager(
                this@StockRequisitionSubmitActivity,
                RecyclerView.VERTICAL, false
            )

        }
    }


   /* override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            refreshAdapter()

        }
    }*/
   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
       super.onActivityResult(requestCode, resultCode, data)
       Log.d("onActivityResult", "called with request=$requestCode result=$resultCode data=$data")

       if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
           refreshAdapter()

           //val finalExposure = data.getDoubleExtra("final_exposure", 0.0)
          // val actualExposure = data.getIntExtra("actual_exposure", 0)
           val bundle = data.extras
          /* val finalExposure = when (val fe = bundle?.get("final_exposure")) {
               is Int -> fe.toDouble()
               is Long -> fe.toDouble()
               is Double -> fe
               is String -> fe.toDoubleOrNull() ?: 0.0
               else -> 0.0
           }*/
          /* val calculatedExposure = calculateFinalExposure()
           Log.d("Recalculated Exposure", "Live: $calculatedExposure")

// Optional: You can also use the value passed from intent (just for logging)
           val passedExposure = when (val fe = bundle?.get("final_exposure")) {
               is Int -> fe.toDouble()
               is Long -> fe.toDouble()
               is Double -> fe
               is String -> fe.toDoubleOrNull() ?: 0.0
               else -> 0.0
           }
           Log.d("PASS_BACK", "Received: $passedExposure but Recalculated: $calculatedExposure")
*/
          /* val liveExposure = calculateFinalExposure()
           Log.d("Exposure", "Live recalculated total: $liveExposure")
*/
           val actualExposure = when (val ae = bundle?.get("actual_exposure")) {
               is Int -> ae.toDouble()
               is Long -> ae.toDouble()
               is Double -> ae
               is String -> ae.toDoubleOrNull() ?: 0.0
               else -> 0.0
           }
           val storeExposure = when (val ae = bundle?.get("store_exposure")) {
               is Int -> ae.toDouble()
               is Long -> ae.toDouble()
               is Double -> ae
               is String -> ae.toDoubleOrNull() ?: 0.0
               else -> 0.0
           }

           Log.d("PASS_BACK", "final: $storeExposure, actual: $actualExposure")

           // Save locally if needed
           totalExposerstore = storeExposure.toString()
           Log.d("Total=",totalExposerstore)
           actualStoreExposure = actualExposure
           Log.d("Total=",actualStoreExposure.toString())
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
        Toast.makeText(this@StockRequisitionSubmitActivity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showSucessDialog(msg: String) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.sucess_dialog_layout)
        dialog.setCancelable(false)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(false)

        val confirm = dialog.findViewById<MaterialButton>(R.id.prefer_confirm)
        val logoutMsg = dialog.findViewById<TextView>(R.id.logout_msg)
        val logoutImg = dialog.findViewById<ImageView>(R.id.dialog_logo)

        logoutMsg.text = msg
        logoutMsg.textSize = 16F
        //logoutImg.setImageResource(R.drawable.svg_off)
        //logoutImg.scaleType = ImageView.ScaleType.FIT_CENTER

        confirm.setOnClickListener {
            dialog.dismiss()
            val intent =
                Intent(this@StockRequisitionSubmitActivity, StockRequisitionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        dialog.show()


    }

    override fun onResume() {
        super.onResume()
        refreshAdapter()
    }
    private fun calculateFinalExposure(): Double {
        val cartList = sharedPrefHelper.getSearchResultsList()

        val cartTotal = cartList.sumOf {
            val qty = it.cart_quantity.toDoubleOrNull() ?: 0.0
            val price = it.price
            val pack = it.no_of_packs
            qty * price * pack
        }

        // Convert totalExposerstore string to double safely
        val storeExposureValue = totalExposerstore.toDoubleOrNull() ?: 0.0

        val finalTotal = cartTotal + storeExposureValue
        Log.d("totalstocksubmit=",finalTotal.toString())

       // totalExposerstore = finalTotal.toString() // update string value if needed


        return finalTotal
    }




}