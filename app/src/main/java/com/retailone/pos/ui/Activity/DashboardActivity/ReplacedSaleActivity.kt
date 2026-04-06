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
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.retailone.pos.R
import com.retailone.pos.adapter.ReturnReasonAdapter
import com.retailone.pos.adapter.ReturnSalesItemAdapter
import com.retailone.pos.adapter.SaleReplaceAdapter
import com.retailone.pos.databinding.ActivityReplacedSaleBinding
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

class ReplacedSaleActivity : AppCompatActivity(), OnReturnQuantityChangeListener {

    private lateinit var binding: ActivityReplacedSaleBinding
    private lateinit var returnsale_viewmodel: ReturnSalesDetailsViewmodel
    private lateinit var returnSalesItemAdapter: ReturnSalesItemAdapter
    private lateinit var salesListAdapter: SaleReplaceAdapter

    private var returnItemList = mutableListOf<SalesItem>()
    private var returnReasonList: MutableList<ReturnReasonData> = mutableListOf()

    private lateinit var returnItemData: ReturnItemData
    private var reasonid = -1

    private var storeid = 0
    private var store_manager_id = "0"
    private lateinit var localizationData: LocalizationData

    private var printerUtil: PrinterUtil? = null
    private var returnbatchItemList = mutableListOf<BatchReturnItem>()

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReplacedSaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableBackButton()
        setToolbarImage()

        // Prefill invoice if navigated from Return tab
        intent.getStringExtra("preFillInvoice")?.let { q ->
            if (q.isNotBlank()) binding.searchBar.setQuery(q, false)
        }

        binding.relativeLayout.isVisible = false
        binding.relativeLayout2.isVisible = false

        // Top tabs
        setupTabs(savedInstanceState)

        returnsale_viewmodel = ViewModelProvider(this)[ReturnSalesDetailsViewmodel::class.java]
        localizationData = LocalizationHelper(this).getLocalizationData()

        val loginSession = LoginSession.getInstance(this)
        lifecycleScope.launch {
            // ✅ Initialize repository for offline state management
            returnsale_viewmodel.initRepository(this@ReplacedSaleActivity)
            
            storeid = loginSession.getStoreID().first().toInt()
            store_manager_id = loginSession.getStoreManagerID().first().toString()

            // ✅ Load from local DB first (instant, works offline!)
            val localSales = returnsale_viewmodel.getSalesFromLocalDB(this@ReplacedSaleActivity)
            if (localSales.isNotEmpty()) {
                salesListAdapter = SaleReplaceAdapter(this@ReplacedSaleActivity, localSales)
                binding.salesRecyclerView.apply {
                    adapter = salesListAdapter
                    layoutManager = LinearLayoutManager(this@ReplacedSaleActivity)
                    isVisible = true
                }
            }

            // ✅ Refresh from API
            returnsale_viewmodel.callreplaceSalesListApi(this@ReplacedSaleActivity, storeid.toString())
            
            // ✅ Fetch reasons for offline use
            returnsale_viewmodel.callSaleReturnReasonApi(this@ReplacedSaleActivity)
        }

        // Observe lists
        returnsale_viewmodel.salesListLiveData.observe(this) { response ->
            if (response.status == 1 && response.data.isNotEmpty()) {
                val data = response.data
                salesListAdapter = SaleReplaceAdapter(this, data)
                binding.salesRecyclerView.apply {
                    adapter = salesListAdapter
                    layoutManager = LinearLayoutManager(this@ReplacedSaleActivity)
                    isVisible = true
                }
            } else showMessage("No Sales Found")
        }

        // Loaders
        returnsale_viewmodel.loading.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message ?: "Unknown error")
        }
        returnsale_viewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        // Search by invoice
        binding.addcart.setOnClickListener {
            returnbatchItemList.clear()
            val query = binding.searchBar.query?.toString()?.trim().orEmpty()
            if (query.isEmpty()) {
                showMessage("Enter a valid Invoice ID")
            } else {
                returnsale_viewmodel.callReturnSalesDetailsApi(
                    ReturnItemReq(invoice_id = query),
                    this@ReplacedSaleActivity
                )
            }
        }

        // On details fetched → go to Replace Product picker
        returnsale_viewmodel.returnitem_liveData.observe(this) {
            if (it.data.isNotEmpty()) {
                val intent = Intent(this, SearchReplaceProductActivity::class.java)
                intent.putExtra("invoice_id", binding.searchBar.query.toString().trim())
                startActivity(intent)
            } else showMessage("No Invoice Found")
        }

        // Reasons
//        returnsale_viewmodel.salesreturnreason_liveData.observe(this) {
//            returnReasonList = it.data.toMutableList()
//            binding.reasonInput.setAdapter(ReturnReasonAdapter(this, 0, returnReasonList))
//        }
//        binding.reasonInput.setOnClickListener {
//            if (returnReasonList.isEmpty()) showMessage("Return reason not found, try after sometime")
//        }
//        binding.reasonInput.setOnItemClickListener { _, _, position, _ ->
//            binding.reasonInput.setText(returnReasonList[position].reason_name, false)
//            reasonid = returnReasonList[position].id
//        }

        // Submit
        binding.nextlayout.setOnClickListener {
            if (returnbatchItemList.isNotEmpty()) {
                callReturnAPI(returnbatchItemList)
            } else showMessage("You haven't Return anything")
        }

        printerUtil = PrinterUtil(this)
    }

    /** Build and handle top tabs: index 0=Return, 1=Replace (selected here) */
    private fun setupTabs(savedInstanceState: Bundle?) {
        val tabLayout: TabLayout = binding.topTabs

        if (tabLayout.tabCount == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("Return Product"))
            tabLayout.addTab(tabLayout.newTab().setText("Replace Product"))
        } else {
            tabLayout.getTabAt(0)?.text = "Return Product"
            tabLayout.getTabAt(1)?.text = "Replace Product"
        }

        // Select 1 by default on this screen
        val selected = savedInstanceState?.getInt(KEY_SELECTED_TAB) ?: 1
        tabLayout.getTabAt(selected)?.select()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) {
                    val intent = Intent(this@ReplacedSaleActivity, ReturnSaleActivity::class.java)
                    intent.putExtra("preFillInvoice", binding.searchBar.query?.toString()?.trim())
                    startActivity(intent)
                    finish()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    val intent = Intent(this@ReplacedSaleActivity, ReturnSaleActivity::class.java)
                    intent.putExtra("preFillInvoice", binding.searchBar.query?.toString()?.trim())
                    startActivity(intent)
                    finish()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, binding.topTabs.selectedTabPosition)
    }

    private fun callReturnAPI(returnbatchItemList: MutableList<BatchReturnItem>) {
        val cartitemlist = mutableListOf<ReturnedItem>()
        returnbatchItemList.forEach {
            if ((it.batch_return_quantity ?: 0) != 0) {
                cartitemlist.add(ReturnedItem(id = it.sales_item_id ?: 0, return_quantity = it.batch_return_quantity ?: 0))
            }
        }
        if (reasonid == -1) { showMessage("please select any reason for return"); return }
        val return_data = ReturnSaleReq(
            store_id = storeid,
            store_manager_id = store_manager_id.toInt(),
            reason_id = reasonid,
            sales_id = returnItemData.id,
            returned_items = cartitemlist.toList()
        )
        returnsale_viewmodel.callReturnSalesSubmitApi(return_data, this@ReplacedSaleActivity)
        Log.d("req", Gson().toJson(return_data))
    }

    private fun getReturnDateTime(): String {
        val timezone = when (LocalizationHelper(this).getLocalizationData().timezone) {
            "IST" -> "Asia/Kolkata"
            "CAT" -> "Africa/Lusaka"
            else -> "Africa/Lusaka"
        }
        val tz = TimeZone.getTimeZone(timezone)
        val cal = Calendar.getInstance().apply { timeZone = tz }
        return SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).apply {
            timeZone = tz
        }.format(cal.time)
    }

    private fun setToolbarImage() {
        val organisation_data = OrganisationDetailsHelper(this).getOrganisationData()
        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.fabicon)
            .fitCenter()
            .placeholder(R.drawable.mlogo)
            .error(R.drawable.mlogo)
            .into(binding.image)
    }

    private fun enableBackButton() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "New Activity"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.svg_back_arrow_white)
        }
    }

    //override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onPressed(); return true }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }

    private fun showMessage(msg: String) =
        Toast.makeText(this@ReplacedSaleActivity, msg, Toast.LENGTH_SHORT).show()

    override fun onReturnQuantityChange(position: Int, newQuantity: Int) {
        returnItemList[position].return_quantity = newQuantity
        returnItemList[position].refund_amount = newQuantity * returnItemList[position].retail_price
        var refundTotal = 0.0
        returnItemList.forEach { if (it.refund_amount != 0.0) refundTotal += it.refund_amount }
        binding.rlPrice2.text = NumberFormatter().formatPrice(refundTotal.toString(), localizationData)
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
        val print_receipt = dialog.findViewById<MaterialButton>(R.id.print_receipt)

        logoutMsg.text = msg
        logoutMsg.textSize = 16F

        confirm.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this@ReplacedSaleActivity, MPOSDashboardActivity::class.java))
            finish()
        }
        print_receipt.setOnClickListener { printerUtil?.printReturnReceiptData(returnSaleRes) }
        dialog.show()
    }

    fun dismissKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onResume() { super.onResume(); printerUtil?.registerBatteryReceiver() }
    override fun onPause() { super.onPause(); printerUtil?.unregisterBatteryReceiver() }
}
