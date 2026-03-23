package com.retailone.pos.ui.Activity.DashboardActivity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.retailone.pos.R
import com.retailone.pos.adapter.StockSearchAdapter
import com.retailone.pos.databinding.ActivityStockRequsitionSearchBinding
import com.retailone.pos.databinding.BarcodeProductreqBottomsheetBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.models.CommonModel.StockRequsition.SearchResData
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.network.Constants
import com.retailone.pos.viewmodels.DashboardViewodel.StockRequsitionSearchViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
//test
class StockRequsitionSearchActivity : AppCompatActivity() {
    lateinit var  binding: ActivityStockRequsitionSearchBinding
    lateinit var stockSearchViewmodel: StockRequsitionSearchViewmodel
    lateinit var stockSearchAdapter: StockSearchAdapter
    lateinit var sharedPrefHelper : SharedPrefHelper
    lateinit var  localizationData : LocalizationData

    private val PERMISSION_REQUEST_CAMERA = 1
    private lateinit var scanQrResultLauncher: ActivityResultLauncher<Intent>
    var textWatcher: TextWatcher? = null

    private var storeid = ""
   /* private var storeExposure: Double = 0.0
    private var actualStoreExposure: Long = 0*/


    private var storeExposure: Double = 0.0
    private var actualStoreExposure: Long = 0L
    private var totalCartValue: Double = 0.0

    private var finalExposure: Double = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("DebugTest", "Activity started")
        super.onCreate(savedInstanceState)
        binding = ActivityStockRequsitionSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stockSearchViewmodel = ViewModelProvider(this)[StockRequsitionSearchViewmodel::class.java]

        sharedPrefHelper = SharedPrefHelper(this)

        localizationData = LocalizationHelper(this).getLocalizationData()

        val loginSession = LoginSession.getInstance(this)
        Log.d("batch","test1")
        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first()
            stockSearchViewmodel.callStockSearchApi("",storeid,this@StockRequsitionSearchActivity)

        }


        prepareSearchRCV()
        Log.d("batch","test2")

        scanQrResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultData ->
            if (resultData.resultCode == RESULT_OK && resultData.data != null) {
                val result = ScanIntentResult.parseActivityResult(resultData.resultCode, resultData.data)
                Toast.makeText(this, "Scannedbefore: " + result.contents, Toast.LENGTH_LONG).show()
                // ✅ Print/log the scanned value
               // Log.d("ScannedBarcode", "Scanned Value: ${result.contents}")
              //  println("Scanned Value: ${result.contents}")  // Optional
                // This line may be skipped if `result.contents` is null
                Log.d("ScanResult", "Raw scan result: $result")
                if (result.contents == null) {
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show()
                    Log.d("ScanResult", "Scan was cancelled or empty")
                } else {
                   // binding.debugScanText.text = "Scanned: ${result.contents}"

                    Log.d("ScanResult", "Scanned barcode: ${result.contents}")
                    println("✅ Scanned: ${result.contents}")
                   /// binding.searchBar.setQuery(result.contents.toString(),true)
                    Toast.makeText(this, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()

                    stockSearchViewmodel.callStockSearchBarcodeApi(result.contents.toString(),storeid,this@StockRequsitionSearchActivity)

                }
            }
        }
        Log.d("batch","test3")
        binding.barcodeimage.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this@StockRequsitionSearchActivity,
                        arrayOf(android.Manifest.permission.CAMERA),
                        PERMISSION_REQUEST_CAMERA
                    )
                } else {
                    startScanning()
                }
            } else {
                startScanning()
            }

        }

        Log.d("batch","test4")

        stockSearchViewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }
        Log.d("batch","test5")
        stockSearchViewmodel.stockSearchBarcodeLivedata.observe(this){

            if(it.data.isNotEmpty()){
                //showMessage(it.data[0].product_name)
                barcodeProductBottomSheet(it.data[0])

            }else{
                showMessage("Scanned product currently unavailable")
            }

            /*    binding.searchstockRcv.isVisible = true
            binding.relativeLayout.isVisible = false*/


        }

        Log.d("batch","test6")
       /* stockSearchViewmodel.stockSearchLivedata.observe(this){

            if(it.data.isNotEmpty()){
                stockSearchAdapter = StockSearchAdapter(it.data,this@StockRequsitionSearchActivity)
                binding.searchstockRcv.adapter = stockSearchAdapter
                binding.searchstockRcv.isVisible = true
                binding.noDataFound.isVisible = false
            }else{
                binding.searchstockRcv.isVisible = false
                binding.noDataFound.isVisible = true
            }

            *//*    binding.searchstockRcv.isVisible = true
            binding.relativeLayout.isVisible = false*//*


        }*/
        Log.d("batch","test7")
        stockSearchViewmodel.stockSearchLivedata.observe(this) { response ->
            if (response.data.isNotEmpty()) {
                //storeExposure = response.store_exposure ?: 0.0
               // actualStoreExposure = response.actual_store_exposure_limit: Long?

                // 🔥 SAFELY parse any type
                storeExposure = when (val se = response.store_exposure) {
                    is Int -> se.toDouble()
                    is Long -> se.toDouble()
                    is Double -> se
                    is String -> se.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }

                actualStoreExposure = when (val ase = response.actual_store_exposure_limit) {
                    is Int -> ase.toLong()
                    is Long -> ase
                    is Double -> ase.toLong()
                    is String -> ase.toLongOrNull() ?: 0L
                    else -> 0L
                }

                Log.d("output1",storeExposure.toString())
                Log.d("output2",actualStoreExposure .toString())

                stockSearchAdapter = StockSearchAdapter(response.data, this@StockRequsitionSearchActivity) { total ->
                    totalCartValue = total
                    // Do not validate here
                }

                binding.searchstockRcv.adapter = stockSearchAdapter
                binding.searchstockRcv.isVisible = true
                binding.noDataFound.isVisible = false
            } else {
                binding.searchstockRcv.isVisible = false
                binding.noDataFound.isVisible = true
            }
        }

        binding.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                stockSearchViewmodel.callStockSearchApi(newText?:"",storeid,this@StockRequsitionSearchActivity)

                return false
            }

            })

        Log.d("batch","test8")
      /*  binding.relativeLayout.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("sucess","true")
           // resultIntent.putExtra("id", service.id)
           // resultIntent.putExtra("service_name", service.service_name)
            this.setResult(Activity.RESULT_OK, resultIntent)
            this.finish()
        }
*/
        /*binding.relativeLayout.setOnClickListener {
            val finalExposure = storeExposure + totalCartValue

            // Store the finalExposure in Shared Preferences
            sharedPrefHelper.setFinalExposure(finalExposure)
            Log.d("final:",finalExposure.toString()+"actual:"+actualStoreExposure,)
            if (finalExposure > actualStoreExposure) {
                Toast.makeText(this, "❌ Total exceeds actual store exposure!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val resultIntent = Intent()

            resultIntent.putExtra("sucess", "true")
            resultIntent.putExtra("final_exposure", finalExposure)
            resultIntent.putExtra("actual_exposure", actualStoreExposure)
            this.setResult(Activity.RESULT_OK, resultIntent)
            this.finish()
        }*/
        binding.relativeLayout.setOnClickListener {

            // ✅ Sync latest quantities first
            stockSearchAdapter.syncInputs()



            val totalCartValue = sharedPrefHelper.getTotalCartValue()
            finalExposure = storeExposure + totalCartValue

            Log.d("final:", "🧾 Cart: $totalCartValue | Final: $finalExposure | Limit: $actualStoreExposure")

            // ✅ Check limit breach
            if (finalExposure > actualStoreExposure) {
                AlertDialog.Builder(this)
                    .setTitle("❌ Limit Exceeded")
                    .setMessage("Total exposure ₹$finalExposure exceeds allowed limit ₹$actualStoreExposure.")
                    .setPositiveButton("OK") { _, _ ->
                        // Still return data
                        val resultIntent = Intent()
                        resultIntent.putExtra("sucess", "false")
                        resultIntent.putExtra("final_exposure", finalExposure)
                        resultIntent.putExtra("actual_exposure", actualStoreExposure)
                        resultIntent.putExtra("store_exposure", storeExposure)
                        setResult(Activity.RESULT_OK, resultIntent)
                        Log.d("PASS_BACK", "Returning from exceeded: final=$finalExposure, actual=$actualStoreExposure")
                        finish()
                    }
                    .show()
                return@setOnClickListener
            }

            // ✅ Proceed if within limit
            val resultIntent = Intent()
            resultIntent.putExtra("sucess", "true")
            resultIntent.putExtra("final_exposure", finalExposure)
            resultIntent.putExtra("actual_exposure", actualStoreExposure)
            resultIntent.putExtra("store_exposure", storeExposure)
            setResult(Activity.RESULT_OK, resultIntent)
            Log.d("PASS_BACK", "Returning exposure -> final: $finalExposure, actual: $actualStoreExposure")
            finish()
        }

        //default

        //stockSearchViewmodel.callStockSearchApi("",storeid,this@StockRequsitionSearchActivity)


    }



    private fun showMessage(msg: String) {
        Toast.makeText(this@StockRequsitionSearchActivity, msg, Toast.LENGTH_SHORT).show()
    }



    private fun prepareSearchRCV() {

        binding.searchstockRcv.apply {
            layoutManager = LinearLayoutManager(this@StockRequsitionSearchActivity,
                RecyclerView.VERTICAL,false)
        }
    }


    private fun startScanning() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Scan a QR code or barcode")
        options.setOrientationLocked(true)
        scanQrResultLauncher.launch(ScanContract().createIntent(this, options))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        @NonNull permissions: Array<String>,
        @NonNull grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun barcodeProductBottomSheet(productitem: SearchResData) {

        val d_binding = BarcodeProductreqBottomsheetBinding.inflate(layoutInflater)

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(d_binding.root)

        dialog.setCancelable(false)

       /* viewmodel.loadingLiveData.removeObservers(this) // Remove previous observers to avoid re-triggering


        // Observing loading state
        viewmodel.loadingLiveData.observe(this) {
            d_binding.progress.isVisible = it.isProgress
        }

        viewmodel.get_customer_liveData.removeObservers(this)*/ // Remove previous observers to avoid re-triggering
        d_binding.saveBtn.setOnClickListener {

            //default
            dialog.dismiss()

            stockSearchViewmodel.callStockSearchApi("",storeid,this@StockRequsitionSearchActivity)
        }



        d_binding.quantEdit.removeTextChangedListener(textWatcher)

        // Create a new TextWatcher for the current item
        textWatcher = createTextWatcher(d_binding, productitem)
        d_binding.quantEdit.addTextChangedListener(textWatcher)




        d_binding.itemName.text = productitem.product_name
        d_binding.itemType.text = productitem.pack_product_description
        d_binding.bottomsheetText.text = productitem.barcode

//        val formattedPrice =
//            NumberFormatter().formatPrice(productitem.whole_sale_price, localizationData)
//        d_binding.itemPrice.text = formattedPrice

        if (productitem.stock_quantity.toInt() > 0) {
            d_binding.quantityContainer.isVisible = true

            //manage visibility of add button
            if (sharedPrefHelper.isProductAdded(
                    productitem.product_id,
                    productitem.distribution_pack_id
                )
            ) {
                d_binding.addlayout.isVisible = false // Product is already added, hide the add button
                d_binding.quantLayout.isVisible = true // Show the quantity EditText
                d_binding.quantEdit.text = Editable.Factory.getInstance().newEditable(
                    sharedPrefHelper.getQuantity(
                        productitem.product_id,
                        productitem.distribution_pack_id
                    )
                )
            } else {
                d_binding.addlayout.isVisible = true // Show the add button
                d_binding.quantLayout.isVisible = false // Hide the quantity EditText
            }

            //holder.binding.addlayout.isVisible = true
            // holder.binding.quantLayout.isVisible = false

            d_binding.itemUnit.text = "${productitem.stock_quantity} Units"
            d_binding.itemUnit.setTextColor(Color.parseColor("#008000"))

            d_binding.addcart.setOnClickListener {
                sharedPrefHelper.saveSearchItem(productitem)
                //initial quantity ad
                sharedPrefHelper.updateQuantity(
                    productitem.product_id,
                    productitem.distribution_pack_id,
                    "1"
                )

                d_binding.addlayout.isVisible = false
                // holder.binding.plusMinusLayout.isVisible = true
                //holder.binding.cartProductQuantity.text = "1"
                d_binding.quantEdit.text = Editable.Factory.getInstance().newEditable("1")
                d_binding.quantLayout.isVisible = true
                d_binding.quantEdit.requestFocus()

            }

        } else {
          d_binding.quantityContainer.isVisible = false
          d_binding.itemUnit.text = "Out Of Stock"
          d_binding.itemUnit.setTextColor(Color.parseColor("#FF0000"))
        }


        Glide.with(this)
            .load(Constants.IMAGE_URL + productitem.product_photo)
            .centerCrop() // Add center crop
            .placeholder(R.drawable.temp) // Add a placeholder drawable
            .error(R.drawable.temp) // Add an error drawable (if needed)
            .into(d_binding.productimg)



        // Show the dialog
        dialog.show()
    }

    private fun createTextWatcher(tbinding: BarcodeProductreqBottomsheetBinding, productitem: SearchResData): TextWatcher? {
        var oldnum = 0
        var isProgrammaticChange = false

        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isProgrammaticChange) {
                    try {
                        oldnum = s.toString().toInt()
                    } catch (e: NumberFormatException) {
                        // Handle the exception if needed
                    }
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticChange) return

                try {
                    val enteredValue = s.toString().toIntOrNull() ?: 0

                    if (enteredValue == 0) {
                        Toast.makeText(this@StockRequsitionSearchActivity, "Product Quantity can't be zero", Toast.LENGTH_SHORT).show()

                        isProgrammaticChange = true
                        tbinding.quantEdit.text = Editable.Factory.getInstance().newEditable("")

                        sharedPrefHelper.updateQuantity(
                            productitem.product_id,
                            productitem.distribution_pack_id,
                            "0"
                        )
                    } else if (enteredValue <= productitem.stock_quantity.toInt()) {
                        sharedPrefHelper.updateQuantity(
                            productitem.product_id,
                            productitem.distribution_pack_id,
                            enteredValue.toString()
                        )
                    } else {
                        Toast.makeText(this@StockRequsitionSearchActivity, "Can't add ${productitem.product_name} more than Stocks", Toast.LENGTH_SHORT).show()

                        isProgrammaticChange = true
                        if (oldnum > 0) {
                            tbinding.quantEdit.text = Editable.Factory.getInstance().newEditable(oldnum.toString())
                        } else {
                            tbinding.quantEdit.text = Editable.Factory.getInstance().newEditable("")
                        }
                        tbinding.quantEdit.setSelection(tbinding.quantEdit.text.length)
                    }
                } catch (e: NumberFormatException) {
                    // Handle the exception if needed
                } finally {
                    isProgrammaticChange = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Perform actions after the text has changed if needed
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        //textWatcher.
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra("sucess", "false")
        resultIntent.putExtra("final_exposure", finalExposure)
        resultIntent.putExtra("actual_exposure", actualStoreExposure)
        resultIntent.putExtra("store_exposure", storeExposure)
        setResult(Activity.RESULT_OK, resultIntent)
        Log.d("PASS_BACK", "Returning onBackPressed -> final=$finalExposure, actual=$actualStoreExposure")
        super.onBackPressed()
    }



}