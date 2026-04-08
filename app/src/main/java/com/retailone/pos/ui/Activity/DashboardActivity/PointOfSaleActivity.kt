package com.retailone.pos.ui.Activity.DashboardActivity

import NumberFormatter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.retailone.pos.R
import com.retailone.pos.adapter.PointofsaleItemAdapter
import com.retailone.pos.adapter.PosSearchAdapter
import com.retailone.pos.databinding.ActivityPointOfSaleBinding
import com.retailone.pos.databinding.BarcodeProductsaleBottomsheetBinding
import com.retailone.pos.databinding.BarcodeProductsaleRcvBottomsheetBinding
import com.retailone.pos.databinding.PosSearchDialogLayoutBinding
import com.retailone.pos.interfaces.OnDeleteItemClickListener
import com.retailone.pos.interfaces.OnQuantityChangeListener
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.CustomerSessionHelper
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.CommonModel.StroreProduct.PosSaleBatch
import com.retailone.pos.models.CommonModel.StroreProduct.StoreProData
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.BatchCartItem
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.CartProductItem
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PosAddToCartReq
import com.retailone.pos.network.Constants
import com.retailone.pos.utils.BatchUtils
import com.retailone.pos.utils.FeatureManager
import com.retailone.pos.utils.FunUtils
import com.retailone.pos.utils.NetworkUtils
import com.retailone.pos.utils.OfflineCartCalculator
import com.retailone.pos.viewmodels.DashboardViewodel.PointofSaleViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.apply

class PointOfSaleActivity : AppCompatActivity(), OnDeleteItemClickListener,
    OnQuantityChangeListener {

    companion object {
        private const val TAG_SEARCH_LIST = "POS_SEARCH_UI"
        private const val TAG_CART_LIST = "POS_CART_UI"
    }

    lateinit var binding: ActivityPointOfSaleBinding
    lateinit var pos_viewmodel: PointofSaleViewmodel
    lateinit var positem_adapter: PointofsaleItemAdapter
    lateinit var incudebinding: PosSearchDialogLayoutBinding
    lateinit var sheetBehavior: BottomSheetBehavior<ConstraintLayout>
    var posItemList = mutableListOf<StoreProData>()
    var storeid = 0
    var c_id = 0
    var c_name = ""
    var c_mobile = ""
    var c_tpin = ""
    lateinit var localizationData: LocalizationData
    private val PERMISSION_REQUEST_CAMERA = 1
    private lateinit var scanQrResultLauncher: ActivityResultLauncher<Intent>
    var canpressback = true
    private var maxSpotDiscountLimit = 0.0
    private var appliedSpotDiscountPercent = 0.0

    override fun onDestroy() {
        super.onDestroy()
        // Stop network checking to prevent memory leaks
        android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacksAndMessages(null)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPointOfSaleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // customer info
        c_id = intent.getIntExtra("c_id", 0)
        c_name = intent.getStringExtra("c_name").toString()
        c_mobile = intent.getStringExtra("c_mobile").toString()
        c_tpin = intent.getStringExtra("c_tpin").toString()

        // bindings
        incudebinding = binding.include
        sheetBehavior = BottomSheetBehavior.from(incudebinding.bottomSheetLayout)
        pos_viewmodel = ViewModelProvider(this)[PointofSaleViewmodel::class.java]
        localizationData = LocalizationHelper(this).getLocalizationData()

        // bottom bar hidden initially if cart empty
        if (posItemList.isEmpty()) {
            binding.relativeLayout.isVisible = false
        }

        // fetch store products on launch
        val loginSession = LoginSession.getInstance(this)
        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first().toInt()
            pos_viewmodel.callSearchStoreProductApi("", storeid, c_id, this@PointOfSaleActivity)

            val isSpotDiscountEnabled = loginSession.isSpotDiscountEnabled().first()
            val limitStr = loginSession.getSpotDiscountLimit().first()
            maxSpotDiscountLimit = limitStr.toDoubleOrNull() ?: 0.0

            binding.spotDiscountCard.isVisible = isSpotDiscountEnabled

            // Setup UI listeners AFTER limit is loaded so maxSpotDiscountLimit is correct
            setupSpotDiscountUI()
        }





        // loading spinner for main screen (center progress card)
        pos_viewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)


            // bottom bar: show progress layout OR next layout
            binding.progressLayout.isVisible = it.isProgress
            binding.nextlayout.isVisible = !it.isProgress
        }

        // scanner result callback
        scanQrResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultData ->
            if (resultData.resultCode == RESULT_OK && resultData.data != null) {
                val result = ScanIntentResult.parseActivityResult(
                    resultData.resultCode, resultData.data
                )
                if (result.contents == null) {
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show()
                } else {
                    pos_viewmodel.callSearchStoreProductBarcodeApi(
                        result.contents.toString(), storeid, c_id, this@PointOfSaleActivity
                    )
                }
            }
        }

        enableBackButton()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackWithConfirm()
            }
        })

        // set up both RecyclerViews
        preparePositemRCV()        // cart list (positem_rcv)
        prepareProductLists()      // available products + bottom sheet list
        setToolbarImage()

        // tap search text -> open bottom sheet with search bar
        binding.searchtext.setOnClickListener {
            dismissKeyboard(it)
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        // barcode icon on main screen
        binding.barcodeimage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this@PointOfSaleActivity,
                        arrayOf(android.Manifest.permission.CAMERA),
                        PERMISSION_REQUEST_CAMERA
                    )
                } else {
                    startScanning()
                }
            } else startScanning()
        }

        // barcode icon inside bottom sheet
        incudebinding.barcodeimage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this@PointOfSaleActivity,
                        arrayOf(android.Manifest.permission.CAMERA),
                        PERMISSION_REQUEST_CAMERA
                    )
                } else {
                    startScanning()
                }
            } else startScanning()
        }

        // barcode result livedata
        pos_viewmodel.storeProSearchBarcodeLivedata.observe(this) {
            if (it.data.isNotEmpty()) {
                // log what is shown in this list too
                logSearchList(it.data)
                barcodeProductSaleBottomSheetRCV(it.data)
                if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            } else {
                showMessage("Scanned Product Currrently Unavailable")
            }
        }

        // bottom bar checkout button default click
        binding.nextlayout.setOnClickListener {
            Log.d("sdf", posItemList.toString())
            if (posItemList.isNotEmpty()) {
                if (!posItemList.any {
                        BatchUtils.getTotalPosCartQuantity(it.batch).toDouble() == 0.0
                    }) {
                    // actual navigation happens again after totals calculation below
                } else {
                    showMessage("Quantity Of can't be zero or empty")
                }
            } else {
                showMessage("Please Add Atleast one item")
            }
        }

        // sync UI once
        refreshCartUI()
    }

    /**
     * Toggle between empty state (product list) and cart list
     */
    private fun refreshCartUI() {
        val hasItems = posItemList.isNotEmpty()
        binding.positemRcv.isVisible = hasItems
        binding.addproductLayout.isVisible = !hasItems
    }
    private fun observeNetworkStatus() {
//        val tvWelcome = findViewById<TextView>(R.id.welcomeText)
        val sessionHelper = CustomerSessionHelper(this)
        val customerName = sessionHelper.getCustomerName()

        // Check network status every second
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val networkCheckRunnable = object : Runnable {
            override fun run() {
                val onlineStatus = if (NetworkUtils.isInternetAvailable(this@PointOfSaleActivity)) "🟢" else "🔴"

//                if (customerName.isNotEmpty()) {
//                    tvWelcome.text = "$onlineStatus WELCOME $customerName"
//                } else {
//                    tvWelcome.text = "$onlineStatus WELCOME GUEST"
//                }

                // Check again after 1 second
                handler.postDelayed(this, 1000)
            }
        }

        // Start checking
        handler.post(networkCheckRunnable)
    }

    /**
     * Log the values that are shown in the product list (name / pack / units)
     */
    private fun logSearchList(products: List<StoreProData>) {
        Log.d(TAG_SEARCH_LIST, "Search result size = ${products.size}")
        products.forEachIndexed { index, item ->
            val name = item.product_name ?: ""
            val pack = buildString {
                append(item.pack_product_description ?: "")
                if (!item.uom.isNullOrEmpty()) {
                    append(" (")
                    append(item.uom)
                    append(")")
                }
            }
            val units = "${item.stock_quantity.toInt()} Units"
            Log.d(
                TAG_SEARCH_LIST, "[$index] name='$name', pack='$pack', units='$units'"
            )
        }
    }

    /**
     * Log the values shown in cart list (positemRcv)
     */
    private fun logCartList() {
        Log.d(TAG_CART_LIST, "Cart size = ${posItemList.size}")
        posItemList.forEachIndexed { index, item ->
            val name = item.product_name ?: ""
            val pack = buildString {
                append(item.pack_product_description ?: "")
                if (!item.uom.isNullOrEmpty()) {
                    append(" (")
                    append(item.uom)
                    append(")")
                }
            }
            val qty = item.cart_quantity
            Log.d(
                TAG_CART_LIST,
                "[$index] name='$name', pack='$pack', cartQty=$qty, dispenseStatus=${item.dispense_status}"
            )
        }
    }

    /**
     * Prepare:
     * - product chooser lists
     * - totals observer
     */
    private fun prepareProductLists() {

        // MAIN SCREEN PRODUCT LIST
        binding.addproductLayout.apply {
            layoutManager = LinearLayoutManager(
                this@PointOfSaleActivity, RecyclerView.VERTICAL, false
            )
        }

        // BOTTOM SHEET SEARCH LIST
        incudebinding.searchstockRcv.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        // Observe store product search and bind adapters
        pos_viewmodel.storeProSearchLivedata.observe(this) { result ->
            val products = result.data

            Log.d(TAG_SEARCH_LIST, "==== Data from API ====")
            products.forEachIndexed { index, item ->
                Log.d(
                    TAG_SEARCH_LIST,
                    "[$index] id=${item.product_id}, packId=${item.distribution_pack_id}, " + "name='${item.product_name}', pack='${item.pack_product_description}', " + "uom='${item.uom}', stock='${item.stock_quantity}', cartQty='${item.cart_quantity}'"
                )
            }

            if (products.isNotEmpty()) {

                // main list
                val mainListAdapter =
                    PosSearchAdapter(products, this@PointOfSaleActivity) { clickitem ->
                        sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                        updateQuantity(clickitem)

                        positem_adapter = PointofsaleItemAdapter(
                            this@PointOfSaleActivity,
                            posItemList,
                            this@PointOfSaleActivity,
                            this@PointOfSaleActivity
                        )
                        binding.positemRcv.adapter = positem_adapter

                        refreshCartUI()
                    }
                binding.addproductLayout.adapter = mainListAdapter
                binding.addproductLayout.isVisible = posItemList.isEmpty()

                // bottom-sheet list
                val sheetListAdapter =
                    PosSearchAdapter(products, this@PointOfSaleActivity) { clickitem ->
                        sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                        updateQuantity(clickitem)

                        positem_adapter = PointofsaleItemAdapter(
                            this@PointOfSaleActivity,
                            posItemList,
                            this@PointOfSaleActivity,
                            this@PointOfSaleActivity
                        )
                        binding.positemRcv.adapter = positem_adapter

                        refreshCartUI()
                    }

                incudebinding.searchstockRcv.adapter = sheetListAdapter
                incudebinding.searchstockRcv.isVisible = true
                incudebinding.noDataFound.isVisible = false
                incudebinding.searchstockRcv.scrollToPosition(0)

            } else {
                binding.addproductLayout.isVisible = false
                incudebinding.searchstockRcv.isVisible = false
                incudebinding.noDataFound.isVisible = true
            }
        }


        // sheet loader
        pos_viewmodel.loadingLiveData.observe(this) {
            incudebinding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        // CART TOTALS OBSERVER
        pos_viewmodel.posAddtocartLivedata.observe(this) { addtocartres ->

            // When user taps bottom bar "Next", navigate with validated totals
            binding.nextlayout.setOnClickListener {
                if (posItemList.isNotEmpty()) {
                    if (!posItemList.any {
                            BatchUtils.getTotalPosCartQuantity(it.batch).toDouble() == 0.0
                        }) {

                        val isTotalizerEnabled = FeatureManager.isEnabled("totalizer")
                        if (!isTotalizerEnabled || !posItemList.any { it.dispense_status.toInt() == 1 }) {

                            val total_amount = FunUtils.stringToDouble(addtocartres.grand_total)

                            if (total_amount > 0) {
                                val intent = Intent(
                                    this@PointOfSaleActivity, PointofSaleDetailsActivity::class.java
                                )
                                intent.putExtra("saleitem", addtocartres)
                                intent.putExtra("c_name", c_name)
                                intent.putExtra("c_mobile", c_mobile)
                                intent.putExtra("c_tpin", c_tpin)
                                intent.putExtra("c_id", c_id)
                                intent.putExtra("spot_discount_percent", appliedSpotDiscountPercent)
                                intent.putExtra("total_amount", total_amount)
                                startActivity(intent)
                            } else {
                                showMessage("Please,Try again later..")
                            }
                        } else {
                            showMessage("Please dispanse all loose item")
                        }
                    } else {
                        showMessage("Quantity Of a Item can't be zero or empty")
                    }
                } else {
                    showMessage("Please Add Atleast one item")
                }
            }

            val gson = Gson()
            val json = gson.toJson(addtocartres)
            Log.d("res", json)

            // --- FIX AREA START ---
            val payableTotalBig = try {
                BigDecimal(addtocartres.grand_total.toString())
            } catch (e: Exception) {
                // fallback: if parsing fails for some reason, just fall back to backend grand_total
                BigDecimal(addtocartres.grand_total.toString()).setScale(2, RoundingMode.HALF_UP)
            }
            // --- FIX AREA END ---

            binding.apply {
                paymentcard.isVisible = true

                // Subtotal
                subtotal.text = NumberFormatter().formatPrice(
                    addtocartres.sub_total.toString(), localizationData
                )
                spotDiscountPercentField.text = "(-) Spot Discount ${addtocartres.spot_discount_percentage}%"
                spotDiscountAmountValue.text = NumberFormatter().formatPrice(addtocartres.spot_discount_amount.toString(), localizationData)
                spotDiscountRow.isVisible = addtocartres.spot_discount_amount.toDoubleOrNull()?.let { it > 0.0 } ?: false

                // 🔹 DYNAMIC TAX RATE DISPLAY
                val uniqueTaxRates = addtocartres.data
                    .mapNotNull { it.taxrate }
                    .filter { it.toIntOrNull() != 0 }  // ✅ Filter out 0% tax
                    .distinct()
                    .sorted()

                val taxLabel = when {
                    uniqueTaxRates.isEmpty() -> "(+) Tax @0%"
                    uniqueTaxRates.size == 1 -> "(+) Tax @${uniqueTaxRates[0]}%"
                    else -> "(+) Tax @${uniqueTaxRates.joinToString(", ")}%"
                }

                taxfield.text = taxLabel


                // Tax amount (keep paise)
                taxAmount.text = NumberFormatter().formatPrice(
                    addtocartres.tax_amount.toString(), localizationData
                )

                // 🔹 HANDLE DISCOUNT ROW VISIBILITY
                val discountAmt = FunUtils.stringToDouble(
                    addtocartres.discount_amount.toString()
                )

                if (discountAmt > 0.0) {
                    // show row
                    delChargeLayout.isVisible = true
                    discountvalue.text = NumberFormatter().formatPrice(
                        discountAmt.toString(), localizationData
                    )
                } else {
                    // hide row if no discount
                    delChargeLayout.isVisible = false
                    discountvalue.text = NumberFormatter().formatPrice(
                        "0", localizationData
                    )
                }

                // Grand total (your existing logic)
                alltotalAmount.text =
                    NumberFormatter().formatPrice(
                        payableTotalBig.setScale(0, RoundingMode.HALF_UP).toPlainString(),
                        localizationData
                    )

                rlPrice.text = NumberFormatter().formatPrice(
                    payableTotalBig.setScale(0, RoundingMode.HALF_UP).toPlainString(),
                    localizationData
                )

                itemno.text = "${addtocartres.data.size} items"

                relativeLayout.isVisible = addtocartres.data.isNotEmpty()
                paymentcard.isVisible = addtocartres.data.isNotEmpty()
            }

        }

        // search typing in bottom sheet
        incudebinding.searchBar.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener,
                androidx.appcompat.widget.SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    pos_viewmodel.callSearchStoreProductApi(
                        newText ?: "", storeid, customerid = c_id, this@PointOfSaleActivity
                    )
                    return false
                }
            })
    }

    private fun preparePositemRCV() {
        binding.positemRcv.apply {
            layoutManager = LinearLayoutManager(
                this@PointOfSaleActivity, RecyclerView.VERTICAL, false
            )
        }
    }

    private fun enableBackButton() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "New Activity"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.svg_back_arrow_white)
        }
        binding.toolbar.setNavigationOnClickListener { handleBackWithConfirm() }
    }

    override fun onSupportNavigateUp(): Boolean {
        handleBackWithConfirm()
        return true
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@PointOfSaleActivity, msg, Toast.LENGTH_SHORT).show()
    }

    // ---- DISPENSE RESULT ----
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            canpressback = false

            val dis_quantity = data?.getDoubleExtra("dis_quantity", 0.0)
            val pro_id = data?.getIntExtra("pro_id", 0)
            val dis_id = data?.getIntExtra("dis_id", 0)

            val existingItemIndex = posItemList.indexOfFirst {
                it.product_id == pro_id && it.distribution_pack_id == dis_id
            }
            val batchX = posItemList[existingItemIndex].batch.toMutableList()
            val updatedBatchX = batchX[0].copy(batch_cart_quantity = dis_quantity ?: 0.0)
            batchX[0] = updatedBatchX

            val updatedItemx = posItemList[existingItemIndex].copy(batch = batchX, dispense_status = 2)

            posItemList[existingItemIndex] = updatedItemx

            if (binding.positemRcv.isComputingLayout) {
                binding.positemRcv.post {
                    positem_adapter.notifyItemChanged(existingItemIndex)
                }
            } else {
                positem_adapter.notifyItemChanged(existingItemIndex)
            }

            refreshCartUI()
            logCartList()

        } else if (requestCode == 200 && resultCode != Activity.RESULT_OK) {
            showMessage("Sorry, dispensing was not successful from the DU.")
        }
    }

    //new
    override fun onDeleteItemClicked(item: StoreProData) {
        val position = posItemList.indexOf(item)
        if (position != -1) {

            // Reset all batch_cart_quantity and any per-batch state
            posItemList[position].batch.forEach { batch ->
                batch.batch_cart_quantity = 0.0
                // if you track anything else (discount per selection, etc.) reset here too if needed
            }

            posItemList.removeAt(position)

            if (binding.positemRcv.isComputingLayout) {
                binding.positemRcv.post {
                    positem_adapter.notifyItemRemoved(position)
                    refreshCartUI()
                    calladdToCartAPI()
                    logCartList()
                }
            } else {
                positem_adapter.notifyItemRemoved(position)
                refreshCartUI()
                calladdToCartAPI()
                logCartList()
            }
        }
    }
    private fun setupSpotDiscountUI() {

        binding.checkboxYes.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkboxNo.isChecked = false
                binding.spotDiscountInputLayout.isVisible = true
                binding.spotDiscountInput.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.spotDiscountInput, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        binding.spotDiscountInput.filters = arrayOf(object : InputFilter {
            override fun filter(
                source: CharSequence?, start: Int, end: Int,
                dest: Spanned?, dstart: Int, dend: Int
            ): CharSequence? {
                val result = dest.toString().substring(0, dstart) +
                        source +
                        dest.toString().substring(dend)
                val pattern = Regex("^\\d{0,3}(\\.\\d{0,2})?$")
                return if (pattern.matches(result)) null else ""
            }
        })

        binding.checkboxNo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkboxYes.isChecked = false
                binding.spotDiscountInputLayout.isVisible = false
                binding.spotDiscountInput.setText("")
                appliedSpotDiscountPercent = 0.0
            }
        }

        binding.spotDiscountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().toDoubleOrNull() ?: return
                if (input > maxSpotDiscountLimit) {
                    binding.spotDiscountInput.setText("")
                    appliedSpotDiscountPercent = 0.0
                    showMessage("Spot discount cannot exceed {$maxSpotDiscountLimit.toString()) %")  // ← updated
                } else {
                    appliedSpotDiscountPercent = input
                    Log.d("SpotDiscount", "Applied discount: $appliedSpotDiscountPercent%")
                    calladdToCartAPI()
                }
            }
        })
    }


    override fun onQuantityChange(position: Int, newBatchList: List<PosSaleBatch>) {
        val oldBatchList = posItemList[position].batch

        // 🔹 Merge discounts: if new discount is 0.0 but old had a value, keep the old one
        val mergedBatchList = newBatchList.map { newBatch ->
            val matchingOld = oldBatchList.find { it.batch_no == newBatch.batch_no }

            if (matchingOld != null) {
                // If adapter sent discount = 0.0 but old had a discount, restore it
                if (newBatch.discount == 0.0 && matchingOld.discount != 0.0) {
                    newBatch.copy(discount = matchingOld.discount)
                } else {
                    newBatch
                }
            } else {
                newBatch
            }
        }

        posItemList[position].batch = mergedBatchList

        // 🔹 Debug log to confirm
        mergedBatchList.forEach { b ->
            Log.d(
                "POS_DISCOUNT_DEBUG",
                "onQuantityChange -> batch_no=${b.batch_no}, qty=${b.batch_cart_quantity}, price=${b.price}, discount=${b.discount}"
            )
        }

        calladdToCartAPI()

        if (binding.positemRcv.isComputingLayout) {
            binding.positemRcv.post {
                positem_adapter.notifyItemChanged(position)
            }
        } else {
            positem_adapter.notifyItemChanged(position)
        }
        refreshCartUI()
        logCartList()
    }


    private fun calladdToCartAPI() {

        Log.d("POS_DISCOUNT_DEBUG", "---- Building AddToCart ----")
        posItemList.forEachIndexed { idx, storeItem ->
            storeItem.batch.forEachIndexed { bIdx, b ->
                Log.d(
                    "POS_DISCOUNT_DEBUG",
                    "cart item[$idx] batch[$bIdx]: batch_no=${b.batch_no}, qty=${b.batch_cart_quantity}, price=${b.price}, discount=${b.discount}"
                )
            }
        }

        // Check if online or offline
        if (!NetworkUtils.isInternetAvailable(this)) {
            Log.d("POS_OFFLINE", "Calculating cart totals offline")

            // ✅ Pass appliedSpotDiscountPercent to offline calculator
            val offlineResult = OfflineCartCalculator.calculateCartTotals(posItemList, appliedSpotDiscountPercent)

            val gson = Gson()
            val json = gson.toJson(offlineResult)
            Log.d("res", json)

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                pos_viewmodel.posAddtocartData.value = offlineResult
            }, 100)

            return
        }


        // ✅ ONLINE MODE: Call API as usual
        val cartitemlist = mutableListOf<CartProductItem>()

        posItemList.forEach { storeItem ->
            val batchCartItemList = storeItem.batch.map { posSaleBatch ->
                BatchCartItem(
                    batchno = posSaleBatch.batch_no,
                    quantity = posSaleBatch.batch_cart_quantity.toInt(),
                    retail_price = posSaleBatch.price,
                    discount = posSaleBatch.discount
                )
            }
            cartitemlist.add(
                CartProductItem(
                    distribution_pack_id = storeItem.distribution_pack_id,
                    product_id = storeItem.product_id,
                    batch = batchCartItemList
                )
            )
        }

        val cart_data = PosAddToCartReq(
            store_id = storeid,
            spot_discount_percentage = appliedSpotDiscountPercent,
            products = cartitemlist
        )
        pos_viewmodel.callAddtoCartPosApi(cart_data, this@PointOfSaleActivity)

        val gson = Gson()
        val json = gson.toJson(cart_data)
        Log.d("reqx", json)
    }




    /**
     * When a product (either loose or normal) is chosen from product list
     */
    fun updateQuantity(clickitem: StoreProData) {
        val existingItemIndex = posItemList.indexOfFirst {
            it.product_id == clickitem.product_id && it.distribution_pack_id == clickitem.distribution_pack_id
        }

        if (existingItemIndex != -1) {
            if (posItemList[existingItemIndex].dispense_status == 2) {
                showMessage("Can't Update the Dispensed Item")
                return
            }
            showMessage("Item already added to cart")
        } else {
            // ✅ UPDATED: Check total batch quantity instead of stock_quantity
            val totalBatchQty = clickitem.batch.sumOf { it.quantity }
            if (totalBatchQty <= 0) {
                showMessage("Out of stock - cannot add to cart")
                return
            }

            if (FunUtils.isLooseOil(
                    clickitem.category_id, clickitem.pack_product_description
                ) && FeatureManager.isEnabled("totalizer")
            ) {
                clickitem.dispense_status = 1
                clickitem.cart_quantity = 1.0
                posItemList.add(clickitem)
            } else {
                clickitem.dispense_status = 0
                clickitem.cart_quantity = 1.0
                posItemList.add(clickitem)
            }
        }

        calladdToCartAPI()
        logCartList()
    }



    private fun barcodeProductSaleBottomSheetRCV(item: List<StoreProData>) {
        val rcv_binding = BarcodeProductsaleRcvBottomsheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(rcv_binding.root)
        dialog.setCancelable(true)

        rcv_binding.searchstockRcv.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        // log barcode search list as well
        logSearchList(item)

        if (item.isNotEmpty()) {
            val adapter = PosSearchAdapter(item, this@PointOfSaleActivity) { clickitem ->
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                dialog.dismiss()

                updateQuantity(clickitem)

                positem_adapter = PointofsaleItemAdapter(
                    this, posItemList, this, this
                )
                binding.positemRcv.adapter = positem_adapter

                refreshCartUI()
            }
            rcv_binding.searchstockRcv.adapter = adapter
            rcv_binding.searchstockRcv.isVisible = true
        } else {
            rcv_binding.searchstockRcv.isVisible = false
        }

        pos_viewmodel.loadingLiveData.observe(this) {
            incudebinding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        dialog.show()
    }

    private fun barcodeProductSaleBottomSheet(productitem: StoreProData) {
        val d_binding = BarcodeProductsaleBottomsheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(d_binding.root)
        dialog.setCancelable(true)

        d_binding.saveBtn.setOnClickListener { dialog.dismiss() }

        d_binding.itemName.text = productitem.product_name
        d_binding.itemType.text = productitem.pack_product_description

        val formattedPrice =
            NumberFormatter().formatPrice(productitem.retail_price ?: "-", localizationData)
        d_binding.itemPrice.text = formattedPrice

        if (productitem.stock_quantity > 0) {
            d_binding.addlayout.isVisible = true
            d_binding.itemUnit.text = "${productitem.stock_quantity} Units"
            d_binding.itemUnit.setTextColor(Color.parseColor("#008000"))
            d_binding.addcart.setOnClickListener {
                updateQuantity(productitem)
                dialog.dismiss()

                positem_adapter = PointofsaleItemAdapter(
                    this, posItemList, this, this
                )
                binding.positemRcv.adapter = positem_adapter

                refreshCartUI()
            }

        } else {
            d_binding.quantityContainer.isVisible = false
            d_binding.itemUnit.text = "Out Of Stock"
            d_binding.itemUnit.setTextColor(Color.parseColor("#FF0000"))
        }

        Glide.with(this).load(Constants.IMAGE_URL + productitem.product_photo).centerCrop()
            .placeholder(R.drawable.temp).error(R.drawable.temp).into(d_binding.productimg)

        dialog.show()
    }

    private fun setToolbarImage() {
        val organisation_data = OrganisationDetailsHelper(this).getOrganisationData()

        Glide.with(this).load(organisation_data.image_url + organisation_data.fabicon).fitCenter()
            .placeholder(R.drawable.mlogo).error(R.drawable.mlogo).into(binding.image)
    }

    private fun startScanning() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Scan a QR code or barcode")
        options.setOrientationLocked(true)
        scanQrResultLauncher.launch(ScanContract().createIntent(this, options))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(
                    this, "Camera permission is required", Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    fun dismissKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun handleBackWithConfirm() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }

        if (!canpressback) {
            showMessage("Please sell the dispensed item first")
            return
        }

        showBackConfirmDialog()
    }

    private fun showBackConfirmDialog() {
        AlertDialog.Builder(this).setTitle("Leave this screen?")
            .setMessage("Are you sure you want to go back?").setCancelable(true)
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Yes") { _, _ -> finish() }.show()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBackWithConfirm()
    }
}
