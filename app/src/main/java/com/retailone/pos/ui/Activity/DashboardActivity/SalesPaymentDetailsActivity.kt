package com.retailone.pos.ui.Activity.DashboardActivity

import NumberFormatter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retailone.pos.R
import com.retailone.pos.adapter.SalesDetailsAdapter
import com.retailone.pos.databinding.ActivitySalesPaymentDetailsBinding
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.Dispatch.DispatchRequest
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.SalesPaymentModel.InvoicePayment.CancelSaleitemRequest
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsReq
import com.retailone.pos.models.SalesPaymentModel.SalesDetails.SalesDetailsRes
import com.retailone.pos.utils.DateTimeFormatting
import com.retailone.pos.utils.NetworkUtils
import com.retailone.pos.viewmodels.DashboardViewodel.SalesPaymentViewmodel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class SalesPaymentDetailsActivity : AppCompatActivity() {

    lateinit var  binding: ActivitySalesPaymentDetailsBinding
    lateinit var viewmodel: SalesPaymentViewmodel
    lateinit var salesDetailsAdapter: SalesDetailsAdapter
    lateinit var  localizationData: LocalizationData


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesPaymentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewmodel = ViewModelProvider(this)[SalesPaymentViewmodel::class.java]
        localizationData = LocalizationHelper(this).getLocalizationData()
        
        // ✅ Initialize repository for offline support
        viewmodel.initRepository(this)
        
        enableBackButton()
        setToolbarImage()
        prepareRecycleview()
        
        val saleid = intent?.getStringExtra("sale_id")
        saleid?.let { id ->
            val saleIdInt = id.toIntOrNull() ?: 0
            
            lifecycleScope.launch {
                // ✅ NEW: First check if this is an offline sale
                val isOffline = viewmodel.isOfflineSale(this@SalesPaymentDetailsActivity, saleIdInt)
                Log.d("SalesPaymentDetails", "Is offline sale: $isOffline")
                
                if (isOffline) {
                    // ✅ OFFLINE SALE: Load from pending_sales table directly
                    Log.d("SalesPaymentDetails", "📴 Loading offline sale details...")
                    val offlineDetails = viewmodel.getOfflineSaleDetails(this@SalesPaymentDetailsActivity, saleIdInt)
                    if (offlineDetails != null) {
                        Log.d("SalesPaymentDetails", "✅ Loaded offline sale details")
                        displaySalesDetails(offlineDetails)
                    } else {
                        Log.e("SalesPaymentDetails", "❌ Failed to load offline sale details")
                        showMessage("Error loading offline sale details")
                    }
                } else {
                    // ✅ ONLINE SALE: Load from cache first (instant, works offline!)
                    val cachedDetails = viewmodel.getCachedSalesDetails(saleIdInt)
                    if (cachedDetails != null) {
                        Log.d("SalesPaymentDetails", "✅ Loaded sale details from cache")
                        displaySalesDetails(cachedDetails)
                    } else {
                        Log.d("SalesPaymentDetails", "ℹ️ No cached data found for sale $saleIdInt")
                    }
                    
                    // ✅ Then call API to refresh if online
                    if (NetworkUtils.isInternetAvailable(this@SalesPaymentDetailsActivity)) {
                        viewmodel.callSalesDetailsApi(SalesDetailsReq(id),this@SalesPaymentDetailsActivity)
                    } else {
                        Log.d("SalesPaymentDetails", "📴 Offline - showing cached data only")
                        if (cachedDetails == null) {
                            showMessage("No offline data available. Please connect to the internet.")
                        }
                    }
                }
            }
        }

        viewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }

        viewmodel.salesdetails_liveData.observe(this){ salesDetailsRes ->

            if(salesDetailsRes.status==1){
                Log.d("SalesPaymentDetails", "✅ API response received")
                
                // ✅ Cache the response for offline use
                val saleid = intent?.getStringExtra("sale_id")
                saleid?.let { id ->
                    lifecycleScope.launch {
                        viewmodel.cacheSalesDetails(
                            id.toIntOrNull() ?: 0,
                            salesDetailsRes.data[0].invoice_id,
                            salesDetailsRes,
                            this@SalesPaymentDetailsActivity
                        )
                    }
                }
                
                displaySalesDetails(salesDetailsRes)
            }


        }

    }
    
    /**
     * Display sales details data on the screen (used for both cached and API data)
     */
    private fun displaySalesDetails(salesDetailsRes: SalesDetailsRes) {
        val salesdata = salesDetailsRes.data[0]

        // 🔍 Log the response to check discount_amount per item
        salesdata.sales_items.forEachIndexed { index, item ->
            Log.d("SalesDetails", "Item $index: ${item.product_name}")
            Log.d("SalesDetails", "  Sub Total: ${item.sub_total}")
            Log.d("SalesDetails", "  Total: ${item.total_amount}")
            // Check if discount_amount exists in the response
            Log.d("SalesDetails", "  Raw JSON: $item")
        }

        val formattedPrice = NumberFormatter().formatPrice(salesdata.grand_total.toString(),localizationData)
        val sum = salesdata.summary
        val subtotalValue = (sum?.total_sub_total ?: salesdata.sub_total)
        val taxValue      = (sum?.total_tax_amount ?: salesdata.tax_amount)
        val totalValue    = (salesdata.grand_total)

        val safeTax = taxValue ?: 0.0
        binding.apply {
            orderId.text = "ID: "+salesdata?.invoice_id?.toString()
            date.text = "Date: "+DateTimeFormatting.formatGlobalTime(salesdata.created_at,localizationData.timezone)
            grandtotal.text = "Grand total: $formattedPrice"
            paymenttype.text = "Payment type: "+salesdata.payment_type.toString()
            storename.text = "Store name: "+(salesdata.store_details.store_name ?: "")
            //  vat.text = "(+) Tax @"+(salesdata.tax?:"")+"%"+":   "+"ZWL"+(salesdata.tax_amount?:"")
            //customername.text = "Customer name: "+(salesdata.customer.customer_name?:"")
            customername.text = "Customer name: " + (salesdata.customer?.customer_name ?: "N/A")
            // binding.btnConfirmcancel.isVisible = salesdata.grand_total >= 0
            binding.btnConfirmcancel.isVisible =
                salesdata.grand_total >= 0 && salesdata.total_refunded_amount <= 0.0


            val roundedSubtotal = BigDecimal.valueOf(subtotalValue)
                .setScale(0, RoundingMode.HALF_UP)

            tvSubtotalValue.text = NumberFormatter().formatPrice(
                roundedSubtotal.toPlainString(),
                localizationData
            )
            //tvSubtotalValue.text = NumberFormatter().formatPrice(subtotalValue.toString(), localizationData)
            val roundedTaxStr = BigDecimal.valueOf(safeTax)
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString()

            tvTaxValue.text = NumberFormatter().formatPrice(
                roundedTaxStr,
                localizationData
            )
            tvTotalValue.text    = NumberFormatter().formatPrice(totalValue.toString(), localizationData)

        }

        salesDetailsAdapter = SalesDetailsAdapter(this,salesdata)

        binding.itemsRcv.adapter = salesDetailsAdapter


        binding.btnConfirmcancel.setOnClickListener {
            //var invoiceId = binding.orderId.text.toString().trim()
            val invoiceIdRaw = binding.orderId.text.toString().trim()
            val invoiceId = invoiceIdRaw.replace("ID:", "").trim()  // ✅ clean prefix


            if (invoiceId.isEmpty()) {
                Toast.makeText(this, "Invoice ID is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = CancelSaleitemRequest(
                invoiceID = invoiceId

            )
            Log.d("CancelSale", "Calling API with invoiceId: $invoiceId")

            viewmodel.callCancelSaleAPI(request, this)
        }
    }



    private fun showMessage(msg: String) {
        Toast.makeText(this@SalesPaymentDetailsActivity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun prepareRecycleview() {
        binding.itemsRcv.apply {
            layoutManager = LinearLayoutManager(this@SalesPaymentDetailsActivity,
                RecyclerView.VERTICAL,false)

        }
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