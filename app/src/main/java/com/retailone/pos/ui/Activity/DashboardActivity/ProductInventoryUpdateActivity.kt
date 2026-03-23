package com.retailone.pos.ui.Activity.DashboardActivity

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.retailone.pos.R
import com.retailone.pos.adapter.PiUpdateAdapter
import com.retailone.pos.adapter.StockSubmitAdapter
import com.retailone.pos.databinding.ActivityProductInventoryBinding
import com.retailone.pos.databinding.ActivityProductInventoryUpdateBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.InventoryStockHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.ProductInventoryModel.InventoryUpdateReqModel.InventoryProduct
import com.retailone.pos.models.ProductInventoryModel.InventoryUpdateReqModel.InventoryUpdateRequest
import com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel.PurchaseItem
import com.retailone.pos.models.StockRequisitionModel.SubmitStockRequsitionModel.SubmitStockRequest
import com.retailone.pos.ui.Activity.MPOSDashboardActivity
import com.retailone.pos.viewmodels.DashboardViewodel.PiUpdateViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Locale

class ProductInventoryUpdateActivity : AppCompatActivity() {
    lateinit var binding: ActivityProductInventoryUpdateBinding
    lateinit var piu_viewmodel: PiUpdateViewmodel
    lateinit var piu_adapter: PiUpdateAdapter
    lateinit var inventoryStockHelper: InventoryStockHelper
    private var storeid = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductInventoryUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        piu_viewmodel = ViewModelProvider(this)[PiUpdateViewmodel::class.java]
        inventoryStockHelper = InventoryStockHelper(this@ProductInventoryUpdateActivity)

        preparePiuRCV()
        enableBackButton()


        val loginSession = LoginSession.getInstance(this)
        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first()
        }


        binding.barcodecard.setOnClickListener {
            val intent = Intent(this, ProductInventorySearchActivity::class.java)
            startActivityForResult(intent, 100)
        }

        getCurrentTime()

        binding.nextlayout.setOnClickListener {

            val updatedList = inventoryStockHelper.getSearchResultsList().toMutableList()

            if (updatedList.isNotEmpty()) {

                if (!updatedList.any { it.cart_quantity.toInt() == 0 }) {

                    val purchseitem_list = mutableListOf<InventoryProduct>()

                    updatedList.forEach {
                        purchseitem_list.add(
                            InventoryProduct(
                                category_id = it.category_id.toString(),
                                distribution_pack_id = it.distribution_pack_id,
                                product_id = it.product_id.toString(),
                                quantity = it.cart_quantity.toString(),
                                supplier_id = it.supplier_id.toString()
                            )
                        )
                    }
                    val submit_data = InventoryUpdateRequest(purchseitem_list, storeid)

                    val gson = Gson()
                    val jsonString = gson.toJson(submit_data)

                    Log.d("xxx", jsonString)

                    piu_viewmodel.callInventoryUpdateApi(
                        submit_data,
                        this@ProductInventoryUpdateActivity
                    )
                } else {
                    showMessage("Product Quantity can't be empty or zero")
                }

            } else {
                showMessage("Please select atleast one product")
            }

        }

        piu_viewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage)
                showMessage(it.message)
        }

        piu_viewmodel.inventoryUpdateLiveData.observe(this) {

            if (it.status == 1) {
                // showMessage(it.message)
                inventoryStockHelper.clearSearchResultsList()
                refreshAdapter()
                showSucessDialog(it.message)
            } else {
                showMessage("Something Went Wrong ,Try again")
            }
        }


        setToolbarImage()

        refreshAdapter()

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

    private fun getCurrentTime() {
        val calendar = Calendar.getInstance()
        val currentDateTime = calendar.time

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())

        val formattedDateTime = dateFormat.format(currentDateTime)

        binding.calenderText.text = "Stock taking date: $formattedDateTime"
    }

    private fun preparePiuRCV() {

        binding.piuRcv.apply {
            layoutManager = LinearLayoutManager(
                this@ProductInventoryUpdateActivity,
                RecyclerView.VERTICAL,
                false
            )

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            //val cat_id = data?.getIntExtra("cat_id", -1)
            // val id = data?.getStringExtra("id")
            // val service_name = data?.getStringExtra("service_name")

            //  serviceid = id.toString()
            //categotyid = cat_id.toString()

            // Use itemId and itemName as needed in Activity A

            // Toast.makeText(this,"Sucess", Toast.LENGTH_SHORT).show()
            //binding.serviceEdit.text = Editable.Factory.getInstance().newEditable(service_name)

            refreshAdapter()

        }
    }

    private fun refreshAdapter() {

        val updatedList = inventoryStockHelper.getSearchResultsList().toMutableList()

        if (updatedList.isNotEmpty()) {
            //Toast.makeText(this,updatedList.size.toString(),Toast.LENGTH_SHORT).show()
            piu_adapter = PiUpdateAdapter(updatedList, this)
            binding.piuRcv.adapter = piu_adapter
            binding.piuRcv.isVisible = true
            binding.noDataFound.isVisible = false
            binding.relativeLayout.isVisible = true

        } else {
            binding.piuRcv.isVisible = false
            binding.noDataFound.isVisible = true
            binding.relativeLayout.isVisible = false
        }

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
                Intent(this@ProductInventoryUpdateActivity, ProductInventoryActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()

        }

        dialog.show()


    }


    private fun showMessage(msg: String) {
        Toast.makeText(this@ProductInventoryUpdateActivity, msg, Toast.LENGTH_SHORT).show()
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
}