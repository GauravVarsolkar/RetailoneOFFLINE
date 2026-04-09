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
import com.retailone.pos.adapter.ReplaceSalesItemAdapter
import com.retailone.pos.adapter.ReturnReasonAdapter
import com.retailone.pos.databinding.ActivitySearchReplaceProductBinding
import com.retailone.pos.interfaces.OnReturnQuantityChangeListener
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalReturnCartHelper
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.ReplaceModel.ReplaceReturnedItem
import com.retailone.pos.models.ReplaceModel.ReplaceSaleReq
import com.retailone.pos.models.ReturnSalesItemModel.BatchReturnItem
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemData
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemReq
import com.retailone.pos.models.ReturnSalesItemModel.ReturnItemRes
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnSaleRes
import com.retailone.pos.models.ReturnSalesItemModel.SalesItem
import com.retailone.pos.models.ReturnSalesItemModel.SalesReturnReasonModel.ReturnReasonData
import com.retailone.pos.ui.Activity.MPOSDashboardActivity
import com.retailone.pos.utils.PrinterUtil
import com.retailone.pos.utils.NetworkUtils
import com.retailone.pos.localstorage.RoomDB.PosDatabase

import com.retailone.pos.viewmodels.DashboardViewodel.ReturnSalesDetailsViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

class SearchReplaceProductActivity : AppCompatActivity(), OnReturnQuantityChangeListener {

    private var canReplaceFlag: Boolean = false
    lateinit var binding: ActivitySearchReplaceProductBinding
    lateinit var returnsale_viewmodel: ReturnSalesDetailsViewmodel
    lateinit var returnSalesItemAdapter: ReplaceSalesItemAdapter

    var returnItemList = mutableListOf<SalesItem>()
    var returnReasonList: MutableList<ReturnReasonData> = mutableListOf()
    lateinit var returnItemData: ReturnItemData

    var reasonid = -1
    var storeid = 0
    var store_manager_id = "0"
    lateinit var localizationData: LocalizationData
    private var printerUtil: PrinterUtil? = null

    /** ✅ Replace flow selections are POSITION-KEYED to avoid id=0 collisions */
    private val selectedParentPositions: MutableSet<Int> = linkedSetOf()
    private val selectedBatchesByRow: MutableMap<Int, MutableMap<String, BatchReturnItem>> =
        linkedMapOf()

    // stock maps
    private val storeStockBySalesItemId = mutableMapOf<Int, Int>()
    private val storeStockByProductId = mutableMapOf<Int, Int>()
    private val saleQuantityByProductId = mutableMapOf<Int, Double>()
    private val onHoldByProductId = mutableMapOf<Int, Int>()

    private var isInvoiceAlreadyReplaced: Boolean = false

    private var isHoldAction = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchReplaceProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.relativeLayout.isVisible = false
        binding.relativeLayout2.isVisible = false

        val invoiceIdFromIntent = intent.getStringExtra("invoice_id")

        returnsale_viewmodel = ViewModelProvider(this)[ReturnSalesDetailsViewmodel::class.java]
        localizationData = LocalizationHelper(this).getLocalizationData()

        val loginSession = LoginSession.getInstance(this)
        lifecycleScope.launch {
            // ✅ Initialize repository for offline state management
            returnsale_viewmodel.initRepository(this@SearchReplaceProductActivity)

            // ✅ Load return reasons from local cache FIRST (so they are available offline)
            val cachedReasons = returnsale_viewmodel.getReturnReasonsFromLocalDB()
            if (cachedReasons.isNotEmpty()) {
                Log.d("ReplaceReasons", "✅ Loaded ${cachedReasons.size} reasons from cache")
                returnReasonList = cachedReasons.toMutableList()
                binding.reasonInput.setAdapter(ReturnReasonAdapter(this@SearchReplaceProductActivity, 0, returnReasonList))
            }

            storeid = loginSession.getStoreID().first().toInt()
            store_manager_id = loginSession.getStoreManagerID().first().toString()
            if (!invoiceIdFromIntent.isNullOrEmpty()) {
                binding.searchBar.setQuery(invoiceIdFromIntent, false)
                
                // ✅ Call unified API hub (handles both online/offline automatically)
                returnsale_viewmodel.callReturnSalesDetailsApi(
                    ReturnItemReq(invoice_id = invoiceIdFromIntent, store_id = storeid.toString()),
                    this@SearchReplaceProductActivity
                )
            }
        }

        binding.addcart.setOnClickListener {
            selectedParentPositions.clear()
            selectedBatchesByRow.clear()
            applyZeroTotals()
            val q = binding.searchBar.query.toString().trim()
            if (q.isEmpty()) {
                showMessage("Enter a valid Invoice ID")
            } else {
                // ✅ Call unified API hub (handles both online/offline automatically)
                returnsale_viewmodel.callReturnSalesDetailsApi(
                    ReturnItemReq(invoice_id = q, store_id = storeid.toString()), this@SearchReplaceProductActivity
                )
            }
        }

        binding.addproductLayout.setOnClickListener { showMessage("Enter Invoice ID and Search") }

        returnsale_viewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        printerUtil = PrinterUtil(this)
        enableBackButton()
        preparePositemRCV()
        returnsale_viewmodel.callSaleReturnReasonApi(this)

        // ===================== RESPONSE HANDLER =====================
        returnsale_viewmodel.returnitem_liveData.observe(this) { res ->
            if (res.data.isNotEmpty()) {
                val first = res.data[0]
                val detailedItems = if (first.sales_items.isNullOrEmpty()) {
                    first.salesItems.orEmpty().map {
                        com.retailone.pos.models.ReturnSalesItemModel.SalesItemDetailed(
                            id = it.id,
                            sales_id = it.sales_id,
                            product_id = it.product_id,
                            on_hold = 0,
                            distribution_pack_id = it.distribution_pack_id,
                            distribution_pack_name = it.distribution_pack_name,
                            batch = null,
                            quantity = it.quantity,
                            store_stock = 0,
                            retail_price = it.retail_price,
                            discount = 0.0,
                            tax_exclusive_price = it.tax_exclusive_price,
                            total_amount = it.total_amount,
                            product = it.product,
                            distribution_pack = it.distribution_pack,
                            sales_returns = null
                        )
                    }
                } else {
                    first.sales_items.orEmpty()
                }

                storeStockBySalesItemId.clear()
                storeStockByProductId.clear()
                onHoldByProductId.clear()

                // 1) sales_item_id -> store_stock
                detailedItems.forEach { si ->
                    storeStockBySalesItemId[si.id] = si.store_stock
                }

                // 2) product_id -> total store_stock + on_hold flag
                detailedItems
                    .groupBy { it.product_id }
                    .forEach { (productId, listForProduct) ->
                        val totalStockForProduct = listForProduct.sumOf { it.store_stock }
                        val productOnHold = if (listForProduct.any { it.on_hold == 1 }) 1 else 0
                        ////
                        val totalSaleQuantityForProduct = listForProduct.sumOf { it.quantity }

                        saleQuantityByProductId[productId] = totalSaleQuantityForProduct
                        ////

                        storeStockByProductId[productId] = totalStockForProduct
                        onHoldByProductId[productId] = productOnHold
                    }

                // 3) Log + invoice already replaced flag
                val totalReplacedAmount = first.total_replaced_amount ?: 0.0
                isInvoiceAlreadyReplaced = totalReplacedAmount > 0.0

                detailedItems.forEach { item ->
                    val productId = item.product_id
                    val productQuantity = item.quantity
                    val storeStockForProduct = storeStockByProductId[productId] ?: 0
                    val productOnHold = onHoldByProductId[productId] ?: 0

                    Log.d(
                        "ReplaceAdapter",
                        "productId=$productId, qty=$productQuantity, " +
                                "storeStock=$storeStockForProduct, productOnHold=$productOnHold, " +
                                "totalReplacedAmount=$totalReplacedAmount, " +
                                "isInvoiceAlreadyReplaced=$isInvoiceAlreadyReplaced"
                    )
                }

                // ✅ Special exception: at least 1 product is on hold and now has stock > 1
                val hasForceReplaceProduct = isInvoiceAlreadyReplaced &&
                        detailedItems.any { si ->
                            val stockForProduct = storeStockByProductId[si.product_id] ?: si.store_stock
                            //val totalSaleQuantityForProduct = saleQuantityByProductId[si.product_id]?: si.quantity

                            val onHold = onHoldByProductId[si.product_id] ?: si.on_hold
                            //// onHold == 1 && stockForProduct > 1 //old
                            ////  onHold == 1 && stockForProduct >= totalSaleQuantityForProduct
                            onHold == 1
                        }

                val onHoldAndStockAvailableReplace = isInvoiceAlreadyReplaced &&
                        detailedItems.any { si ->
                            val stockForProduct = storeStockByProductId[si.product_id] ?: si.store_stock
                            val totalSaleQuantityForProduct = saleQuantityByProductId[si.product_id]?: si.quantity

                            val onHold = onHoldByProductId[si.product_id] ?: si.on_hold
                            //// onHold == 1 && stockForProduct > 1 //old
                            onHold == 1 && stockForProduct >= totalSaleQuantityForProduct
                            /// onHold == 1
                        }

                // ✅ OFFLINE STOCK OVERRIDE:
                // When offline, si.store_stock in the cached SalesItemDetailed is stale (often 0).
                // Re-populate the stock maps from the local StoreProductDao so the on-hold
                // decision logic mirrors exactly what online mode does with live API stock.
                if (!NetworkUtils.isInternetAvailable(this@SearchReplaceProductActivity) && detailedItems.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            val productIds = detailedItems.map { it.product_id }.distinct()
                            val localProducts = PosDatabase.getDatabase(this@SearchReplaceProductActivity)
                                .storeProductDao()
                                .getProductsByProductIds(productIds, storeid)

                            // Build a fast lookup: product_id -> sum of stock_quantity across all packs
                            val localStockByProductId = localProducts
                                .groupBy { it.product_id }
                                .mapValues { (_, entities) -> entities.sumOf { it.stock_quantity }.toInt() }

                            // Also build lookup by compositeKey (product_id_distribution_pack_id)
                            val localStockByCompositeKey = localProducts.associateBy { it.compositeKey }

                            Log.d("OfflineReplaceStock", "Local stock map: $localStockByProductId")

                            // Override storeStockByProductId with local values
                            localStockByProductId.forEach { (productId, localStock) ->
                                storeStockByProductId[productId] = localStock
                            }

                            // Override storeStockBySalesItemId per sales item using composite key
                            detailedItems.forEach { si ->
                                val key = "${si.product_id}_${si.distribution_pack_id}"
                                val localEntity = localStockByCompositeKey[key]
                                if (localEntity != null) {
                                    storeStockBySalesItemId[si.id] = localEntity.stock_quantity.toInt()
                                    Log.d("OfflineReplaceStock", "Overridden sid=${si.id} product=${si.product_id} stock=${localEntity.stock_quantity}")
                                } else {
                                    // Fallback: use product-level sum if no exact pack match
                                    val fallbackStock = localStockByProductId[si.product_id] ?: 0
                                    storeStockBySalesItemId[si.id] = fallbackStock
                                    Log.d("OfflineReplaceStock", "Fallback sid=${si.id} product=${si.product_id} stock=$fallbackStock")
                                }
                            }

                            // Re-run visibility after overriding with real local stock
                            updateNextLayoutVisibilityForStock()
                            if (::returnSalesItemAdapter.isInitialized) {
                                returnSalesItemAdapter.notifyDataSetChanged()
                            }
                        } catch (e: Exception) {
                            Log.e("OfflineReplaceStock", "Failed to load local stock: ${e.message}")
                        }
                    }
                }

                updateNextLayoutVisibilityForStock()


                val data = res.data[0]
                val allItems = data.salesItems.orEmpty()
                
                // Keep returnItemList as full list for index consistency
                returnItemList = allItems.toMutableList()
                returnItemData = data

                // Logic to check if invoice is already replaced or all items refunded
                val isAlreadyReplaced = (data.total_replaced_amount ?: 0.0) > 0.0 || isInvoiceAlreadyReplaced
                val allRefunded = allItems.all { (data.total_refunded_amount ?: 0.0) > 0.0 } // Or check individual items if available

                if (isAlreadyReplaced || allRefunded) {
                    val msg = if (isAlreadyReplaced) "This invoice has already been replaced." else "All items are already refunded."
                    showMessage(msg)

                    returnSalesItemAdapter = ReplaceSalesItemAdapter(
                        res.data,
                        this@SearchReplaceProductActivity,
                        this,
                        selectedParentPositions = selectedParentPositions,
                        onBatchChange = { _, _ -> /* no-op */ },
                        onParentToggle = { _, _, _ -> /* no-op */ },
                        readOnly = true,
                        storeStockMap = storeStockByProductId,
                        onHoldMap = onHoldByProductId
                    )

                    binding.positemRcv.adapter = returnSalesItemAdapter
                    binding.positemRcv.isVisible = true
                    binding.addproductLayout.isVisible = false
                    binding.reasonLayout.isVisible = false
                    binding.summaryCard.isVisible = true
                    binding.relativeLayout2.isVisible = true
                    binding.paymentcard.isVisible = false
                    // ✅ Hide the action button — invoice is already replaced, no further action allowed
                    binding.relativeLayout.isVisible = false
                    binding.nextlayout.isVisible = false
                    setTotalsFromServer(data)
                    return@observe
                }


                // NORMAL REPLACEABLE FLOW
                selectedParentPositions.clear()
                selectedBatchesByRow.clear()
                applyZeroTotals()

                returnSalesItemAdapter = ReplaceSalesItemAdapter(
                    res.data,
                    this@SearchReplaceProductActivity,
                    this,
                    selectedParentPositions = selectedParentPositions,
                    onBatchChange = { adapterPos, currentSelectedBatches ->
                        mergeBatchesForRow(adapterPos, currentSelectedBatches)
                        recomputeTotalsUnion()
                        updateReplaceUi()
                    },
                    onParentToggle = { productId, checked, adapterPos ->
                        handleParentToggle(productId, checked, adapterPos)
                        recomputeTotalsUnion()
                        updateReplaceUi()
                    },
                    readOnly = false,
                    storeStockMap = storeStockByProductId,
                    onHoldMap = onHoldByProductId
                )

                binding.positemRcv.adapter = returnSalesItemAdapter
                binding.positemRcv.isVisible = true
                binding.addproductLayout.isVisible = false
                binding.reasonLayout.isVisible = true
                binding.remarkLayout.isVisible = true
                binding.summaryCard.isVisible = true
                binding.relativeLayout.isVisible = true
                binding.relativeLayout2.isVisible = true
                binding.paymentcard.isVisible = false

                val taxDisplay = formatTaxForDisplay(data.tax)
                binding.taxfield.setText("(+) Tax @$taxDisplay")
                updateReplaceUi()
            } else {
                showMessage("No Invoice Found")
                binding.positemRcv.isVisible = false
                binding.addproductLayout.isVisible = true
                binding.reasonLayout.isVisible = false
                binding.remarkLayout.isVisible = false
                binding.relativeLayout.isVisible = false
                binding.relativeLayout2.isVisible = false
            }
        }

        // =================== /RESPONSE HANDLER ===================

        returnsale_viewmodel.returnsalesubmit_liveData.observe(this) {
            if (it.status == 1) showSucessDialog(it.message, it) else showMessage(it.message)
        }

        returnsale_viewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        setToolbarImage()

        returnsale_viewmodel.salesreturnreason_liveData.observe(this) {
            returnReasonList = it.data.toMutableList()
            binding.reasonInput.setAdapter(ReturnReasonAdapter(this, 0, returnReasonList))
        }

        binding.reasonInput.setOnClickListener {
            if (returnReasonList.isEmpty()) showMessage("Replace reason not found, try after sometime")
        }

        binding.reasonInput.setOnItemClickListener { _, _, position, _ ->
            binding.reasonInput.setText(returnReasonList[position].reason_name, false)
            reasonid = returnReasonList[position].id
        }

        binding.nextlayout.setOnClickListener {
            Log.e("ReplaceSaleRequest", "Save/Hold Button Clicked. canReplaceFlag=$canReplaceFlag")

            if (reasonid == -1) {
                showMessage("Please select any reason for replacement")
                Log.e("ReplaceSaleRequest", "Failed: Reason not selected (-1)")
                return@setOnClickListener
            }

            // CHECK BUTTON TEXT to determine if this is HOLD action BEFORE building replace lines
            isHoldAction = (binding.next.text.toString().trim().equals("HOLD", ignoreCase = true))
            Log.e("ReplaceSaleRequest", "Determined isHoldAction=$isHoldAction from button text '${binding.next.text}'")

            val replaceLines = buildReplaceLines()
            Log.e("ReplaceSaleRequest", "buildReplaceLines returned ${replaceLines.size} items.")
            
            if (replaceLines.isEmpty()) {
                val detailedItemsSize = returnItemData.sales_items?.size ?: 0
                val salesItemsSize = returnItemData.salesItems?.size ?: 0
                val parentsSize = selectedParentPositions.size
                val batchesSize = selectedBatchesByRow.size
                Log.e("ReplaceSaleRequest", "Nothing to replace! state = detailedItems:$detailedItemsSize, salesItems:$salesItemsSize, parents:$parentsSize, batches:$batchesSize")
                
                showMessage("Nothing to replace")
                return@setOnClickListener
            }

            val onHold = if (canReplaceFlag) 0 else 1

            Log.e("ReplaceSaleRequest", "Proceeding with replace/hold. onHold=$onHold")

            val req = ReplaceSaleReq(
                sales_id = returnItemData.id,
                reason_id = reasonid,
                store_id = storeid,
                store_manager_id = store_manager_id.toInt(),
                remark = binding.remarkInput.text.toString(),
                on_hold = onHold,
                return_date_time = getReturnDateTime(),
                returned_items = replaceLines
            )

            Log.e("ReplaceSaleRequest", "Request: $req")
            Log.e("ReplaceSaleRequest", "Request: ${Gson().toJson(req)}")

            if (NetworkUtils.isInternetAvailable(this@SearchReplaceProductActivity)) {
                Log.d("ReplaceSubmit", "📡 Online - submitting immediately")
                // Even online, setting local flag ensures instant consistency
                if (isHoldAction) {
                   com.retailone.pos.localstorage.SharedPreference.OnHoldInvoiceHelper.markAsOnHold(this@SearchReplaceProductActivity, returnItemData.invoice_id.orEmpty())
                } else {
                   com.retailone.pos.localstorage.SharedPreference.OnHoldInvoiceHelper.removeOnHold(this@SearchReplaceProductActivity, returnItemData.invoice_id.orEmpty())
                }
                returnsale_viewmodel.callReplaceSaleApi(req, this@SearchReplaceProductActivity)
            } else {
                Log.d("ReplaceSubmit", "📴 Offline - queuing for later sync")
                
                // IMPORTANT: Locally mark this invoice as "ON HOLD" immediately, 
                // so replaced/return lists show the yellow ON HOLD badge instead of REPLACED.
                if (isHoldAction) {
                   com.retailone.pos.localstorage.SharedPreference.OnHoldInvoiceHelper.markAsOnHold(this@SearchReplaceProductActivity, returnItemData.invoice_id.orEmpty())
                } else {
                   com.retailone.pos.localstorage.SharedPreference.OnHoldInvoiceHelper.removeOnHold(this@SearchReplaceProductActivity, returnItemData.invoice_id.orEmpty())
                }
                
                lifecycleScope.launch {
                    val invoiceId = binding.searchBar.query.toString().trim()
                    val queueId = returnsale_viewmodel.queueReplaceRequest(invoiceId, req)

                    if (queueId > 0) {
                        showOfflineSuccessDialog()
                    } else {
                        showMessage("Failed to queue replace request")
                    }
                }
            }
        }

    }

    // -------- Parent toggle: keep Activity-side POSITIONS in sync --------
    private fun handleParentToggle(productId: Int, checked: Boolean, position: Int) {
        if (checked) selectedParentPositions.add(position) else selectedParentPositions.remove(
            position
        )
        Log.d(
            "ReplaceSummary",
            "handleParentToggle -> pid=$productId pos=$position checked=$checked (UI only)"
        )
    }

    /** Normalize a batch key */
    private fun normalizeBatchKey(batch: String?): String =
        batch?.trim()?.lowercase(Locale.ROOT) ?: ""

    /** ✅ Merge snapshot for ONE ROW (adapter position). */
    private fun mergeBatchesForRow(adapterPos: Int, snapshot: List<BatchReturnItem>) {
        val newMap: MutableMap<String, BatchReturnItem> = linkedMapOf()
        snapshot.forEach { br -> newMap[normalizeBatchKey(br.batch)] = br }

        if (newMap.isEmpty()) selectedBatchesByRow.remove(adapterPos)
        else selectedBatchesByRow[adapterPos] = newMap

        Log.d("ReplaceSummary",
            "mergeBatchesForRow: pos=$adapterPos snapshot=${snapshot.map { it.batch to (it.batch_return_quantity to (it.return_quantity ?: 0)) }} " + "all=${selectedBatchesByRow.mapValues { it.value.keys }}"
        )
    }

    /**
     * ✅ UNION totals (replaceable flow), POSITION-BASED:
     * - All batches of each parent-selected row
     * - PLUS any explicitly selected batches per row
     * Subtotal is tax-exclusive; Tax shown separately; Total = Subtotal + Tax.
     */
//    private fun recomputeTotalsUnion() {
//        if (!this::returnItemData.isInitialized) return
//
//        val taxPercent = parseTaxPercent(returnItemData.tax)
//        val items = returnItemData.salesItems.orEmpty()
//        if (selectedParentPositions.isEmpty() && selectedBatchesByRow.isEmpty()) {
//            applyZeroTotals(); return
//        }
//
//        // UNION set of (rowPos, batchKey)
//        val keys: MutableSet<Pair<Int, String>> = linkedSetOf()
//
//        // 1) From parent positions -> include all their batches (or _no_batch_)
//        selectedParentPositions.forEach { pos ->
//            val si = items.getOrNull(pos) ?: return@forEach
//            if (!si.batches.isNullOrEmpty()) {
//                si.batches!!.forEach { b -> keys.add(pos to normalizeBatchKey(b.batch)) }
//            } else {
//                keys.add(pos to "_no_batch_")
//            }
//        }
//
//        // 2) From explicit batch selections per row
//        selectedBatchesByRow.forEach { (pos, mapForRow) ->
//            mapForRow.keys.forEach { bkey -> keys.add(pos to bkey) }
//        }
//
//        // 3) Sum totals on union
//        var subtotalExcl = 0.0
//        keys.forEach { (pos, bkey) ->
//            val si = items.getOrNull(pos) ?: return@forEach
//            val b = if (bkey == "_no_batch_") null
//            else si.batches?.firstOrNull { normalizeBatchKey(it.batch) == bkey }
//
//            val qtyPurchased = if (b != null) ceil(b.quantity).toInt().coerceAtLeast(0)
//            else ceil(si.quantity).toInt().coerceAtLeast(0)
//
//            val unitExcl = resolveUnitPriceExclusive(b, si, taxPercent)
//            subtotalExcl += qtyPurchased * unitExcl
//        }
//
//        val taxAmount = subtotalExcl * taxPercent / 100.0
//
//        val grandTotal = round2(subtotalExcl + taxAmount)
//        val grandTotalRounded =
//            BigDecimal.valueOf(grandTotal).setScale(0, RoundingMode.HALF_UP).toDouble()
//
//        // Push UI
//        binding.subtotal.setText(String.format(Locale.US, "%.2f", subtotalExcl))
//        binding.taxAmount.setText(String.format(Locale.US, "%.2f", taxAmount))
//        binding.alltotalAmount.setText(String.format(Locale.US, "%.2f", grandTotalRounded))
//
//        binding.tvSubtotalValue.text = NumberFormatter().formatPrice(
//            String.format(Locale.US, "%.2f", subtotalExcl), localizationData
//        )
//        binding.tvTaxValue.text = NumberFormatter().formatPrice(
//            String.format(Locale.US, "%.2f", taxAmount), localizationData
//        )
//        binding.tvTotalValue.text = NumberFormatter().formatPrice(
//            String.format(Locale.US, "%.2f", grandTotalRounded), localizationData
//        )
//
//        Log.d(
//            "ReplaceSummary",
//            "UNION totals -> subExcl=$subtotalExcl tax=$taxAmount total=$grandTotal (tax%=$taxPercent) " + "parents=$selectedParentPositions batches=${selectedBatchesByRow.mapValues { it.value.keys }}"
//        )
//    }

    //////////////
    //newly added By Smruti
    private val salesItemPriceMap by lazy {
        if (!this::returnItemData.isInitialized) emptyMap()
        else {
            returnItemData.sales_items.orEmpty()
                .mapNotNull { item ->
                    item.tax_exclusive_price?.let {
                        Pair(item.product_id, item.batch) to it
                    }
                }
                .toMap()
        }
    }

    /////////////


    /**
     * ✅ UNION totals (replaceable flow), POSITION-BASED:
     * - All batches of each parent-selected row
     * - PLUS any explicitly selected batches per row
     * Subtotal is tax-exclusive; Tax shown separately;
     * Discount only from selected discount batches; Total = Subtotal + Tax - Discount.
     */


    private fun recomputeTotalsUnion() {
        if (!this::returnItemData.isInitialized) return

        val taxPercent = parseTaxPercent(returnItemData.tax)
        val items = returnItemData.salesItems.orEmpty()

        if (selectedParentPositions.isEmpty() && selectedBatchesByRow.isEmpty()) {
            applyZeroTotals()
            return
        }

        // UNION set of (rowPos, batchKey)
        val keys: MutableSet<Pair<Int, String>> = linkedSetOf()

        // 1) From parent positions -> include all their batches (or _no_batch_)
        selectedParentPositions.forEach { pos ->
            val si = items.getOrNull(pos) ?: return@forEach
            if (!si.batches.isNullOrEmpty()) {
                si.batches!!.forEach { b -> keys.add(pos to normalizeBatchKey(b.batch)) }
            } else {
                keys.add(pos to "_no_batch_")
            }
        }

        // 2) From explicit batch selections per row
        selectedBatchesByRow.forEach { (pos, mapForRow) ->
            mapForRow.keys.forEach { bkey -> keys.add(pos to bkey) }
        }

        // 3) ✅ Sum gross subtotal (tax-exclusive) + calculate tax from price difference
        var subtotalExcl = 0.0
        var taxAmount = 0.0

        keys.forEach { (pos, bkey) ->
            val si = items.getOrNull(pos) ?: return@forEach
            val b = if (bkey == "_no_batch_") null
            else si.batches?.firstOrNull { normalizeBatchKey(it.batch) == bkey }

            val qtyPurchased = if (b != null) {
                ceil(b.quantity ?: 0.0).toInt().coerceAtLeast(0)
            } else {
                ceil(si.quantity).toInt().coerceAtLeast(0)
            }

            // ✅ Get prices from sales_items (detailed list) first, then fallback to batch
            val detailedItem = returnItemData.sales_items?.firstOrNull { detailSi ->
                detailSi.id == (b?.sales_item_id ?: si.id) &&
                        (b == null || detailSi.batch?.trim()?.equals(b.batch?.trim(), ignoreCase = true) == true)
            }

            val taxExclusivePrice = detailedItem?.tax_exclusive_price
                ?: b?.tax_exclusive_price
                ?: si?.tax_exclusive_price
                ?: 0.0

            val retailPrice = detailedItem?.retail_price
                ?: b?.retail_price
                ?: si?.retail_price
                ?: 0.0

            // ✅ Calculate tax per unit from price difference
            val taxPerUnit = retailPrice - taxExclusivePrice

            // Add to subtotal (tax-exclusive)
            subtotalExcl += qtyPurchased * taxExclusivePrice

            // ✅ Add to tax total (from price difference, not percentage)
            taxAmount += qtyPurchased * taxPerUnit
        }


        // Round tax amount
        val taxAmountRounded = kotlin.math.round(taxAmount)

        // 5) NEW: discount only for actually selected discount batches / lines
        val discountTotal = computeSelectedDiscountTotal(items)

        // 6) Grand Total = Subtotal + Tax - Discount (not below 0)
        val grossPlusTax = round2(subtotalExcl + taxAmountRounded)
        val finalTotal = (grossPlusTax - discountTotal).coerceAtLeast(0.0)

        val grandTotalRounded = BigDecimal
            .valueOf(finalTotal)
            .setScale(0, RoundingMode.HALF_UP)
            .toDouble()

        // -------- Push to UI (old fields + summary card) --------
        binding.subtotal.setText(String.format(Locale.US, "%.2f", subtotalExcl))
        binding.taxAmount.setText(String.format(Locale.US, "%.2f", taxAmountRounded))
        binding.alltotalAmount.setText(String.format(Locale.US, "%.2f", grandTotalRounded))

        binding.tvSubtotalValue.text = NumberFormatter().formatPrice(
            String.format(Locale.US, "%.2f", subtotalExcl),
            localizationData
        )
        binding.tvTaxValue.text = NumberFormatter().formatPrice(
            String.format(Locale.US, "%.2f", taxAmountRounded),
            localizationData
        )

        // 🔻 Discount row in summary card
        if (discountTotal > 0.0) {
            binding.discountSummaryRow.isVisible = true
            val roundedDiscount = BigDecimal.valueOf(discountTotal).setScale(0, RoundingMode.HALF_UP)
            binding.tvDiscountValue.text = NumberFormatter().formatPrice(
                roundedDiscount.toPlainString(),
                localizationData
            )
        } else {
            binding.discountSummaryRow.isVisible = false
            binding.tvDiscountValue.text =
                NumberFormatter().formatPrice("0.00", localizationData)
        }

        // 🔻 Discount row in bottom payment card (after Tax)
        if (discountTotal > 0.0) {
            binding.delChargeLayout.isVisible = true
            val roundedDiscount = BigDecimal.valueOf(discountTotal).setScale(0, RoundingMode.HALF_UP)
            binding.discountvalue.text = NumberFormatter().formatPrice(
                roundedDiscount.toPlainString(),
                localizationData
            )
        } else {
            binding.delChargeLayout.isVisible = false
            binding.discountvalue.text =
                NumberFormatter().formatPrice("0", localizationData)
        }

        // Final total in summary card
        binding.tvTotalValue.text = NumberFormatter().formatPrice(
            String.format(Locale.US, "%.2f", grandTotalRounded),
            localizationData
        )

        Log.d(
            "ReplaceSummary",
            "UNION totals -> subExcl=$subtotalExcl tax=$taxAmountRounded discount=$discountTotal final=$finalTotal (tax%=$taxPercent) " +
                    "parents=$selectedParentPositions batches=${selectedBatchesByRow.mapValues { it.value.keys }}"
        )
    }



    /** Prefer tax-exclusive; else convert inclusive retail -> exclusive. */
    /* private fun resolveUnitPriceExclusive(
         batch: BatchReturnItem?, item: SalesItem?, taxPercent: Double
     ): Double {
         val excl = batch?.tax_exclusive_price ?: item?.tax_exclusive_price
         if (excl != null) return round2(excl)
         val retailIncl = batch?.retail_price ?: item?.retail_price ?: 0.0
         return if (taxPercent > 0) round2(retailIncl / (1.0 + taxPercent / 100.0)) else round2(
             retailIncl
         )
     }*/

    /// New Code Added By Smruti resolveUnitPriceExclusive

    private fun resolveUnitPriceExclusive(
        batch: BatchReturnItem?,
        item: SalesItem?,
        taxPercent: Double
    ): Double {

        //  Try batch-level exclusive price
        val excl = batch?.tax_exclusive_price ?: item?.tax_exclusive_price
        if (excl != null) return round2(excl)

        // Fallback: get from sales_items
        val productId = item?.product_id
        val batchKey = batch?.batch

        val mappedExcl = salesItemPriceMap[productId to batchKey]
        if (mappedExcl != null) return round2(mappedExcl)

        // 3️ Last fallback: convert retail (existing behavior)
        val retailIncl = batch?.retail_price ?: item?.retail_price ?: 0.0
        return if (taxPercent > 0)
            round2(retailIncl / (1.0 + taxPercent / 100.0))
        else
            round2(retailIncl)
    }
/////////////////////




    private fun round2(v: Double): Double =
        BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).toDouble()

    // -------------------- existing/kept logic --------------------
//    private fun applyZeroTotals() {
//        binding.subtotal.setText("0.00")
//        binding.taxAmount.setText("0.00")
//        binding.alltotalAmount.setText("0.00")
//        binding.tvSubtotalValue.text = NumberFormatter().formatPrice("0.00", localizationData)
//        binding.tvTaxValue.text = NumberFormatter().formatPrice("0.00", localizationData)
//        binding.tvTotalValue.text = NumberFormatter().formatPrice("0.00", localizationData)
//    }

    private fun showOfflineSuccessDialog() {
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

        logoutMsg.text = "Replace request saved offline.\nIt will be submitted when internet is available."
        logoutMsg.textSize = 16F

        print_receipt.isVisible = false

        confirm.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this@SearchReplaceProductActivity, MPOSDashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    private fun applyZeroTotals() {
        binding.subtotal.setText("0.00")
        binding.taxAmount.setText("0.00")
        binding.alltotalAmount.setText("0.00")

        binding.tvSubtotalValue.text = NumberFormatter().formatPrice("0.00", localizationData)
        binding.tvTaxValue.text = NumberFormatter().formatPrice("0.00", localizationData)
        binding.tvTotalValue.text = NumberFormatter().formatPrice("0.00", localizationData)

        // reset summary card discount
        binding.discountSummaryRow.isVisible = false
        binding.tvDiscountValue.text = NumberFormatter().formatPrice("0.00", localizationData)

        // reset bottom payment-card discount
        binding.delChargeLayout.isVisible = false
        binding.discountvalue.text = NumberFormatter().formatPrice("0.00", localizationData)
    }



//    private fun setTotalsFromServer(d: ReturnItemData) {
//        val subTotal = d.sub_total.toString()
//        val taxDisplay = formatTaxForDisplay(d.tax)
//        binding.subtotal.setText(subTotal)
//        binding.taxfield.setText("(+) Tax @$taxDisplay")
//        binding.taxAmount.setText(d.tax_amount.toString())
//        binding.alltotalAmount.setText(d.grand_total.toString())
//        binding.tvSubtotalValue.text =
//            NumberFormatter().formatPrice(d.sub_total.toString(), localizationData)
//        binding.tvTaxValue.text =
//            NumberFormatter().formatPrice(d.tax_amount.toString(), localizationData)
//        binding.tvTotalValue.text =
//            NumberFormatter().formatPrice(d.grand_total.toString(), localizationData)
//    }

    private fun setTotalsFromServer(d: ReturnItemData) {
        val subTotal = d.sub_total.toString()
        val taxDisplay = formatTaxForDisplay(d.tax)

        binding.subtotal.setText(subTotal)
        binding.taxfield.setText("(+) Tax @$taxDisplay")
        binding.taxAmount.setText(d.tax_amount.toString())
        binding.alltotalAmount.setText(d.grand_total.toString())

        binding.tvSubtotalValue.text =
            NumberFormatter().formatPrice(d.sub_total.toString(), localizationData)
        binding.tvTaxValue.text =
            NumberFormatter().formatPrice(d.tax_amount.toString(), localizationData)
        binding.tvTotalValue.text =
            NumberFormatter().formatPrice(d.grand_total.toString(), localizationData)

        // 🔻 No selection here: hide discount row
        val discountAmt = d.spot_discount_amount?.toDoubleOrNull() ?: 0.0
        if (discountAmt > 0.0) {
            binding.discountSummaryRow.visibility = View.VISIBLE
            val roundedDiscount = BigDecimal.valueOf(discountAmt).setScale(0, RoundingMode.HALF_UP)
            binding.tvDiscountValue.text =
                NumberFormatter().formatPrice(roundedDiscount.toPlainString(), localizationData)
        } else {
            binding.discountSummaryRow.visibility = View.GONE
        }
    }


    private fun updateReplaceUi() {
        val cart = LocalReturnCartHelper.getCartItems(this)
        Log.e("ReplaceSaleRequest",cart.size.toString())
        //  val items = returnItemData.salesItems.orEmpty()
        val items = returnItemData.salesItems.orEmpty()

        val canReplace: Boolean = when {
            cart.isNotEmpty() -> {
                cart.groupBy { it.id }.all { (salesItemId, lines) ->
                    val requested = lines.sumOf { it.return_quantity }
                    val available = storeStockBySalesItemId[salesItemId] ?: 0
                    available >= requested
                }
            }

            selectedBatchesByRow.isNotEmpty() || selectedParentPositions.isNotEmpty() -> {
                // Check stock per item using id; if id<=0, don't block.
                val ids = (selectedBatchesByRow.keys + selectedParentPositions).mapNotNull { pos ->
                    items.getOrNull(pos)?.id
                }.toSet()
                ids.all { id -> id <= 0 || (storeStockBySalesItemId[id] ?: 0) > 0 }
            }

            else -> {
                val detailed = returnItemData.sales_items.orEmpty()
                detailed.isNotEmpty() && detailed.all { si ->
                    val available = storeStockByProductId[si.product_id] ?: storeStockBySalesItemId[si.id] ?: si.store_stock
                    val needed = ceil(si.quantity).toInt()
                    available >= needed
                }
            }
        }

        canReplaceFlag = canReplace

        // Initial label; will be refined by stock logic below
        binding.next.text = if (canReplace) "Save and Replace" else "HOLD"
        binding.next.isEnabled = true
        binding.nextlayout.isEnabled = true

        Log.d(
            "ReplaceUI",
            "updateReplaceUi -> canReplace=$canReplace, " +
                    "cartSize=${cart.size}, " +
                    "parents=$selectedParentPositions, " +
                    "batches=${selectedBatchesByRow.mapValues { it.value.keys }}"
        )

        updateNextLayoutVisibilityForStock()
    }

    /**
     * Compute total discount only for:
     * - rows where a discount batch is actually selected, and
     * - parent rows with no batches where line has discount.
     * Each sales item’s discount is counted at most ONCE.
     */

    private fun extractDiscountFromSalesItem(source: Any?): Double {
        if (source == null) return 0.0
        return try {
            val json = Gson().toJson(source)
            val obj = JSONObject(json)

            when {
                // main field
                obj.has("discount") -> {
                    obj.optString("discount").toDoubleOrNull()
                        ?: obj.optDouble("discount", 0.0)
                }

                // optional alternate backend field names, if any
                obj.has("discount_amount") -> {
                    obj.optString("discount_amount").toDoubleOrNull()
                        ?: obj.optDouble("discount_amount", 0.0)
                }

                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }


    /**
     * Compute total discount only for:
     * - rows where a discount batch is actually selected, and
     * - parent rows with no batches where line has discount.
     *
     * Each ROW (adapter position) is counted at most once.
     */
    private fun computeSelectedDiscountTotal(items: List<SalesItem>): Double {
        var totalDiscount = 0.0

        // We dedupe by ADAPTER POSITION (ids can be 0 / reused)
        val countedPositions = mutableSetOf<Int>()

        // -------- 1) Rows with explicit batch selections --------
        selectedBatchesByRow.forEach { (pos, mapForRow) ->
            val si = items.getOrNull(pos) ?: return@forEach
            if (!countedPositions.add(pos)) return@forEach  // already counted this row

            var rowDiscount = 0.0

            // Only the batches that user actually checked for this row
            mapForRow.values.forEach { br ->
                val d = discountForBatchSelection(br)
                if (d > rowDiscount) rowDiscount = d
            }

            // Fallback: if no batch-level discount but line itself has discount (no batches case)
            if (rowDiscount <= 0.0 && si.batches.isNullOrEmpty()) {
                rowDiscount = discountForSalesItemId(si.id)
            }

            if (rowDiscount > 0.0) {
                totalDiscount += rowDiscount
            }
        }

        // -------- 2) Parent-only rows (no explicit batch selection) --------
        selectedParentPositions.forEach { pos ->
            // If this row already processed as "explicit batch" above, skip
            if (!countedPositions.add(pos)) return@forEach

            val si = items.getOrNull(pos) ?: return@forEach

            var rowDiscount = 0.0

            if (!si.batches.isNullOrEmpty()) {
                // Parent row with batches: use the SAME batch logic as the card
                si.batches!!.forEach { br ->
                    val d = discountForBatchSelection(br)
                    if (d > rowDiscount) rowDiscount = d
                }
            } else {
                // No batches: read discount from detailed list
                rowDiscount = discountForSalesItemId(si.id)
            }

            if (rowDiscount > 0.0) {
                totalDiscount += rowDiscount
            }
        }

        Log.d(
            "ReplaceSummary",
            "computeSelectedDiscountTotal -> totalDiscount=$totalDiscount, countedPositions=$countedPositions"
        )

        return totalDiscount
    }





    private fun discountForSalesItemId(salesItemId: Int): Double {
        if (!this::returnItemData.isInitialized) return 0.0

        val detailedSales = returnItemData.sales_items.orEmpty()
        var maxDiscount = 0.0

        detailedSales.forEach { row ->
            try {
                val obj = JSONObject(Gson().toJson(row))

                val rowId = when {
                    obj.has("id") -> obj.optInt("id", -1)
                    obj.has("sales_item_id") -> obj.optInt("sales_item_id", -1)
                    obj.has("sale_detail_id") -> obj.optInt("sale_detail_id", -1)
                    else -> -1
                }

                if (rowId == salesItemId) {
                    val d = extractDiscountFromSalesItem(row)
                    if (d > maxDiscount) maxDiscount = d
                }
            } catch (_: Exception) {
            }
        }

        return maxDiscount.coerceAtLeast(0.0)
    }


    /**
     * Same discount resolution as batch adapter:
     * find discount by (sales_item_id + batch name) then by id as fallback.
     * Used ONLY to detect which batch is the "discount batch".
     */
    private fun discountForBatchSelection(item: BatchReturnItem): Double {
        if (!this::returnItemData.isInitialized) return 0.0

        val batchName = batchTextFromBatch(item).trim()
        val detailedSales = returnItemData.sales_items.orEmpty()

        // Exact match on id + batch
        val exact = detailedSales.firstOrNull { si ->
            si.id == item.sales_item_id &&
                    (si.batch?.trim()?.equals(batchName, ignoreCase = true) ?: false)
        }

        var discount = exact?.let { extractDiscountFromSalesItem(it) } ?: 0.0

        // Fallback: match only by sales_item_id (same behaviour as in adapter)
        if (discount <= 0.0) {
            val byId = detailedSales.firstOrNull { it.id == item.sales_item_id }
            discount = byId?.let { extractDiscountFromSalesItem(it) } ?: 0.0
        }

        Log.d(
            "ReplaceSummary",
            "discountForBatchSelection -> sales_item_id=${item.sales_item_id}, batch='$batchName', discount=$discount"
        )

        return discount.coerceAtLeast(0.0)
    }



    /**
     * Helper to read batch name from BatchReturnItem, same logic as in child adapter.
     */
    private fun batchTextFromBatch(item: BatchReturnItem): String {
        return try {
            val obj = JSONObject(Gson().toJson(item))
            when {
                obj.has("batch") -> obj.optString("batch")
                obj.has("batch_no") -> obj.optString("batch_no")
                obj.has("batchNumber") -> obj.optString("batchNumber")
                obj.has("batch_name") -> obj.optString("batch_name")
                obj.has("lot") -> obj.optString("lot")
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }



//    private fun updateReplaceUi() {
//        val cart = LocalReturnCartHelper.getCartItems(this)
//        val items = returnItemData.salesItems.orEmpty()
//
//        val canReplace: Boolean = when {
//            cart.isNotEmpty() -> {
//                cart.groupBy { it.id }.all { (salesItemId, lines) ->
//                    val requested = lines.sumOf { it.return_quantity }
//                    val available = storeStockBySalesItemId[salesItemId] ?: 0
//                    available >= requested
//                }
//            }
//
//            selectedBatchesByRow.isNotEmpty() || selectedParentPositions.isNotEmpty() -> {
//                // Check stock per item using id; if id<=0, don't block.
//                val ids = (selectedBatchesByRow.keys + selectedParentPositions).mapNotNull { pos ->
//                    items.getOrNull(pos)?.id
//                }.toSet()
//                ids.all { id -> id <= 0 || (storeStockBySalesItemId[id] ?: 0) > 0 }
//            }
//
//            else -> {
//                val detailed = returnItemData.sales_items.orEmpty()
//                detailed.isNotEmpty() && detailed.all { si ->
//                    val available = si.store_stock
//                    val needed = ceil(si.quantity).toInt()
//                    available >= needed
//                }
//            }
//        }
//
//        canReplaceFlag = canReplace
//        binding.next.text = if (canReplace) "Save and Replace" else "HOLD"
//        binding.next.isEnabled = true
//        binding.nextlayout.isEnabled = true
//        updateNextLayoutVisibilityForStock()
//    }


    private fun updateNextLayoutVisibilityForStock() {

        if (!this::returnItemData.isInitialized) {
            return
        }

        val detailed = if (this::returnItemData.isInitialized) {
            if (returnItemData.sales_items.isNullOrEmpty()) {
                returnItemData.salesItems.orEmpty().map {
                    com.retailone.pos.models.ReturnSalesItemModel.SalesItemDetailed(
                        id = it.id,
                        sales_id = it.sales_id,
                        product_id = it.product_id,
                        on_hold = 0,
                        distribution_pack_id = it.distribution_pack_id,
                        distribution_pack_name = it.distribution_pack_name,
                        batch = null,
                        quantity = it.quantity,
                        store_stock = 0,
                        retail_price = it.retail_price,
                        discount = 0.0,
                        tax_exclusive_price = it.tax_exclusive_price,
                        total_amount = it.total_amount,
                        product = it.product,
                        distribution_pack = it.distribution_pack,
                        sales_returns = null
                    )
                }
            } else {
                returnItemData.sales_items.orEmpty()
            }
        } else emptyList()

        val anyStockAvailable = detailed.any { si ->
            val available = storeStockByProductId[si.product_id] ?: storeStockBySalesItemId[si.id] ?: si.store_stock
            available > 0
        }

        val forceReplaceExists = isInvoiceAlreadyReplaced && detailed.any { si ->
            val stockForProduct = storeStockByProductId[si.product_id] ?: si.store_stock
            val saleQuantityForProduct = saleQuantityByProductId[si.product_id] ?: si.quantity
            si.on_hold == 1 && stockForProduct >= saleQuantityForProduct
        }

        // ✅ If the invoice is already replaced with no force-replace opportunity, hide the button.
        // This also prevents the async offline stock coroutine from re-showing the button after
        // return@observe has already been called in the isAlreadyReplaced block.
        if (isInvoiceAlreadyReplaced && !forceReplaceExists) {
            binding.relativeLayout.isVisible = false
            binding.nextlayout.isVisible = false
            return
        }

        // Treat as "still on hold" ONLY if not a force-replace candidate
        val isOnHold = if (this::returnItemData.isInitialized) {
            detailed.any { si ->
                val stockForProduct = storeStockByProductId[si.product_id] ?: si.store_stock
                val saleQuantityForProduct =  saleQuantityByProductId[si.product_id] ?: si.quantity

                //// si.on_hold == 1 && stockForProduct <= 1
                si.on_hold == 1 && stockForProduct <= saleQuantityForProduct
            }
        } else false

        val hasAnyPositiveUnderstock = detailed.any { si ->
            val needed = ceil(si.quantity).toInt()
            val available = storeStockByProductId[si.product_id] ?: storeStockBySalesItemId[si.id] ?: si.store_stock
            available > 0 && available < needed
        }


        val allZeroStock = detailed.isNotEmpty() && detailed.all { si -> 
            val available = storeStockByProductId[si.product_id] ?: storeStockBySalesItemId[si.id] ?: si.store_stock
            available <= 0 
        }


        // can Any product save and replace newly added by Smruti
        val canAnySaveReplace = if (this::returnItemData.isInitialized) {
            detailed.any { si ->
                val stockForProduct = storeStockByProductId[si.product_id] ?: si.store_stock
                val saleQuantityForProduct =  saleQuantityByProductId[si.product_id] ?: si.quantity

                //// si.on_hold == 1 && stockForProduct <= 1
                stockForProduct >=  saleQuantityForProduct
            }
        } else false

        binding.relativeLayout.isVisible = true
        binding.nextlayout.isVisible = true

        canReplaceFlag = (anyStockAvailable && !hasAnyPositiveUnderstock) || forceReplaceExists || canAnySaveReplace
        binding.next.text = if (canReplaceFlag) "Save and Replace" else "HOLD"



        // 🔑 Enable button when forceReplaceExists even if other rules would disable it


        val enabled = when {
            allZeroStock && !forceReplaceExists -> !isOnHold
            ///// modified
            hasAnyPositiveUnderstock && !forceReplaceExists ->  !isOnHold
            //// hasAnyPositiveUnderstock && !forceReplaceExists -> false
            else -> anyStockAvailable || !isOnHold || forceReplaceExists
        }

        Log.d(
            "ReplaceDebugLog",
            "updateNextLayoutVisibilityForStock summary:\n" +
            "anyStockAvailable: $anyStockAvailable \n" +
            "forceReplaceExists: $forceReplaceExists \n" +
            "isOnHold: $isOnHold \n" +
            "hasAnyPositiveUnderstock: $hasAnyPositiveUnderstock \n" +
            "allZeroStock: $allZeroStock \n" +
            "canAnySaveReplace: $canAnySaveReplace \n" +
            "canReplaceFlag: $canReplaceFlag \n" +
            "enabled: $enabled"
        )
        // Also log per-item stock mapping
        detailed.forEach {
            val stock = storeStockByProductId[it.product_id] ?: it.store_stock
            val saleQty = saleQuantityByProductId[it.product_id] ?: it.quantity
            Log.d("ReplaceDebugLog", "Product ID: ${it.product_id}, mapped stock: $stock, true sale qty: $saleQty, API on_hold: ${it.on_hold}, API store_stock: ${it.store_stock}")
        }


        binding.next.isEnabled = enabled
        binding.nextlayout.isEnabled = enabled
        binding.nextlayout.isClickable = enabled
        binding.nextlayout.alpha = if (enabled) 1f else 0.5f

        if ((anyStockAvailable && !hasAnyPositiveUnderstock) || forceReplaceExists) {
            binding.calenderText.text = "Note: An invoice can only be replaced once."
            binding.calenderText.setTextColor(getColor(R.color.black))
        } else {
            binding.calenderText.text = "Stock is not available."
            binding.calenderText.setTextColor(getColor(R.color.Red))
        }

        /* Log.d(
             "ReplaceForce",
             "updateNextLayoutVisibility -> anyStock=$anyStockAvailable, " + "forceReplaceExists=$forceReplaceExists, isOnHold=$isOnHold, " + "enabled=$enabled"
         )*/
    }


    private fun formatTaxForDisplay(raw: Any?): String {
        val s0 = raw?.toString()?.trim().orEmpty()
        if (s0.isEmpty()) return "0"
        val s1 = s0.replace(Regex("[^0-9.,]"), "").replace(',', '.')
        if (s1.isEmpty() || s1 == ".") return "0"
        if (s1.contains('.')) {
            return try {
                BigDecimal(s1).stripTrailingZeros().toPlainString()
            } catch (_: Exception) {
                s1
            }
        }
        val n = s1.toLongOrNull() ?: return s1
        val scaled = n / 10.0
        return DecimalFormat("#.##").format(scaled)
    }

    private fun parseTaxPercent(raw: Any?): Double {
        val s0 = raw?.toString()?.trim().orEmpty()
        if (s0.isEmpty()) return 0.0
        val cleaned = s0.replace(Regex("[^0-9.,-]"), "").replace(',', '.')
        if (cleaned.isEmpty() || cleaned == "." || cleaned == "-") return 0.0
        val value = cleaned.toDoubleOrNull() ?: return 0.0
        if (!s0.contains('.') && !s0.contains(',') && value >= 100) {
            val by10 = value / 10.0
            return if (by10 <= 100) by10 else value / 100.0
        }
        return value
    }

    private fun getReturnDateTime(): String {
        val zone = localizationData.timezone
        val timezone = when (zone) {
            "IST" -> "Asia/Kolkata"
            "CAT" -> "Africa/Lusaka"
            else -> "Africa/Lusaka"
        }
        val calendar = Calendar.getInstance().apply { timeZone = TimeZone.getTimeZone(timezone) }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(timezone)
        }
        return dateFormat.format(calendar.time)
    }

    private fun setToolbarImage() {
        val organisation_data = OrganisationDetailsHelper(this).getOrganisationData()
        Glide.with(this).load(organisation_data.image_url + organisation_data.fabicon).fitCenter()
            .placeholder(R.drawable.mlogo).error(R.drawable.mlogo).into(binding.image)
    }

    private fun preparePositemRCV() {
        binding.positemRcv.apply {
            layoutManager = LinearLayoutManager(
                this@SearchReplaceProductActivity, RecyclerView.VERTICAL, false
            )
        }
    }

    private fun enableBackButton() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "New Activity"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.svg_back_arrow_white)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed(); return true
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@SearchReplaceProductActivity, msg, Toast.LENGTH_SHORT).show()
    }

    // Legacy (normal flow) — unchanged
    override fun onReturnQuantityChange(position: Int, newQuantity: Int) {
        returnItemList[position].return_quantity = newQuantity
        returnItemList[position].refund_amount = newQuantity * returnItemList[position].retail_price
        recalculateTotals()
    }

    private fun recalculateTotals() {
        var subtotal = 0.0
        val taxRate = parseTaxPercent(returnItemData.tax)
        returnItemList.forEach {
            if (!it.readonlyMode && !it.isExpired && it.return_quantity > 0) {
                val rateIncl = it.retail_price
                val qty = it.return_quantity
                subtotal += rateIncl * qty
            }
        }
        val taxAmount =
            if (subtotal == 0.0) 0.0 else round2(subtotal * (taxRate / (100.0 + taxRate)))
        val grandTotal = subtotal
        binding.subtotal.setText(String.format(Locale.US, "%.2f", subtotal))
        binding.taxAmount.setText(String.format(Locale.US, "%.2f", taxAmount))
        binding.alltotalAmount.setText(String.format(Locale.US, "%.2f", java.math.BigDecimal.valueOf(grandTotal).setScale(0, java.math.RoundingMode.HALF_UP).toDouble()))
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
        val print_receipt = dialog.findViewById<MaterialButton>(R.id.print_receipt)
        print_receipt.isVisible = false

        // Determine message based on action
        val displayMessage = when {
            isHoldAction -> "Product On-Hold"
            !msg.isNullOrBlank() -> msg  // Use API message if available
            else -> "Items Replaced Successfully"  // Fallback message
        }

        logoutMsg.text = displayMessage  // CHANGED: Use displayMessage instead of msg
        logoutMsg.textSize = 16F

        confirm.setOnClickListener {
            dialog.dismiss()
            val intent =
                Intent(this@SearchReplaceProductActivity, MPOSDashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

//        print_receipt.setOnClickListener {
//            printerUtil?.printReturnReceiptData(returnSaleRes)
//        }

        dialog.show()

        // Reset flag after showing dialog
        isHoldAction = false  // ADDED: Reset flag
    }


    fun dismissKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume(); printerUtil?.registerBatteryReceiver()
    }

    override fun onPause() {
        super.onPause(); printerUtil?.unregisterBatteryReceiver()
    }

    // ===== JSON helper (kept) =====
    private fun extractRefundedAmount(item: SalesItem): Double {
        return try {
            val json = Gson().toJson(item)
            val obj = JSONObject(json)
            when {
                obj.has("total_refunded_amount") -> obj.optString("total_refunded_amount")
                    .toDoubleOrNull() ?: obj.optDouble("total_refunded_amount", 0.0)

                obj.has("total_returned_amount") -> obj.optString("total_returned_amount")
                    .toDoubleOrNull() ?: obj.optDouble("total_returned_amount", 0.0)

                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    /** Build returned_items for API (positions → ids at the end) */
    private fun buildReplaceLines(): List<ReplaceReturnedItem> {
        // If the backend didn't supply sales_items (pure offline scenario), fake it from salesItems
        val detailedItems = if (returnItemData.sales_items.isNullOrEmpty()) {
            returnItemData.salesItems.orEmpty().map {
                com.retailone.pos.models.ReturnSalesItemModel.SalesItemDetailed(
                    id = it.id,
                    sales_id = it.sales_id,
                    product_id = it.product_id,
                    on_hold = 0,
                    distribution_pack_id = it.distribution_pack_id,
                    distribution_pack_name = it.distribution_pack_name,
                    batch = null,
                    quantity = it.quantity,
                    store_stock = 0,
                    retail_price = it.retail_price,
                    discount = 0.0,
                    tax_exclusive_price = it.tax_exclusive_price,
                    total_amount = it.total_amount,
                    product = it.product,
                    distribution_pack = it.distribution_pack,
                    sales_returns = null
                )
            }
        } else {
            returnItemData.sales_items.orEmpty()
        }
        
        val totalReplacedAmount =
            returnItemData.total_replaced_amount // not null, but kept for clarity

        if (selectedBatchesByRow.isNotEmpty() || selectedParentPositions.isNotEmpty()) {

            val result = mutableListOf<ReplaceReturnedItem>()

            // From explicit batch selections (by row)
            selectedBatchesByRow.forEach { (pos, mapForRow) ->
                val si = detailedItems.getOrNull(pos) ?: return@forEach
                val sid = si.id
                val productId = si.product_id

                val availableStockForProduct = storeStockByProductId[productId] ?: si.store_stock
                val saleQuantityForProduct =
                    saleQuantityByProductId[productId] ?: si.quantity

                //  Log.e("ReplaceSaleRequest","availableStockForProduct"+availableStockForProduct.toString())

                val wasOnHold = si.on_hold == 1
                // 🚩 Special case: product was on hold, now stock > 1 → on_hold must be 0
                val forceReplace =  (totalReplacedAmount != null) && wasOnHold && availableStockForProduct >= saleQuantityForProduct

                //// (totalReplacedAmount != null) && wasOnHold && availableStockForProduct > 1


                val lineOnHold = when {
                    forceReplace -> 0
                    ////availableStockForProduct <= 0 -> 1
                    availableStockForProduct < saleQuantityForProduct -> 1

                    else -> 0
                }

                val boxes = mapForRow.values.sumOf { it.batch_return_quantity ?: 0 }
                val bottles = mapForRow.values.sumOf { it.return_quantity ?: 0 }

                Log.d(
                    "ReplaceOnHold",
                    "Batch row pos=$pos sid=$sid productId=$productId stock=$availableStockForProduct on_hold=$lineOnHold force=$forceReplace"
                )

                result += ReplaceReturnedItem(
                    id = sid,
                    return_quantity = ceil(si.quantity).toInt(),
                    defective_boxes = boxes,
                    defective_bottles = bottles,
                    on_hold = lineOnHold,
                    product_id = si.product_id,
                    distribution_pack_id = si.distribution_pack_id
                )
            }

            // From parent-only rows without explicit batch selections
            selectedParentPositions.forEach { pos ->
                if (!selectedBatchesByRow.containsKey(pos)) {
                    val si = detailedItems.getOrNull(pos) ?: return@forEach
                    val sid = si.id
                    val productId = si.product_id


                    val availableStockForProduct =
                        storeStockByProductId[productId] ?: si.store_stock

                    val saleQuantityForProduct =
                        saleQuantityByProductId[productId] ?: si.quantity

                    Log.e("ReplaceSaleRequest","availableStockForProduct"+availableStockForProduct)


                    val wasOnHold = si.on_hold == 1
                    val forceReplace =
                        ////(totalReplacedAmount != null) && wasOnHold && availableStockForProduct > 1
                        (totalReplacedAmount != null) && wasOnHold && availableStockForProduct >= saleQuantityForProduct

                    val lineOnHold = when {
                        forceReplace -> 0
                        ////availableStockForProduct <= 0 -> 1
                        availableStockForProduct < saleQuantityForProduct -> 1
                        else -> 0
                    }

                    Log.d(
                        //"ReplaceOnHold",
                        "ReplaceSaleRequest",
                        "Parent row pos=$pos sid=$sid productId=$productId stock=$availableStockForProduct on_hold=$lineOnHold " +
                                "force=$forceReplace  saleQuantity=$saleQuantityForProduct on_hold_before=${si.on_hold}"
                    )

                    result += ReplaceReturnedItem(
                        id = sid,
                        return_quantity = ceil(si.quantity).toInt(),
                        defective_boxes = 0,
                        defective_bottles = 0,
                        on_hold = lineOnHold,
                        product_id = si.product_id,
                        distribution_pack_id = si.distribution_pack_id
                    )
                }
            }

            return result.filter {
                (it.defective_boxes ?: 0) > 0 || (it.defective_bottles
                    ?: 0) > 0 || selectedParentPositions.any { pos -> detailedItems.getOrNull(pos)?.id == it.id }
            }
        }




        // Fallbacks unchanged
        val saved = LocalReturnCartHelper.getCartItems(this)
        if (saved.isNotEmpty()) {


            return saved.map { line ->
                val si = detailedItems.firstOrNull { it.id == line.id }
                val productId = si?.product_id

                val availableStockForProduct = if (productId != null) {
                    storeStockByProductId[productId] ?: si.store_stock
                } else {
                    0
                }


                ////
                val saleQuantityForProduct = if (productId != null) {
                    saleQuantityByProductId[productId] ?: si.quantity
                } else {
                    0.0
                }


                val wasOnHold = (si?.on_hold ?: 0) == 1
                val forceReplace =
                    (totalReplacedAmount != null) && wasOnHold && availableStockForProduct >= saleQuantityForProduct

                //// (totalReplacedAmount != null) && wasOnHold && availableStockForProduct > 1

                val lineOnHold = when {
                    forceReplace -> 0
                    ////availableStockForProduct <= 0 -> 1
                    availableStockForProduct < saleQuantityForProduct -> 1
                    else -> 0
                }

                Log.d(
                    "ReplaceOnHold",
                    "Saved line sid=${line.id} productId=$productId stock=$availableStockForProduct on_hold=$lineOnHold force=$forceReplace"
                )

                ReplaceReturnedItem(
                    id = line.id, return_quantity = ceil(
                        si?.quantity ?: 0.0
                    ).toInt(), defective_boxes = 0, defective_bottles = 0, on_hold = lineOnHold,
                    product_id = si?.product_id, distribution_pack_id = si?.distribution_pack_id
                )
            }
        }
        if (isHoldAction && selectedBatchesByRow.isEmpty() && selectedParentPositions.isEmpty()) {
            if (detailedItems.isNotEmpty()) {
                Log.e("ReplaceSaleRequest", "buildReplaceLines: using detailedItems for hold action")
                return detailedItems.map { si ->
                    ReplaceReturnedItem(
                        id = si.id,
                        return_quantity = ceil(si.quantity).toInt(),
                        defective_boxes = 0,
                        defective_bottles = 0,
                        on_hold = 1,
                        product_id = si.product_id,
                        distribution_pack_id = si.distribution_pack_id
                    )
                }
            } else {
                val fallbackItems = returnItemData.salesItems.orEmpty()
                Log.e("ReplaceSaleRequest", "buildReplaceLines: detailedItems is empty, using fallback items (size=${fallbackItems.size})")
                return fallbackItems.map { si ->
                    ReplaceReturnedItem(
                        id = si.id,
                        return_quantity = ceil(si.quantity).toInt(),
                        defective_boxes = 0,
                        defective_bottles = 0,
                        on_hold = 1,
                        product_id = si.product_id,
                        distribution_pack_id = si.distribution_pack_id
                    )
                }
            }
        }
        return emptyList()
    }
}
