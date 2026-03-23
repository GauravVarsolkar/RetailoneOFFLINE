package com.retailone.pos.ui.Activity.DashboardActivity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.retailone.pos.R
import com.retailone.pos.adapter.PastRequDetailsAdapter
import com.retailone.pos.adapter.PastRequisitionAdapter
import com.retailone.pos.databinding.ActivityMaterialRecivingItemsBinding
import com.retailone.pos.databinding.BottomsheetPastRequisitionLayoutBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.OrderItem
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsList
import com.retailone.pos.viewmodels.DashboardViewodel.StockRequisitionViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MaterialRecivingItemsActivity :  AppCompatActivity() {
    lateinit var  binding: ActivityMaterialRecivingItemsBinding
    lateinit var stockRequisitionViewmodel: StockRequisitionViewmodel
    lateinit var pastRequisitionAdapter: PastRequisitionAdapter

    private  var storeid = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMaterialRecivingItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stockRequisitionViewmodel = ViewModelProvider(this)[StockRequisitionViewmodel::class.java]

        enableBackButton()


        val loginSession= LoginSession.getInstance(this)
        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first()
            stockRequisitionViewmodel.callPastRequsitionApi(storeid,this@MaterialRecivingItemsActivity)
        }


        stockRequisitionViewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }

        preparePastReqRCV()


        stockRequisitionViewmodel.pastrequsition_livedata.observe(this){

            if(it.status == 1){
                pastRequisitionAdapter = PastRequisitionAdapter(this, it.data, "all"){ requestid->

                    val intent = Intent(this@MaterialRecivingItemsActivity,MaterialReceivingActivity::class.java)
                    intent.putExtra("request_id",requestid)
                    startActivity(intent)

                }
                binding.pastReqsRcv.adapter = pastRequisitionAdapter
            }
        }


        binding.swipeLayout.setOnRefreshListener {
            stockRequisitionViewmodel.callPastRequsitionApi(storeid,this@MaterialRecivingItemsActivity)
            binding.swipeLayout.isRefreshing = false
        }

        setToolbarImage()

     /*   stockRequisitionViewmodel.past_req_details_livedata.observe(this){

            //DetailsBottomsheet(it.data[0].order_items,this)

            val intent = Intent(this@MaterialRecivingItemsActivity,MaterialReceivingActivity::class.java)
            startActivity(intent)


        }*/


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

    private fun preparePastReqRCV() {

        binding.pastReqsRcv.apply {
            layoutManager = LinearLayoutManager(this@MaterialRecivingItemsActivity,
                RecyclerView.VERTICAL,false)

        }
    }




    private fun DetailsBottomsheet(pastReqDetailsList: PastReqDetailsList, context: Context) {
        val d_binding = BottomsheetPastRequisitionLayoutBinding.inflate(layoutInflater)

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(d_binding.root)
        // dialog.setCancelable(false)
        //dialog.setCanceledOnTouchOutside(false)

        d_binding.detailsRcv.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL,false)

        val detailsAdapter = PastRequDetailsAdapter(context,pastReqDetailsList)
        d_binding.detailsRcv.adapter = detailsAdapter

        d_binding.closeBottomsheet.setOnClickListener {
            dialog.dismiss()
        }


        dialog.show()
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
        Toast.makeText(this@MaterialRecivingItemsActivity, msg, Toast.LENGTH_SHORT).show()
    }
}