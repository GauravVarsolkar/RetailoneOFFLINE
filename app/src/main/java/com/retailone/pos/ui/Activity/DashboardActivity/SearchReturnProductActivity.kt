package com.retailone.pos.ui.Activity.DashboardActivity

import NumberFormatter
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.retailone.pos.adapter.ReturnSalesItemBatchAdapter
import com.retailone.pos.adapter.SalesListAdapter
import com.retailone.pos.databinding.ActivityReturnSaleBinding
import com.retailone.pos.databinding.ActivitySearchReturnProductBinding
import com.retailone.pos.interfaces.OnReturnQuantityChangeListener
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalReturnCartHelper
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.ReplaceModel.ReplaceReturnedItem
import com.retailone.pos.models.ReturnSalesItemModel.BatchReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnSaleReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleReqModel.ReturnedItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnSaleRes
import com.retailone.pos.models.ReturnSalesItemModel.SalesItem
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData
import com.retailone.pos.ui.Activity.MPOSDashboardActivity
import com.retailone.pos.utils.NetworkUtils
import com.retailone.pos.utils.PrinterUtil
import com.retailone.pos.viewmodels.DashboardViewodel.ReturnSalesDetailsViewmodel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.math.RoundingMode

import kotlin.math.log

class SearchReturnProductActivity : AppCompatActivity(), OnReturnQuantityChangeListener {

    lateinit var binding: ActivitySearchReturnProductBinding
    lateinit var returnsale_viewmodel: ReturnSalesDetailsViewmodel
    lateinit var returnSalesItemAdapter: ReturnSalesItemAdapter
    lateinit var salesListAdapter: SalesListAdapter
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
        binding = ActivitySearchReturnProductBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.relativeLayout.isVisible = false
        binding.relativeLayout2.isVisible = false
        val invoiceIdFromIntent = intent.getStringExtra("invoice_id")
        returnsale_viewmodel = ViewModelProvider(this)[ReturnSalesDetailsViewmodel::class.java]
        returnsale_viewmodel.initRepository(this)
        localizationData = LocalizationHelper(this).getLocalizationData()
        val loginSession = LoginSession.getInstance(this)

        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first().toInt()
            store_manager_id = loginSession.getStoreManagerID().first().toString()

            // ✅ STEP 1: Load return reasons FIRST (before displaying any sale)
            val cachedReasons = returnsale_viewmodel.getReturnReasonsFromLocalDB()
            if (cachedReasons.isNotEmpty()) {
                Log.d("ReturnReasons", "✅ Loaded ${cachedReasons.size} reasons from cache")
                returnReasonList = cachedReasons.toMutableList()
                binding.reasonInput.setAdapter(ReturnReasonAdapter(this@SearchReturnProductActivity, 0, returnReasonList))
            } else {
                Log.w("ReturnReasons", "⚠️ No reasons in cache!")
            }

            // ✅ STEP 2: THEN load and display the sale (reasons are now available)
            if (!invoiceIdFromIntent.isNullOrEmpty()) {
                binding.searchBar.setQuery(invoiceIdFromIntent, false)

                // ✅ Check if online or offline
                if (NetworkUtils.isInternetAvailable(this@SearchReturnProductActivity)) {
                    // Online: Direct API call
                    returnsale_viewmodel.callReturnSalesDetailsApi(
                        ReturnItemReq(invoice_id = invoiceIdFromIntent),
                        this@SearchReturnProductActivity
                    )
                } else {
                    // Offline: Load from cache
                    val cachedSale = returnsale_viewmodel.getDetailedSaleFromLocalDB(invoiceIdFromIntent)
                    if (cachedSale != null) {
                        Log.d("SearchReturn", "✅ Loaded from offline cache: $invoiceIdFromIntent")
                        displaySaleDetails(cachedSale)
                    } else {
                        showMessage("No cached data available for this invoice. Please connect to internet.")
                    }
                }
            }

            // ✅ STEP 3: Cleanup old data
            returnsale_viewmodel.cleanupOldDetailedSales()
        }


        binding.addcart.setOnClickListener {
            returnbatchItemList.clear()
            if (binding.searchBar.query.toString().trim() == "") {
                showMessage("Enter a valid Invoice ID")
            } else {
                val invoiceId = binding.searchBar.query.toString().trim()

                // ✅ Check if online or offline
                if (NetworkUtils.isInternetAvailable(this)) {
                    // Online: Direct API call
                    returnsale_viewmodel.callReturnSalesDetailsApi(
                        ReturnItemReq(invoice_id = invoiceId),
                        this@SearchReturnProductActivity
                    )
                } else {
                    // Offline: Load from cache
                    lifecycleScope.launch {
                        val cachedSale = returnsale_viewmodel.getDetailedSaleFromLocalDB(invoiceId)
                        if (cachedSale != null) {
                            Log.d("SearchReturn", "✅ Loaded from offline cache: $invoiceId")
                            displaySaleDetails(cachedSale)
                        } else {
                            showMessage("No cached data available for this invoice. Please connect to internet.")
                        }
                    }
                }
            }
        }

        binding.addproductLayout.setOnClickListener {
            showMessage("Enter Invoice ID and Search")
        }

        returnsale_viewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        printerUtil = PrinterUtil(this)
        enableBackButton()
        preparePositemRCV()

        returnsale_viewmodel.callSaleReturnReasonApi(this)

        returnsale_viewmodel.returnitem_liveData.observe(this) {
            if (it.data.isNotEmpty()) {
                val data = it.data[0]
                Log.d("INITIAL_TAX_DEBUG", "========== API RESPONSE ==========")
                Log.d("INITIAL_TAX_DEBUG", "invoice_id: ${data.invoice_id}")
                Log.d("INITIAL_TAX_DEBUG", "sub_total: ${data.sub_total}")
                Log.d("INITIAL_TAX_DEBUG", "tax (String): '${data.tax}'")
                Log.d("INITIAL_TAX_DEBUG", "tax_amount (String): '${data.tax_amount}'")
                Log.d("INITIAL_TAX_DEBUG", "tax_amount (toDouble): ${data.tax_amount.toDoubleOrNull()}")
                Log.d("INITIAL_TAX_DEBUG", "grand_total: ${data.grand_total}")
                Log.d("INITIAL_TAX_DEBUG", "total_refunded_amount: ${data.total_refunded_amount}")
                Log.d("INITIAL_TAX_DEBUG", "==================================")

                lifecycleScope.launch {
                    returnsale_viewmodel.saveDetailedSaleToLocalDB(data)
                    Log.d("SearchReturn", "💾 Saved to cache: ${data.invoice_id}")
                }

                // ✅ Use displaySaleDetails for consistency
                displaySaleDetails(data)

            } else {
                showMessage("No Invoice Found")

                binding.positemRcv.isVisible = false
                binding.addproductLayout.isVisible = true
                binding.reasonLayout.isVisible = false
                binding.summaryCard.isVisible = false
                binding.paymentcard.isVisible = false
                binding.relativeLayout.isVisible = false
                binding.relativeLayout2.isVisible = false
            }
        }

        returnsale_viewmodel.returnsalesubmit_liveData.observe(this) {
            if (it.status == 1) {
                lifecycleScope.launch {
                    val grandTotal = returnItemData.grand_total.toDoubleOrNull() ?: 0.0
                    returnsale_viewmodel.updateSaleRefundedAmount(
                        returnItemData.invoice_id,
                        grandTotal,
                        reasonid
                    )
                    Log.d("OnlineReturn", "💾 Updated cache for ${returnItemData.invoice_id} as refunded")
                }

                showSucessDialog(it.message, it)
            } else {
                showMessage(it.message)
            }
        }

        returnsale_viewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        setToolbarImage()

        returnsale_viewmodel.salesreturnreason_liveData.observe(this) {
            returnReasonList = it.data.toMutableList()
            binding.reasonInput.setAdapter(ReturnReasonAdapter(this, 0, returnReasonList))
            Log.d("ReturnReasons", "🔄 Updated with ${it.data.size} reasons from API")
        }

        binding.reasonInput.setOnClickListener {
            if (returnReasonList.isEmpty()) {
                showMessage("Return reason not found, try after sometime")
            }
        }

        binding.reasonInput.setOnItemClickListener { parent, view, position, id ->
            binding.reasonInput.setText(returnReasonList[position].reason_name, false)
            reasonid = returnReasonList[position].id
        }

        binding.nextlayout.setOnClickListener {
            if (returnbatchItemList.isNotEmpty()) {
                callReturnAPI(returnbatchItemList)
            } else {
                showMessage("You haven't Return anything")
            }
        }
    }

    // ✅ NEW: Unified display method for both online and offline
    private fun displaySaleDetails(data: ReturnItemData) {
        Log.d("DISPLAY_SALE", "========== DISPLAYING SALE ==========")
        Log.d("DISPLAY_SALE", "invoice_id: ${data.invoice_id}")
        Log.d("DISPLAY_SALE", "total_refunded_amount: ${data.total_refunded_amount}")
        Log.d("DISPLAY_SALE", "reason_id: ${data.reason_id}")
        Log.d("DISPLAY_SALE", "====================================")

        if (data.total_refunded_amount > 0) {
            // ✅ SALE ALREADY RETURNED - Fetch reason name and update UI
            lifecycleScope.launch {
                Log.d("DISPLAY_SALE", "🔍 Sale is refunded, fetching reason for ID: ${data.reason_id}")

                val reasonName = if (data.reason_id > 0) {
                    val fetchedReason = returnsale_viewmodel.getReasonNameById(data.reason_id)
                    Log.d("DISPLAY_SALE", "✅ Fetched reason: '$fetchedReason'")
                    fetchedReason
                } else {
                    Log.w("DISPLAY_SALE", "⚠️ reason_id is ${data.reason_id}, showing 'Not Given'")
                    "Not Given"
                }

                // ✅ Update UI on main thread with reason name
                withContext(Dispatchers.Main) {
                    returnItemData = data
                    returnItemList = data.salesItems.toMutableList()

                    // ✅ Pass reason name to adapter with named parameter
                    returnSalesItemAdapter = ReturnSalesItemAdapter(
                        returnitem = listOf(data),
                        context = this@SearchReturnProductActivity,
                        onReturnQuantityChangeListener = this@SearchReturnProductActivity,
                        returnReasonName = reasonName,  // ✅ Pass the fetched reason name
                        onBatchChange = {
                            Log.d("rtn", it.toString())
                            returnbatchItemList = it.toMutableList()
                        }
                    )

                    binding.positemRcv.adapter = returnSalesItemAdapter
                    binding.positemRcv.isVisible = true
                    binding.addproductLayout.isVisible = false
                    binding.reasonLayout.isVisible = false

                    // ✅ Show summary card
                    binding.summaryCard.isVisible = true
                    updateSummaryCard(
                        subtotal = data.sub_total,
                        tax = data.tax_amount.toDoubleOrNull() ?: 0.0,
                        total = data.grand_total.toDoubleOrNull() ?: 0.0
                    )

                    // ❌ Hide old payment card and bottom layouts
                    binding.paymentcard.isVisible = false
                    binding.relativeLayout.isVisible = false
                    binding.relativeLayout2.isVisible = false

                    // ✅ Show toast with reason
                    showMessage("This invoice has already been returned. Reason: $reasonName")
                }
            }

        } else {
            // ✅ NORMAL RETURN FLOW
            returnItemData = data
            returnItemList = data.salesItems.toMutableList()

            // ✅ Pass default "Not Given" for normal returns (no reason yet)
            returnSalesItemAdapter = ReturnSalesItemAdapter(
                returnitem = listOf(data),
                context = this@SearchReturnProductActivity,
                onReturnQuantityChangeListener = this,
                returnReasonName = "Not Given",  // ✅ No reason yet for new returns
                onBatchChange = {
                    Log.d("rtn", it.toString())
                    returnbatchItemList = it.toMutableList()
                }
            )

            binding.positemRcv.adapter = returnSalesItemAdapter
            binding.positemRcv.isVisible = true
            binding.addproductLayout.isVisible = false
            binding.reasonLayout.isVisible = true

            // ❌ HIDE new summary card for normal returns
            binding.summaryCard.isVisible = false

            // ✅ Show bottom layouts
            binding.relativeLayout.isVisible = true
            binding.relativeLayout2.isVisible = true
            binding.paymentcard.isVisible = false

            val subtotalValue = data.sub_total
            val taxValue = data.tax_amount.toDoubleOrNull() ?: 0.0
            val grandValue = data.grand_total.toDoubleOrNull() ?: 0.0

            val roundedSubtotal = BigDecimal.valueOf(subtotalValue)
                .setScale(0, RoundingMode.HALF_UP)

            val roundedTax = BigDecimal.valueOf(taxValue)
                .setScale(0, RoundingMode.HALF_UP)

            binding.subtotal.setText(roundedSubtotal.toPlainString())

            val taxDisplay = formatTaxForDisplay(data.tax)
            binding.taxfield.setText("(+) Tax @$taxDisplay")

            binding.taxAmount.setText(roundedTax.toPlainString())
            binding.alltotalAmount.setText(
                NumberFormatter().formatPrice(grandValue.toString(), localizationData)
            )
        }

        // ✅ Logging
        Log.d("VISIBILITY_DEBUG", "summaryCard.isVisible = ${binding.summaryCard.isVisible}")
        Log.d("VISIBILITY_DEBUG", "paymentcard.isVisible = ${binding.paymentcard.isVisible}")
        Log.d("VISIBILITY_DEBUG", "relativeLayout.isVisible = ${binding.relativeLayout.isVisible}")
        Log.d("VISIBILITY_DEBUG", "relativeLayout2.isVisible = ${binding.relativeLayout2.isVisible}")
    }

    /**
     * ✅ NEW: Updates the summary card with subtotal, tax, and total
     */
    private fun updateSummaryCard(subtotal: Double, tax: Double, total: Double, discount: Double = 0.0) {
        // Round values
        val roundedSubtotal = BigDecimal.valueOf(subtotal).setScale(0, RoundingMode.HALF_UP)
        val roundedTax = BigDecimal.valueOf(tax).setScale(0, RoundingMode.HALF_UP)
        val roundedTotal = BigDecimal.valueOf(total).setScale(0, RoundingMode.HALF_UP)

        // Update summary card TextViews
        binding.tvSubtotalValue.text = NumberFormatter().formatPrice(
            roundedSubtotal.toPlainString(),
            localizationData
        )

        binding.tvTaxValue.text = NumberFormatter().formatPrice(
            roundedTax.toPlainString(),
            localizationData
        )

        binding.tvTotalValue.text = NumberFormatter().formatPrice(
            roundedTotal.toPlainString(),
            localizationData
        )

        // Show/hide discount row
        if (discount > 0.0) {
            binding.discountSummaryRow.isVisible = true
            binding.tvDiscountValue.text = NumberFormatter().formatPrice(
                String.format(Locale.US, "%.2f", discount),
                localizationData
            )
        } else {
            binding.discountSummaryRow.isVisible = false
        }
    }

    // ✅ UPDATED: Converts "16.5", "16,5", "16.50%", or "165" -> rounded integer like "18"
    private fun formatTaxForDisplay(raw: Any?): String {
        val s0 = raw?.toString()?.trim().orEmpty()
        if (s0.isEmpty()) return "0"

        // keep digits and one decimal separator; accept comma or dot
        val s1 = s0.replace(Regex("[^0-9.,]"), "").replace(',', '.')
        if (s1.isEmpty() || s1 == ".") return "0"

        // already has decimal separator — format nicely
        if (s1.contains('.')) {
            return try {
                val value = BigDecimal(s1)
                // ✅ Round to nearest integer
                val rounded = value.setScale(0, RoundingMode.HALF_UP)
                rounded.toPlainString()
            } catch (_: Exception) {
                s1
            }
        }

        // no decimal point -> legacy value (e.g., 165 should be 16.5)
        val n = s1.toLongOrNull() ?: return s1

        // Heuristic: older code multiplied % by 10 (16.5 -> 165)
        val scaled = n / 10.0

        // ✅ Round to nearest integer
        return BigDecimal.valueOf(scaled).setScale(0, RoundingMode.HALF_UP).toPlainString()
    }

    /** Purchased (sold) qty for a given sales_item_id (ceil). */
    private fun purchasedQtyFor(salesItemId: Int): Int {
        val q = returnItemData.sales_items.orEmpty().firstOrNull { it.id == salesItemId }?.quantity
            ?: 0.0
        return kotlin.math.ceil(q).toInt()
    }

    private fun buildReplaceLines(): List<ReturnedItem> {

        // 1️⃣ Aggregate defect info per sales_item_id from live batch list
        val defectMap: Map<Int, Pair<Int, Int>> = returnbatchItemList
            .groupBy { it.sales_item_id }
            .mapValues { (_, batches) ->
                val totalBoxes = batches.sumOf { it.defective_boxes ?: 0 }
                val totalPacks = batches.sumOf { it.defective_bottles ?: 0 }
                totalBoxes to totalPacks
            }

        val output = mutableListOf<ReturnedItem>()

        // 2️⃣ Items saved in local cart (user pressed pencil/save) → always send
        val cartLines = LocalReturnCartHelper.getCartItems(this)
        if (cartLines.isNotEmpty()) {

            cartLines.forEach { line ->
                val (boxes, packs) = defectMap[line.id] ?: (0 to 0)

                output.add(
                    ReturnedItem(
                        id = line.id,
                        return_quantity = line.return_quantity,  // qty user confirmed
                        defective_boxes = boxes,                // total boxes from all batches
                        defective_bottles = packs               // total packs from all batches
                    )
                )
            }
            return output
        }

        // 3️⃣ No cart, but we have edits in batches → derive qty + defects from batches
        if (defectMap.isNotEmpty()) {
            // sales_item_id -> total Qty from UI
            val qtyMap = returnbatchItemList
                .groupBy { it.sales_item_id }
                .mapValues { (_, batches) ->
                    batches.sumOf { it.return_quantity ?: 0 }
                }

            defectMap.forEach { (salesItemId, boxesPacks) ->
                val (boxes, packs) = boxesPacks
                val userQty = qtyMap[salesItemId] ?: 0

                output.add(
                    ReturnedItem(
                        id = salesItemId,
                        return_quantity = userQty,
                        defective_boxes = boxes,
                        defective_bottles = packs
                    )
                )
            }

            // If we want to send only rows where user actually entered something:
            return output.filter {
                (it.defective_boxes ?: 0) > 0 ||
                        (it.defective_bottles ?: 0) > 0 ||
                        (it.return_quantity ?: 0) > 0
            }
        }

        // 4️⃣ Fallback: no edits at all → return full invoice with 0 defects
        val detailed = returnItemData.sales_items.orEmpty()
        detailed.forEach { si ->
            output.add(
                ReturnedItem(
                    id = si.id,
                    return_quantity = kotlin.math.ceil(si.quantity).toInt(),
                    defective_boxes = 0,
                    defective_bottles = 0
                )
            )
        }

        return output
    }

    private fun callReturnAPI(returnbatchItemList: MutableList<BatchReturnItem>) {
        val savedItems = buildReplaceLines()
        if (savedItems.isEmpty()) {
            showMessage("No items saved for return.")
            return
        }
        if (reasonid != -1) {
            val return_data = ReturnSaleReq(
                store_id = storeid,
                store_manager_id = store_manager_id.toInt(),
                reason_id = reasonid,
                sales_id = returnItemData.id,
                returned_items = savedItems
            )

            if (NetworkUtils.isInternetAvailable(this)) {
                Log.d("ReturnSubmit", "📡 Online - submitting immediately")
                returnsale_viewmodel.callReturnSalesSubmitApi(
                    return_data, this@SearchReturnProductActivity
                )
            } else {
                Log.d("ReturnSubmit", "📴 Offline - queuing for later sync")
                lifecycleScope.launch {
                    val queueId = returnsale_viewmodel.queueReturnRequest(return_data)

                    if (queueId > 0) {
                        showOfflineSuccessDialog()
                    } else {
                        showMessage("Failed to queue return request")
                    }
                }
            }

            LocalReturnCartHelper.clearCart(this)
        } else {
            showMessage("please select any reason for return")
        }
    }

    private fun showOfflineSuccessDialog() {
        lifecycleScope.launch {
            val grandTotal = returnItemData.grand_total.toDoubleOrNull() ?: 0.0

            Log.d("OfflineReturn", "🔍 BEFORE SAVE: invoice=${returnItemData.invoice_id}, reason=$reasonid")

            // ✅ STEP 1: Update cache with refunded amount AND reason_id
            returnsale_viewmodel.updateSaleRefundedAmount(
                returnItemData.invoice_id,
                grandTotal,
                reasonid
            )

            Log.d("OfflineReturn", "💾 Called updateSaleRefundedAmount with reason=$reasonid")

            // ✅ STEP 2: Wait for DB write to complete
            kotlinx.coroutines.delay(200)  // Increase delay

            // ✅ STEP 3: Reload sale from cache to get updated data with reason_id
            val updatedSale = returnsale_viewmodel.getDetailedSaleFromLocalDB(returnItemData.invoice_id)

            if (updatedSale != null) {
                Log.d("OfflineReturn", "🔄 AFTER RELOAD: reason_id=${updatedSale.reason_id}")

                // ✅ Test: Try to get reason name immediately
                val testReasonName = returnsale_viewmodel.getReasonNameById(updatedSale.reason_id)
                Log.d("OfflineReturn", "🧪 TEST: Fetched reason name = '$testReasonName'")

                // ✅ STEP 4: Update the in-memory data
                returnItemData = updatedSale

                // ✅ STEP 5: Display updated sale details
                displaySaleDetails(updatedSale)
            } else {
                Log.e("OfflineReturn", "❌ Failed to reload sale from cache!")
            }
        }

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.pos_sucess_dialog)
        dialog.setCancelable(false)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(false)

        val confirm = dialog.findViewById<MaterialButton>(R.id.prefer_confirm)
        val logoutMsg = dialog.findViewById<TextView>(R.id.logout_msg)
        val print_receipt = dialog.findViewById<MaterialButton>(R.id.print_receipt)

        logoutMsg.text = "Return request saved offline.\nIt will be submitted when internet is available."
        logoutMsg.textSize = 16F

        print_receipt.isVisible = false

        confirm.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this@SearchReturnProductActivity, MPOSDashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    private fun getReturnDateTime(): String {

        val zone = localizationData.timezone
        lateinit var timezone: String

        if (zone == "IST") {
            timezone = "Asia/Kolkata"
        } else if (zone == "CAT") {
            timezone = "Africa/Lusaka"
        } else {
            timezone = "Africa/Lusaka"
        }

        val calendar = Calendar.getInstance()

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

        Glide.with(this).load(organisation_data.image_url + organisation_data.fabicon)
            .fitCenter()
            .placeholder(R.drawable.mlogo)
            .error(R.drawable.mlogo)
            .into(binding.image)
    }

    private fun preparePositemRCV() {

        binding.positemRcv.apply {
            layoutManager = LinearLayoutManager(
                this@SearchReturnProductActivity, RecyclerView.VERTICAL, false
            )
        }
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
        Toast.makeText(this@SearchReturnProductActivity, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onReturnQuantityChange(position: Int, newQuantity: Int) {
        Log.d("QUANTITY_CHANGE_DEBUG", "========== onReturnQuantityChange ==========")
        Log.d("QUANTITY_CHANGE_DEBUG", "position: $position")
        Log.d("QUANTITY_CHANGE_DEBUG", "newQuantity: $newQuantity")
        returnItemList[position].return_quantity = newQuantity
        returnItemList[position].refund_amount = newQuantity * returnItemList[position].retail_price

        Log.d("QUANTITY_CHANGE_DEBUG", "Calling recalculateTotals()")
        recalculateTotals()
    }

    // ✅ UPDATED: Now properly checks for items and shows correct totals
    private fun recalculateTotals() {
        var subtotal = 0.0
        var taxAmount = 0.0

        // ❌ DON'T show summary card here (only for already returned invoices)
        // binding.summaryCard.isVisible = false  // Keep it hidden

        // ✅ Keep showing bottom layouts
        binding.relativeLayout.isVisible = true
        binding.relativeLayout2.isVisible = true

        Log.d("TAX_FIX_DEBUG", "========== recalculateTotals() START ==========")
        Log.d("TAX_FIX_DEBUG", "returnItemList.size: ${returnItemList.size}")

        // Check if any items have been selected for return
        val hasReturnItems = returnItemList.any {
            !it.readonlyMode && !it.isExpired && it.return_quantity > 0
        }

        if (hasReturnItems) {
            // User has selected items - calculate based on selected items
            returnItemList.forEachIndexed { index, item ->
                Log.d("TAX_FIX_DEBUG", "--- Item $index ---")
                Log.d("TAX_FIX_DEBUG", "  product_name: ${item.product_name}")
                Log.d("TAX_FIX_DEBUG", "  retail_price: ${item.retail_price}")
                Log.d("TAX_FIX_DEBUG", "  tax_exclusive_price: ${item.tax_exclusive_price}")
                Log.d("TAX_FIX_DEBUG", "  return_quantity: ${item.return_quantity}")
                Log.d("TAX_FIX_DEBUG", "  readonlyMode: ${item.readonlyMode}")
                Log.d("TAX_FIX_DEBUG", "  isExpired: ${item.isExpired}")

                if (!item.readonlyMode && !item.isExpired && item.return_quantity > 0) {
                    val taxExclusivePrice = item.tax_exclusive_price ?: item.retail_price
                    val qty = item.return_quantity

                    val itemSubtotal = taxExclusivePrice * qty
                    subtotal += itemSubtotal

                    val itemTax = (item.retail_price - taxExclusivePrice) * qty
                    taxAmount += itemTax

                    Log.d("TAX_FIX_DEBUG", "  ✅ INCLUDED in calculation")
                    Log.d("TAX_FIX_DEBUG", "  taxExclusivePrice: $taxExclusivePrice")
                    Log.d("TAX_FIX_DEBUG", "  itemSubtotal: $itemSubtotal")
                    Log.d("TAX_FIX_DEBUG", "  itemTax: $itemTax")
                } else {
                    Log.d("TAX_FIX_DEBUG", "  ❌ SKIPPED")
                }
            }

            Log.d("TAX_FIX_DEBUG", "CALCULATED subtotal: $subtotal")
            Log.d("TAX_FIX_DEBUG", "CALCULATED taxAmount: $taxAmount")

            val roundedSubtotal = BigDecimal.valueOf(subtotal)
                .setScale(0, RoundingMode.HALF_UP)

            val roundedTax = BigDecimal.valueOf(taxAmount)
                .setScale(0, RoundingMode.HALF_UP)

            val grandTotal = roundedSubtotal.add(roundedTax)

            binding.subtotal.setText(roundedSubtotal.toPlainString())
            binding.taxAmount.setText(roundedTax.toPlainString())
            binding.alltotalAmount.setText(grandTotal.toPlainString())

        } else {
            // No items selected - show original invoice totals
            Log.d("TAX_FIX_DEBUG", "No items selected - showing original invoice totals")

            val subtotalValue = returnItemData.sub_total
            val taxValue = returnItemData.tax_amount.toDoubleOrNull() ?: 0.0
            val grandValue = returnItemData.grand_total.toDoubleOrNull() ?: 0.0

            val roundedSubtotal = BigDecimal.valueOf(subtotalValue)
                .setScale(0, RoundingMode.HALF_UP)

            val roundedTax = BigDecimal.valueOf(taxValue)
                .setScale(0, RoundingMode.HALF_UP)

            binding.subtotal.setText(roundedSubtotal.toPlainString())
            binding.taxAmount.setText(roundedTax.toPlainString())
            binding.alltotalAmount.setText(
                NumberFormatter().formatPrice(grandValue.toString(), localizationData)
            )
        }

        Log.d("TAX_FIX_DEBUG", "========== recalculateTotals() END ==========")
    }

    private fun showSucessDialog(msg: String, returnSaleRes: ReturnSaleRes) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.pos_sucess_dialog)
        dialog.setCancelable(false)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(false)

        val confirm = dialog.findViewById<MaterialButton>(R.id.prefer_confirm)
        val logoutMsg = dialog.findViewById<TextView>(R.id.logout_msg)
        val logoutImg = dialog.findViewById<ImageView>(R.id.dialog_logo)
        val print_receipt = dialog.findViewById<MaterialButton>(R.id.print_receipt)

        logoutMsg.text = msg
        logoutMsg.textSize = 16F

        confirm.setOnClickListener {
            dialog.dismiss()

            val intent = Intent(this@SearchReturnProductActivity, MPOSDashboardActivity::class.java)
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
