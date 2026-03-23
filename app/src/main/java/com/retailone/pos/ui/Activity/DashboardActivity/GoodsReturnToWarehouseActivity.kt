//package com.retailone.pos.ui.Activity.DashboardActivity
//
//import android.content.Context
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.util.Log
//import android.widget.Toast
//import androidx.core.view.isVisible
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.google.android.material.bottomsheet.BottomSheetDialog
//import com.retailone.pos.R
//import com.retailone.pos.adapter.PastRequDetailsAdapter
//import com.retailone.pos.adapter.ProductReorderAlertAdapter
//import com.retailone.pos.adapter.StockListAdapter
//import com.retailone.pos.databinding.ActivityGoodsReturntoWarehouseBinding
//import com.retailone.pos.databinding.BottomsheetPastRequisitionLayoutBinding
//import com.retailone.pos.localstorage.DataStore.LoginSession
//import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
//import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnRequests
//import com.retailone.pos.models.LocalizationModel.LocalizationData
//import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData
//import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsList
//import com.retailone.pos.utils.DateTimeFormatting
//import com.retailone.pos.viewmodels.DashboardViewodel.GoodsReturntoWarehouseViewModel
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//class GoodsReturnToWarehouseActivity : AppCompatActivity() {
//    lateinit var  binding:ActivityGoodsReturntoWarehouseBinding
//   // lateinit var matrcvViewmodel: MaterialReceivingViewmodel
//    lateinit var goodsReturntoWarehouseViewModel : GoodsReturntoWarehouseViewModel
//   // lateinit var goodsReturnWarehouseViewModel : GoodsReturnWarehouseViewModel
//    //lateinit var pastRequisitionAdapter: PastRequisitionAdapter
//    lateinit var productReorderAlertAdapter: ProductReorderAlertAdapter
//    lateinit var  localizationData: LocalizationData;
//    lateinit var attachedRecyclerView: RecyclerView
//
//    private lateinit var stockListAdapter: StockListAdapter
//    var returnReasonList: MutableList<ReturnReasonData> = mutableListOf()
//
//    private  var storeid = ""
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityGoodsReturntoWarehouseBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//       // matrcvViewmodel = ViewModelProvider(this)[MaterialReceivingViewmodel::class.java]
//        goodsReturntoWarehouseViewModel = ViewModelProvider(this)[GoodsReturntoWarehouseViewModel::class.java]
//       // goodsReturnWarehouseViewModel = ViewModelProvider(this)[GoodsReturnWarehouseViewModel::class.java]
//       // localizationData = LocalizationHelper(this).getLocalizationData()
//
//       // binding.productAlertRcv.adapter = stockListAdapter
//
//
//        enableBackButton()
//
//        val loginSession= LoginSession.getInstance(this)
//        /*lifecycleScope.launch {
//            storeid = loginSession.getStoreID().first()
//            stockRequisitionViewmodel.callPastRequsitionApi(storeid,this@GoodsReturnToWarehouseActivity)
//        }*/
//
//        lifecycleScope.launch {
//            storeid = LoginSession.getInstance(this@GoodsReturnToWarehouseActivity).getStoreID().first()
//            goodsReturntoWarehouseViewModel.callStockListApi(storeid, this@GoodsReturnToWarehouseActivity)
//            goodsReturntoWarehouseViewModel.callSaleReturnReasonApi(this@GoodsReturnToWarehouseActivity)
//        }
//
//
//
//                binding.relativeLayout.setOnClickListener {
//
//                    val returnDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
//                    val remarks = binding.etRemarks.text.toString().trim()
//
//                   // stockListAdapter.syncInputs() // ✅ Sync before getting items
//
//                    val selectedItems = stockListAdapter.getSelectedItems()
//
//
//                    if (selectedItems.isEmpty()) {
//                        Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show()
//                        return@setOnClickListener
//                    }
//
//                    // ✅ Check for missing remarks only if condition is "good"
//                   /* val missingRemarks = selectedItems.any {
//                        it.condition.equals("Good", ignoreCase = true) && it.remarks.isNullOrEmpty()
//                    }*/
//                    ///////
//                  /*  val missingRemarks = selectedItems.any { si ->
//                        (si.condition.equals("Good", true)||  (si.condition.equals("Store Stock", true)  || si.fromGoodReturnedMap) && si.remarks.isBlank())
//                    }*/
//                    val missingRemarks = selectedItems.any { si ->
//                        (si.condition.equals("Good", true)
//                                || si.condition.equals("Store Stock", true)
//                                || si.fromGoodReturnedMap)
//                                && si.remarks.isNullOrBlank()
//                    }
//
//
//                    ////////
//
//
//                    if (missingRemarks) {
//                        Toast.makeText(this, "Remark field cannot be empty for selected items.", Toast.LENGTH_SHORT).show()
//                        return@setOnClickListener
//                    }
//
//                    val request = StockReturnRequests(
//                        store_id = storeid.toInt(),
//                        return_date = returnDate,
//                        remarks = remarks,
//                        items = selectedItems
//                    )
//
//                    goodsReturntoWarehouseViewModel.submitStockReturn(request, this)
//                }
//
//
//        goodsReturntoWarehouseViewModel.loadingLiveData.observe(this){
//            binding.progress.isVisible = it.isProgress
//
//            if(it.isMessage)
//                showMessage(it.message)
//        }
//
//
//
//        goodsReturntoWarehouseViewModel.salesreturnreason_liveData.observe(this) {
//            // Filter status == 1 and extract reason_name
//            returnReasonList = it.data.filter { reason -> reason.status == 1 }.toMutableList()
//            val reasonNames = returnReasonList.map { reason -> reason.reason_name }
//
//
//
//        }
//
//            goodsReturntoWarehouseViewModel.stockListLiveData.observe(this) { response ->
//                if (response.status == 1) {
//                  //  val products = response.data.flatMap { it.products }
//                    val filteredProducts = response.data.flatMap { it.products }.filter { product ->
//                        product.distribution_pack_data.any { pack ->
//                            val stock = pack.stock_quatity ?: 0
//                            val returnedQtySum = pack.returned_items?.values?.sum() ?: 0
//                            val goodReturnedQtySum = pack.good_returned_items?.values?.sum() ?: 0
//
//                            // Include this product only if there's any quantity available
//                            stock > 0 || returnedQtySum > 0 || goodReturnedQtySum > 0
//                        }
//                    }
//
//                    // ✅ Initialize adapter globally
//                    // ✅ Extract reason names from previously filtered list
//                    val reasonNames = returnReasonList.map { it.reason_name }
//                    stockListAdapter = StockListAdapter(filteredProducts, reasonNames)
//                    Log.d("ReturnreasonList",returnReasonList.toString())
//                    //val adapter = StockListAdapter(products)
//                    binding.productAlertRcv.layoutManager = LinearLayoutManager(this)
//                    //binding.productAlertRcv.adapter = adapter
//                    binding.productAlertRcv.adapter = stockListAdapter
//                   // stockListAdapter.attachedRecyclerView = binding.productAlertRcv
//                }
//            }
//
//        goodsReturntoWarehouseViewModel.loadingLiveData.observe(this) {
//            binding.progress.isVisible = it.isProgress
//            if (it.isMessage) {
//                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
//            }
//        }
//
//
//        //preparePastReqRCV()
//        prepareProductAlertRCV()
//
//
//        showToolbarImage()
//
//        // 4. ACTIVITY CODE EXTRACT
//
//
//
//
//
//
//
//// 5. OBSERVE RESPONSE
//
//        goodsReturntoWarehouseViewModel.stockReturnSubmitLiveData.observe(this) { response ->
//            if (response.status == "success") {
//                Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
//                finish() // or redirect
//            } else {
//                Toast.makeText(this, response.message, Toast.LENGTH_LONG).show()
//                response.errors?.forEach { (field, messages) ->
//                    Log.e("Validation", "$field: ${messages.joinToString()}")
//                }
//            }
//        }
//
//
//    }
//
//    private fun showToolbarImage() {
//        val organisation_data = OrganisationDetailsHelper(this).getOrganisationData()
//
//        Glide.with(this)
//            .load(organisation_data.image_url + organisation_data.fabicon)
//            .fitCenter() // Add center crop
//            .placeholder(R.drawable.mlogo) // Add a placeholder drawable
//            .error(R.drawable.mlogo) // Add an error drawable (if needed)
//            .into(binding.image)
//    }
//
//
//    /*private fun preparePastReqRCV() {
//
//        binding.pastReqsRcv.apply {
//            layoutManager = LinearLayoutManager(this@GoodsReturnToWarehouseActivity,
//                RecyclerView.VERTICAL,false)
//
//        }
//    }*/
//
//    private fun prepareProductAlertRCV() {
//        productReorderAlertAdapter = ProductReorderAlertAdapter()
//
//        binding.productAlertRcv.apply {
//            layoutManager = LinearLayoutManager(this@GoodsReturnToWarehouseActivity,
//                RecyclerView.VERTICAL,false)
//            adapter = productReorderAlertAdapter
//        }
//    }
//
//
//
//    private fun DetailsBottomsheet(pastReqDetailsList: PastReqDetailsList, context:Context) {
//        val d_binding = BottomsheetPastRequisitionLayoutBinding.inflate(layoutInflater)
//
//        val dialog = BottomSheetDialog(this)
//        dialog.setContentView(d_binding.root)
//        // dialog.setCancelable(false)
//        //dialog.setCanceledOnTouchOutside(false)
//
//        d_binding.detailsRcv.layoutManager = LinearLayoutManager(this,RecyclerView.VERTICAL,false)
//
//        val detailsAdapter = PastRequDetailsAdapter(context,pastReqDetailsList)
//        d_binding.detailsRcv.adapter = detailsAdapter
//
//        d_binding.closeBottomsheet.setOnClickListener {
//            dialog.dismiss()
//        }
//
//        pastReqDetailsList.created_at?.let {
//            d_binding.orderdate.isVisible = true
//            d_binding.orderdate.text ="Ordered at: "+DateTimeFormatting.formatOrderdate(it,localizationData.timezone)
//
//        }
//        pastReqDetailsList.approve_date?.let {
//            d_binding.approvedate.isVisible = true
//            d_binding.approvedate.text ="Approved at: "+DateTimeFormatting.formatApprovedate(it,localizationData.timezone)
//        }
//
//        pastReqDetailsList.receive_date?.let {
//            d_binding.receivedate.isVisible = true
//            d_binding.receivedate.text ="Received at: "+DateTimeFormatting.formatReceivedate(it,localizationData.timezone)
//        }
//
//        dialog.show()
//    }
//
//    private fun enableBackButton() {
//        setSupportActionBar(binding.toolbar)
//        //actionbar
//        val actionbar = supportActionBar
//        //set actionbar title
//        actionbar!!.title = "New Activity"
//        //set back button
//        actionbar.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setHomeAsUpIndicator(R.drawable.svg_back_arrow_white)
//    }
//
//
//    override fun onSupportNavigateUp(): Boolean {
//        onBackPressed()
//        return true
//    }
//
//
//
//    private fun showMessage(msg: String) {
//        Toast.makeText(this@GoodsReturnToWarehouseActivity, msg, Toast.LENGTH_SHORT).show()
//    }
//}


package com.retailone.pos.ui.Activity.DashboardActivity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import com.retailone.pos.adapter.ProductReorderAlertAdapter
import com.retailone.pos.adapter.StockListAdapter
import com.retailone.pos.databinding.ActivityGoodsReturntoWarehouseBinding
import com.retailone.pos.databinding.BottomsheetPastRequisitionLayoutBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.GoodsToWarehouseModel.Stock.StockReturnRequests
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsList
import com.retailone.pos.utils.DateTimeFormatting
import com.retailone.pos.viewmodels.DashboardViewodel.GoodsReturntoWarehouseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoodsReturnToWarehouseActivity : AppCompatActivity() {
    lateinit var binding: ActivityGoodsReturntoWarehouseBinding
    lateinit var goodsReturntoWarehouseViewModel: GoodsReturntoWarehouseViewModel
    lateinit var productReorderAlertAdapter: ProductReorderAlertAdapter
    lateinit var localizationData: LocalizationData
    lateinit var attachedRecyclerView: RecyclerView

    private lateinit var stockListAdapter: StockListAdapter
    var returnReasonList: MutableList<ReturnReasonData> = mutableListOf()

    private var storeid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGoodsReturntoWarehouseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        goodsReturntoWarehouseViewModel =
            ViewModelProvider(this)[GoodsReturntoWarehouseViewModel::class.java]

        enableBackButton()

        lifecycleScope.launch {
            storeid =
                LoginSession.getInstance(this@GoodsReturnToWarehouseActivity).getStoreID().first()
            goodsReturntoWarehouseViewModel.callStockListApi(
                storeid,
                this@GoodsReturnToWarehouseActivity
            )
            goodsReturntoWarehouseViewModel.callSaleReturnReasonApi(
                this@GoodsReturnToWarehouseActivity
            )
        }

        binding.relativeLayout.setOnClickListener {

            val returnDate =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val remarks = binding.etRemarks.text.toString().trim()

            val selectedItems = stockListAdapter.getSelectedItems()

            if (selectedItems.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please select at least one item",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val missingRemarks = selectedItems.any { si ->
                (si.condition.equals("Good", true)
                        || si.condition.equals("Store Stock", true)
                        || si.fromGoodReturnedMap)
                        && si.remarks.isNullOrBlank()
            }

            if (missingRemarks) {
                Toast.makeText(
                    this,
                    "Remark field cannot be empty for selected items.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val request = StockReturnRequests(
                store_id = storeid.toInt(),
                return_date = returnDate,
                remarks = remarks,
                items = selectedItems
            )

            goodsReturntoWarehouseViewModel.submitStockReturn(request, this)
        }

        goodsReturntoWarehouseViewModel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress

            if (it.isMessage)
                showMessage(it.message)
        }

        goodsReturntoWarehouseViewModel.salesreturnreason_liveData.observe(this) {
            returnReasonList = it.data.filter { reason -> reason.status == 1 }.toMutableList()
        }

        goodsReturntoWarehouseViewModel.stockListLiveData.observe(this) { response ->
            if (response.status == 1) {
                // 🔧 FIXED: use new map structure (Map<String, ReturnedItemDetails>)
                val filteredProducts =
                    response.data.flatMap { it.products }.filter { product ->
                        product.distribution_pack_data.any { pack ->
                            val stock = pack.stock_quatity

                            val returnedQtySum = pack.returned_items
                                ?.values
                                ?.sumOf { detailsMap ->
                                    detailsMap["total_quantity"]
                                        ?: detailsMap.values.sum()
                                } ?: 0

                            val goodReturnedQtySum = pack.good_returned_items
                                ?.values
                                ?.sumOf { detailsMap ->
                                    detailsMap["total_quantity"]
                                        ?: detailsMap.values.sum()
                                } ?: 0

                            // Include product only if there’s any quantity
                            stock > 0 || returnedQtySum > 0 || goodReturnedQtySum > 0
                        }
                    }

                val reasonNames = returnReasonList.map { it.reason_name }
                stockListAdapter = StockListAdapter(filteredProducts, reasonNames)
                Log.d("ReturnreasonList", returnReasonList.toString())

                binding.productAlertRcv.layoutManager =
                    LinearLayoutManager(this)
                binding.productAlertRcv.adapter = stockListAdapter
            }
        }

        goodsReturntoWarehouseViewModel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
        }

        prepareProductAlertRCV()
        showToolbarImage()



        goodsReturntoWarehouseViewModel.stockReturnSubmitLiveData.observe(this) { response ->
            if (response.status == "success") {
                Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, response.message, Toast.LENGTH_LONG).show()
                response.errors?.forEach { (field, messages) ->
                    Log.e("Validation", "$field: ${messages.joinToString()}")
                }
            }
        }
    }




    private fun showToolbarImage() {
        val organisation_data = OrganisationDetailsHelper(this).getOrganisationData()

        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.fabicon)
            .fitCenter()
            .placeholder(R.drawable.mlogo)
            .error(R.drawable.mlogo)
            .into(binding.image)
    }

    private fun prepareProductAlertRCV() {
        productReorderAlertAdapter = ProductReorderAlertAdapter()

        binding.productAlertRcv.apply {
            layoutManager = LinearLayoutManager(
                this@GoodsReturnToWarehouseActivity,
                RecyclerView.VERTICAL,
                false
            )
            adapter = productReorderAlertAdapter   // will be replaced when stockList arrives
        }
    }

    private fun DetailsBottomsheet(
        pastReqDetailsList: PastReqDetailsList,
        context: Context
    ) {
        val d_binding = BottomsheetPastRequisitionLayoutBinding.inflate(layoutInflater)

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(d_binding.root)

        d_binding.detailsRcv.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        val detailsAdapter = PastRequDetailsAdapter(context, pastReqDetailsList)
        d_binding.detailsRcv.adapter = detailsAdapter

        pastReqDetailsList.created_at?.let {
            d_binding.orderdate.isVisible = true
            d_binding.orderdate.text =
                "Ordered at: " + DateTimeFormatting.formatOrderdate(it, localizationData.timezone)
        }
        pastReqDetailsList.approve_date?.let {
            d_binding.approvedate.isVisible = true
            d_binding.approvedate.text =
                "Approved at: " + DateTimeFormatting.formatApprovedate(
                    it,
                    localizationData.timezone
                )
        }
        pastReqDetailsList.receive_date?.let {
            d_binding.receivedate.isVisible = true
            d_binding.receivedate.text =
                "Received at: " + DateTimeFormatting.formatReceivedate(
                    it,
                    localizationData.timezone
                )
        }

        d_binding.closeBottomsheet.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun enableBackButton() {
        setSupportActionBar(binding.toolbar)
        val actionbar = supportActionBar
        actionbar!!.title = "New Activity"
        actionbar.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.svg_back_arrow_white)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@GoodsReturnToWarehouseActivity, msg, Toast.LENGTH_SHORT).show()
    }
}
