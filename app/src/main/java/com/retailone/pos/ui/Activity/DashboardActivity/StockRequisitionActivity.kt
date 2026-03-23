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
import com.retailone.pos.adapter.ProductReorderAlertAdapter
import com.retailone.pos.databinding.ActivityStockRequisitionBinding
import com.retailone.pos.databinding.BottomsheetPastRequisitionLayoutBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsList
import com.retailone.pos.utils.DateTimeFormatting
import com.retailone.pos.viewmodels.DashboardViewodel.MaterialReceivingViewmodel
import com.retailone.pos.viewmodels.DashboardViewodel.StockRequisitionViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StockRequisitionActivity : AppCompatActivity() {
    lateinit var  binding:ActivityStockRequisitionBinding
    lateinit var matrcvViewmodel: MaterialReceivingViewmodel
    lateinit var stockRequisitionViewmodel: StockRequisitionViewmodel
    lateinit var pastRequisitionAdapter: PastRequisitionAdapter
    lateinit var productReorderAlertAdapter: ProductReorderAlertAdapter
    lateinit var  localizationData: LocalizationData;

    private  var storeid = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStockRequisitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        matrcvViewmodel = ViewModelProvider(this)[MaterialReceivingViewmodel::class.java]
        stockRequisitionViewmodel = ViewModelProvider(this)[StockRequisitionViewmodel::class.java]
        localizationData = LocalizationHelper(this).getLocalizationData()


        enableBackButton()

        val loginSession= LoginSession.getInstance(this)
        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first()
            stockRequisitionViewmodel.callPastRequsitionApi(storeid,this@StockRequisitionActivity)
        }


        binding.relativeLayout.setOnClickListener {
            val intent = Intent(this@StockRequisitionActivity,StockRequisitionSubmitActivity::class.java)
            //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
        }

        stockRequisitionViewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }

        preparePastReqRCV()
        prepareProductAlertRCV()


        stockRequisitionViewmodel.pastrequsition_livedata.observe(this){

            if(it.status == 1){
                pastRequisitionAdapter = PastRequisitionAdapter(this,it.data,"some"){requestid->
                   // Toast.makeText(this,it,Toast.LENGTH_SHORT).show()
                    stockRequisitionViewmodel.callRequisitionDetailsApi(requestid,this)
                }
                binding.pastReqsRcv.adapter = pastRequisitionAdapter
            }
        }

        stockRequisitionViewmodel.past_req_details_livedata.observe(this){

            DetailsBottomsheet(it.data[0],this)
        }



        matrcvViewmodel.getMaterialRcvData()
        matrcvViewmodel.material_rcv_LiveData.observe(this){
            //pastRequisitionAdapter.setMatRcvdData(it)
            productReorderAlertAdapter.setMatRcvdData(it)
        }

        binding.pastViewAll.setOnClickListener {
            startActivity(Intent(this@StockRequisitionActivity,StockRequisitionViewAllActivity::class.java))
        }


        binding.swipeLayout.setOnRefreshListener {

            stockRequisitionViewmodel.callPastRequsitionApi(storeid,this@StockRequisitionActivity)
            binding.swipeLayout.isRefreshing = false

        }

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


    private fun preparePastReqRCV() {

        binding.pastReqsRcv.apply {
            layoutManager = LinearLayoutManager(this@StockRequisitionActivity,
                RecyclerView.VERTICAL,false)

        }
    }

    private fun prepareProductAlertRCV() {
        productReorderAlertAdapter = ProductReorderAlertAdapter()

        binding.productAlertRcv.apply {
            layoutManager = LinearLayoutManager(this@StockRequisitionActivity,
                RecyclerView.VERTICAL,false)
            adapter = productReorderAlertAdapter
        }
    }



    private fun DetailsBottomsheet(pastReqDetailsList: PastReqDetailsList, context:Context) {
        val d_binding = BottomsheetPastRequisitionLayoutBinding.inflate(layoutInflater)

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(d_binding.root)
        // dialog.setCancelable(false)
        //dialog.setCanceledOnTouchOutside(false)

        d_binding.detailsRcv.layoutManager = LinearLayoutManager(this,RecyclerView.VERTICAL,false)

        val detailsAdapter = PastRequDetailsAdapter(context,pastReqDetailsList)
        d_binding.detailsRcv.adapter = detailsAdapter

        d_binding.closeBottomsheet.setOnClickListener {
            dialog.dismiss()
        }

        pastReqDetailsList.created_at?.let {
            d_binding.orderdate.isVisible = true
            d_binding.orderdate.text ="Ordered at: "+DateTimeFormatting.formatOrderdate(it,localizationData.timezone)

        }
        pastReqDetailsList.approve_date?.let {
            d_binding.approvedate.isVisible = true
            d_binding.approvedate.text ="Approved at: "+DateTimeFormatting.formatApprovedate(it,localizationData.timezone)
        }

        pastReqDetailsList.receive_date?.let {
            d_binding.receivedate.isVisible = true
            d_binding.receivedate.text ="Received at: "+DateTimeFormatting.formatReceivedate(it,localizationData.timezone)
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
        Toast.makeText(this@StockRequisitionActivity, msg, Toast.LENGTH_SHORT).show()
    }
}