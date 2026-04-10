package com.retailone.pos.ui.Activity.DashboardActivity

import NumberFormatter
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.retailone.pos.R
import com.retailone.pos.adapter.ExpenseRegisterAdapter.ExpenseCategoryAdapter
import com.retailone.pos.adapter.ExpenseRegisterAdapter.ExpenseHistoryAdapter
import com.retailone.pos.adapter.ExpenseRegisterAdapter.ExpenseVendorAdapter
import com.retailone.pos.databinding.ActivityExpenseRegisterBinding
import com.retailone.pos.databinding.BottomsheetImageUploadBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseCategory.ExpenseCategoryData
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseHistory.ExpenseHistoryReq
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseSubmit.ExpenseSubmitReq
import com.retailone.pos.models.ExpenseRegisterModel.ExpenseVendor.ExpenseVendorData
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.utils.NetworkUtils
import com.retailone.pos.viewmodels.DashboardViewodel.ExpenseRegisterViewmodel
import com.retailone.pos.viewmodels.DashboardViewodel.PettycashDetailsViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ExpenseRegisterActivity : AppCompatActivity() {
    lateinit var binding: ActivityExpenseRegisterBinding
    lateinit var viewmodel: ExpenseRegisterViewmodel
    lateinit var pettycash_viewmodel: PettycashDetailsViewmodel
    private var selectedExpenseDate: String? = null
    lateinit var loginSession: LoginSession

    lateinit var localizationData: LocalizationData
    private val calendar = Calendar.getInstance()


    // var categorydata:ExpenseCategoryRes? = null
    var categoryList: MutableList<ExpenseCategoryData> = mutableListOf()

    //val categoryList = mutableListOf<ExpenseCategoryData>()
    var vendorList: MutableList<ExpenseVendorData> = mutableListOf()

    lateinit var fileProviderUri: Uri
    private var PERMISSIONS: Array<String> = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>


    var isImgSelected = false
    var invoiceFilePath = ""
    lateinit var expenseSubmitReq: ExpenseSubmitReq

    var catValue = ""
    var vendValue = ""
    var catType = "NORMAL"
    var vendType = "NORMAL"

    var storeid = ""
    var store_manager_id = ""

    var expence_amount = 0.0
    var vat_percent = 0  //vat value not percent only integer
    var total_amount = 0.0

    var pettycashtotal_amount = 0.0


    var sdcNo = ""
    var receiptNo = ""
    var remark = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExpenseRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewmodel = ViewModelProvider(this)[ExpenseRegisterViewmodel::class.java]
        pettycash_viewmodel = ViewModelProvider(this)[PettycashDetailsViewmodel::class.java]

        localizationData = LocalizationHelper(this).getLocalizationData()


        loginSession = LoginSession.getInstance(this)
        val localizationData = LocalizationHelper(this).getLocalizationData()
        binding.amountLayout.hint = "Enter expense amount (in ${localizationData?.currency})*"




        lifecycleScope.launch {
            // Check if the user is logged in
            val isLoggedIn = loginSession.getLoginStatus().first()
            val token = loginSession.getToken().first()
            storeid = loginSession.getStoreID().first().toString()
            store_manager_id = loginSession.getStoreManagerID().first().toString()

            // ✅ Load history and petty cash immediately (handles offline viewing via cache)
            viewmodel.callExpenseHistoryApi(ExpenseHistoryReq(storeid), this@ExpenseRegisterActivity)
            pettycash_viewmodel.callPettycashDataApi(storeid, this@ExpenseRegisterActivity)

            // ✅ ENFORCE ONLINE-ONLY SUBMISSION & OFFLINE VIEW-ONLY
            if (!NetworkUtils.isInternetAvailable(this@ExpenseRegisterActivity)) {
                // Disable entry tab, force history view
                setupViewHistory()
                binding.expenceEntry.alpha = 0.5f
                binding.expenceEntry.isClickable = false
                binding.saveBtn.isVisible = false
                
                showMessage("Offline Mode: Expense View Only")
            } else {
                binding.expenceEntry.alpha = 1.0f
                binding.expenceEntry.isClickable = true
                binding.saveBtn.isVisible = true
            }
        }


        pettycash_viewmodel.pettycash_liveData.observe(this) {
            //  showMessage(it.petty_cash_summary.size.toString())

            if (it.status == 1) {
                val formattedPricercv = NumberFormatter().formatPrice(
                    it.pettycash_total_balance.toString(),
                    localizationData
                )
                binding.balance.text = formattedPricercv

                pettycashtotal_amount = try {
                    it.pettycash_total_balance
                        ?.replace(",", "") // Remove commas from the string
                        ?.toDouble() ?: 0.0 // Convert to Double// Convert VAT input to Double
                } catch (e: Exception) {
                    0.0 // Default value if conversion fails
                }


            }
        }

        enableBackButton()
        setupExpenseEntry() //by default
        prepareExpenseHistoryRCV()

        setupGalleryLauncher()
        setupCameraLauncher()

        // image upload

        if (Build.VERSION.SDK_INT >= 33) {
            PERMISSIONS = arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
        }

        setupRequesrPermissionLauncher()



        binding.invoiceImage.setOnClickListener {

            if (checkPermission()) {
                openImageUploadBottomsheet()
            } else {
                requestPermissionLauncher.launch(PERMISSIONS)
            }
        }


        viewmodel.callExpenseCategoryApi(this)
        viewmodel.callExpenseVendorApi(this)

        viewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            binding.saveBtn.isClickable = !it.isProgress

            if (it.isMessage)
                showMessage(it.message)
        }

        viewmodel.expensecategory_liveData.observe(this) {
            val default_data = ExpenseCategoryData("Others", "", 0, 1, "")
            // val categorylist = categorydata?.data?.toMutableList()
            categoryList.clear()
            categoryList = it?.data?.toMutableList() ?: mutableListOf()
            categoryList.add(default_data)

            binding.categoryInput.setAdapter(ExpenseCategoryAdapter(this, 0, categoryList))

        }

        binding.categoryInput.setOnItemClickListener { parent, view, position, id ->

            binding.categoryInput.setText(categoryList[position].category_name, false)
            // vendorRegisterViewModel.getNewDistrictData(it.states.get(position).id)
            // binding.districtEdit.setText("",false)
            //binding.othCategoryLayout.isVisible = categoryList[position].category_name=="Others"

            if (categoryList[position].category_name == "Others") {
                binding.othCategoryLayout.isVisible = true
                catType = "OTH"
                catValue = "" // value from edit text
            } else {
                binding.othCategoryLayout.isVisible = false
                catType = "NORMAL"
                catValue = categoryList[position].category_name

            }
        }


        binding.pettycashDetails.setOnClickListener {
            startActivity(
                Intent(
                    this@ExpenseRegisterActivity,
                    PettycashDetailsActivity::class.java
                )
            )
        }


        viewmodel.expensevendor_liveData.observe(this) {
            val default_data = ExpenseVendorData(
                vendor_name = "Others",
                created_at = "",
                id = 0,
                status = 1,
                updated_at = ""
            )
            // val categorylist = categorydata?.data?.toMutableList()
            vendorList.clear()
            vendorList = it?.data?.toMutableList() ?: mutableListOf()
            vendorList.add(default_data)

            binding.nameInput.setAdapter(ExpenseVendorAdapter(this, 0, vendorList))
        }

        binding.nameInput.setOnItemClickListener { parent, view, position, id ->

            binding.nameInput.setText(vendorList[position].vendor_name, false)
            // vendorRegisterViewModel.getNewDistrictData(it.states.get(position).id)
            // binding.districtEdit.setText("",false)
            //binding.othNameLayout.isVisible = vendorList[position].vendor_name == "Others"

            if (vendorList[position].vendor_name == "Others") {
                binding.othNameLayout.isVisible = true
                vendType = "OTH"
                vendValue = ""
            } else {
                binding.othNameLayout.isVisible = false
                vendType = "NORMAL"
                vendValue = vendorList[position].vendor_name
            }
        }

        binding.sdcnoInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun beforeTextChanged(
                arg0: CharSequence, arg1: Int, arg2: Int,
                arg3: Int
            ) {
            }

            override fun afterTextChanged(et: Editable) {
                var s = et.toString()
                if (s != s.uppercase(Locale.getDefault())) {
                    s = s.uppercase(Locale.getDefault())
                    binding.sdcnoInput.setText(s)
                    binding.sdcnoInput.setSelection(binding.sdcnoInput.length()) //fix reverse texting
                }
            }
        })


        viewmodel.expensesubmit_liveData.observe(this) {
            showMessage(it.message)
            // recreate()

            refreshPage()
        }

        viewmodel.expensehistory_liveData.observe(this) {

            if (it.data.isNotEmpty()) {
                binding.historyRcv.adapter = ExpenseHistoryAdapter(this, it.data)
            } else {
                showMessage("No History Found")
            }
        }

        viewmodel.invoiceupload_liveData.observe(this) {

            if (it.status == 1) {
                val updatedreqdata = expenseSubmitReq.copy(invoice = it.image_url)
                Log.d("ex", "hii" + Gson().toJson(updatedreqdata).toString())
                viewmodel.callExpenseSubmitApi(updatedreqdata, this)
            } else {
                showMessage("Invoice upload failed")
            }

        }


        binding.expenceEntry.setOnClickListener {
            setupExpenseEntry()
        }

        binding.expenceHistory.setOnClickListener {
            setupViewHistory()
            viewmodel.callExpenseHistoryApi(expenseHistoryReq = ExpenseHistoryReq(storeid), this)
        }

        binding.saveBtn.setOnClickListener {

            sdcNo = binding.sdcnoInput.text.toString()
            receiptNo = binding.receiptnoInput.text.toString()
            remark = binding.descripionInput.text.toString()

            //for checking vat is empty or not
            val vatValuetext = binding.vatInput.text.toString()


            //showMessage(vendType + "-" +vendValue)
            if (catType == "OTH") {
                catValue = binding.othCategoryInput.text.toString()
            }
            if (vendType == "OTH") {
                vendValue = binding.othNameInput.text.toString()
            }

            val amount = binding.amountInput.text.toString()

            if (catValue.isEmpty() || catValue.isBlank()) {
                if (catType == "OTH") {
                    showMessage("Please enter a valid expence type")
                } else {
                    showMessage("Please select expense type")
                }
            } else if (catType == "OTH" && (catValue.trim()
                    .lowercase() == "others" || catValue.trim().lowercase() == "other")
            ) {
                showMessage("Please enter a expence type")
            } else if (amount.isEmpty()) {
                showMessage("Please enter valid expense amount")

            } else if (vendValue.isEmpty() || vendValue.isBlank()) {
                if (vendType == "OTH") {
                    showMessage("Please enter a valid party name")
                } else {
                    showMessage("Please select party name")
                }
            } else if (vendType == "OTH" && (vendValue.trim()
                    .lowercase() == "others" || vendValue.trim().lowercase() == "other")
            ) {
                showMessage("Please enter a valid party name")
            } else if (store_manager_id == "") {
                showMessage("Could't fetch store manager info,Please try again..")

            } else if (sdcNo.isNotEmpty() && sdcNo.isNotBlank() && !validSDC(sdcNo)) {
                showMessage("Please enter a valid SDC No")

            } else if (remark.isEmpty() || remark.isBlank()) {
                showMessage("Please enter a valid remarks")

            } else if (total_amount <= 0) {
                showMessage("Expence Can't be zero")

            } else if (vatValuetext.isEmpty() || vatValuetext.isBlank()) {
                showMessage("Please enter a valid vat amount")

            } else if (expence_amount > 0 && vat_percent >= expence_amount) {
                // binding.vatInput.text = Editable.Factory.getInstance().newEditable("0")
               // showMessage("VAT amount should not exceed the expense amount")
                showMessage("VAT amount cannot be equal or more than expense amount")
            } else if (total_amount > pettycashtotal_amount) {
                showMessage("Can't expence more than the pettycash amount")
            }
//            else if (!isImgSelected || invoiceFilePath == "") {
//                showMessage("Please select invoice image")
//
//            }
            else {
                //showMessage("Sucess")

                binding.saveBtn.isClickable = false //stop click multiple times

                val expenseData = ExpenseSubmitReq(
                    amount = amount,
                    category_name = catValue,
                    vendor_name = vendValue,
                    invoice = "abc.jpg",
                    store_manager_id = store_manager_id,
                    store_id = storeid,
                    expense_date_time =  selectedExpenseDate ?: "",

                    vat = vat_percent.toString(),
                    total_amount = total_amount.toString(),
                    sdc_no = sdcNo.toString(),
                    receipt_no = receiptNo,
                    remarks = remark.toString()

                )
                //  viewmodel.callExpenseSubmitApi(expenseData,this)

                expenseSubmitReq = expenseData

                //showMessage("Sucess")
                /* uploadFile(invoiceFilePath)  //fileformat
                  binding.saveBtn.isClickable = true ////enable click multiple times*/

                //  if (!isImgSelected || invoiceFilePath == ""){
                if (invoiceFilePath == "") {

                    // Log.d("ex","hii"+ Gson().toJson(expenseSubmitReq).toString())

                    // image not compulsory

                    viewmodel.callExpenseSubmitApi(expenseSubmitReq, this)
                    Log.d("request:",expenseSubmitReq.toString())

                } else {
                    uploadFile(invoiceFilePath)  //fileformat
                    binding.saveBtn.isClickable = true
                }


            }

        }

        setToolbarImage()
        getCurrentDate()


        binding.dateInput.setOnClickListener {
            showDateDialog()
        }



        binding.amountInput.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val vatvalue = binding.vatInput.text.toString()

                vat_percent = try {
                    vatvalue.toInt() // Convert VAT input to Double
                } catch (e: Exception) {
                    0 // Default value if conversion fails
                } finally {
                    //   showMessage("vat is $vat_percent")
                }


                // val amoutvalue = binding.amountInput.text.toString()


                expence_amount = try {
                    s.toString().toDouble() // Convert VAT input to Double
                } catch (e: Exception) {
                    0.0 // Default value if conversion fails
                } finally {
                    //showMessage("expence is $expence_amount")
                }

                total_amount = expence_amount + vat_percent
                //binding.totalInput.text = Editable.Factory.getInstance().newEditable(total_amount.toString())


                binding.totalInput.text =
                    Editable.Factory.getInstance().newEditable(total_amount.toString())


            }

            override fun afterTextChanged(s: Editable?) {

            }

        })



        binding.vatInput.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val expencevalue = binding.amountInput.text.toString()

                expence_amount = try {
                    expencevalue.toDouble() // Convert VAT input to Double
                } catch (e: Exception) {
                    0.0 // Default value if conversion fails
                } finally {
                }


                // val amoutvalue = binding.amountInput.text.toString()


                vat_percent = try {
                    s.toString().toInt() // Convert VAT input to Double
                } catch (e: Exception) {
                    0 // Default value if conversion fails
                } finally {
                }


                // total_amount = expence_amount+ (expence_amount*vat_percent)/100
                total_amount = expence_amount + vat_percent
                binding.totalInput.text =
                    Editable.Factory.getInstance().newEditable(total_amount.toString())


                /*  if(vat_percent>=100){
                      binding.vatInput.text = Editable.Factory.getInstance().newEditable("0")
                      showMessage("Please enter a valid vat amount")
                  }*/

                /*  if( expence_amount >0 && vat_percent>=expence_amount){
                      binding.vatInput.text = Editable.Factory.getInstance().newEditable("0")
                      showMessage("VAT amount should not exceed the expense amount")
                  }*/


            }

            override fun afterTextChanged(s: Editable?) {
                // Perform actions after the text has changed if needed
            }

        })


    }

    private fun validSDC(sdcNo: String): Boolean {
        if (sdcNo.length != 12) {
            showMessage("SDC Number should 12 digit")
            return false
        }

        // Check if the first 3 characters are alphabets
        val firstThree = sdcNo.substring(0, 3)
        if (!firstThree.matches(Regex("^[A-Za-z]{3}$"))) {
            showMessage("First 3 digit should be charecter")



            return false

        }

        // Check if the remaining 9 characters are digits
        val lastNine = sdcNo.substring(3)
        if (!lastNine.matches(Regex("^\\d{9}$"))) {
            showMessage("Last 9 digit should be Number")

            return false
        }

        return true // Passes all checks


    }
    private fun showDateDialog() {
        val cashupDateTime = intent.getStringExtra("CASHUP_DATE_TIME")
        Log.d("cashupdate", "Raw cashupDateTime: $cashupDateTime")

        val sdf = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.ENGLISH)

        val cleanedDateString = cashupDateTime?.replace("\"", "")?.trim()
        Log.d("datepicker", "Cleaned cashupDateTime: $cleanedDateString")

        val cashupDate: Date = try {
            val parsed = sdf.parse(cleanedDateString ?: "")
            Log.d("datepicker", "Successfully parsed cashupDate: ${sdf.format(parsed)}")
            parsed
        } catch (e: Exception) {
            Log.e("datepicker", "Failed parsing cleaned cashupDateTime: $cleanedDateString", e)
            Date()
        }

        val minDateCal = Calendar.getInstance()
        minDateCal.time = cashupDate
        minDateCal.add(Calendar.DATE, 1)

        val todayCal = Calendar.getInstance()

        Log.d("datepicker", "cashup: ${sdf.format(cashupDate)} | min: ${sdf.format(minDateCal.time)} | max: ${sdf.format(todayCal.time)}")

       /* val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth, 0, 0, 0)

                val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
                val formattedDate = dateFormat.format(selectedDate.time)
                binding.dateInput.text = Editable.Factory.getInstance().newEditable(formattedDate)
                val apiFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.ENGLISH)
                selectedExpenseDate = apiFormat.format(selectedDate.time)
                Log.d("datepicker", "Selected expense date: $selectedExpenseDate")

            },
            minDateCal.get(Calendar.YEAR),
            minDateCal.get(Calendar.MONTH),
            minDateCal.get(Calendar.DAY_OF_MONTH)
        )*/
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val now = Calendar.getInstance()
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND))

                // show on UI
                val displayFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
                binding.dateInput.text = Editable.Factory.getInstance().newEditable(displayFormat.format(selectedDate.time))

                // pass to API in format "30-Jun-2025 10:34 AM"
                val apiFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.ENGLISH)
                selectedExpenseDate = apiFormat.format(selectedDate.time)
                Log.d("datepicker", "Selected expense date for API: $selectedExpenseDate")
            },
            minDateCal.get(Calendar.YEAR),
            minDateCal.get(Calendar.MONTH),
            minDateCal.get(Calendar.DAY_OF_MONTH)
        )


        datePickerDialog.datePicker.minDate = minDateCal.timeInMillis
        datePickerDialog.datePicker.maxDate = todayCal.timeInMillis

        datePickerDialog.show()
    }



    /*private fun showDateDialog() {
         // Create a DatePickerDialog
         val datePickerDialog = DatePickerDialog(
             this,
             { _, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                 // Create a new Calendar instance to hold the selected date
                 val selectedDate = Calendar.getInstance()
                 // Set the selected date using the values received from the DatePicker dialog
                 selectedDate.set(year, monthOfYear, dayOfMonth)
                 // Set time part to "00:00:00"
                 selectedDate.set(Calendar.HOUR_OF_DAY, 0)
                 selectedDate.set(Calendar.MINUTE, 0)
                 selectedDate.set(Calendar.SECOND, 0)
                 // Create a SimpleDateFormat to format the date as "dd/MM/yyyy"
                 val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                 // Format the selected date into a string
                 val formattedDate = dateFormat.format(selectedDate.time)


                 binding.dateInput.text = Editable.Factory.getInstance().newEditable(formattedDate)

                   // Create another SimpleDateFormat to format the date as "yyyy-MM-dd HH:mm:ss"
                  val secondDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                  // Format the selected date into the "yyyy-MM-dd" format
                  val formattedDate2023 = secondDateFormat.format(selectedDate.time)

                  //fromdate = formattedDate2023
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set the maximum date to the current date
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        // Show the DatePicker dialog
        datePickerDialog.show()
    }*/


   /* private fun getExpenceDateTime(): String {
        val zone = localizationData.timezone
        lateinit var timezone: String

        if (zone == "IST") {
            timezone = "Asia/Kolkata"
        } else if (zone == "CAT") {
            timezone = "Africa/Lusaka"
        } else {
            timezone = "Africa/Lusaka"
        }


//        val calendar = Calendar.getInstance()
//
//        val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
//        val currentDateTime = calendar.time
//        val formattedDateTime = dateFormat.format(currentDateTime)
//
//        return formattedDateTime


        val calendar = Calendar.getInstance()

        // Set the time zone to Zambia (Africa/Lusaka)
        val zambiaTimeZone = TimeZone.getTimeZone(timezone)
        calendar.timeZone = zambiaTimeZone

        val currentDateTime = calendar.time

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        dateFormat.timeZone = zambiaTimeZone

        val formattedDateTime = dateFormat.format(currentDateTime)

        return formattedDateTime

    }*/

    private fun getCurrentDate() {
        val zone = localizationData.timezone
        lateinit var timezone: String

        if (zone == "IST") {
            timezone = "Asia/Kolkata"
        } else if (zone == "CAT") {
            timezone = "Africa/Lusaka"
        } else {
            timezone = "Africa/Lusaka"
        }

//        val calendar = Calendar.getInstance()
//        val currentDateTime = calendar.time
//
//        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
//
//        val formattedDateTime = dateFormat.format(currentDateTime)
//
//        binding.calenderText.text = "Cash-up Time: $formattedDateTime"


        val calendar = Calendar.getInstance()

        // Set the time zone to Zambia (Africa/Lusaka)
        val zambiaTimeZone = TimeZone.getTimeZone(timezone)
        calendar.timeZone = zambiaTimeZone

        val currentDateTime = calendar.time

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        dateFormat.timeZone = zambiaTimeZone

        val formattedDateTime = dateFormat.format(currentDateTime)

        binding.dateInput.text = Editable.Factory.getInstance().newEditable(formattedDateTime)

    }


    private fun refreshPage() {

        pettycash_viewmodel.callPettycashDataApi(storeid, this@ExpenseRegisterActivity)

        invoiceFilePath = ""


        binding.apply {
            categoryInput.text = Editable.Factory.getInstance().newEditable("")
            othCategoryInput.text = Editable.Factory.getInstance().newEditable("")
            amountInput.text = Editable.Factory.getInstance().newEditable("")
            nameInput.text = Editable.Factory.getInstance().newEditable("")
            othNameInput.text = Editable.Factory.getInstance().newEditable("")
            invoiceImage.setImageResource(R.drawable.svg_scancam)


            receiptnoInput.text = Editable.Factory.getInstance().newEditable("")
            descripionInput.text = Editable.Factory.getInstance().newEditable("")
            sdcnoInput.text = Editable.Factory.getInstance().newEditable("")
            vatInput.text = Editable.Factory.getInstance().newEditable("")
            totalInput.text = Editable.Factory.getInstance().newEditable("")


            //nameInput.clearFocus()

            // Assuming rootView is the root view of your layout
            // rootView.clearFocus()

        }

        isImgSelected = false

    }

    private fun setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {
            // imageUri = it
            // binding.imageView.setImageURI(it)
            val imageUri = it

            if (imageUri != null) {
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                val cursor = this.contentResolver.query(imageUri, filePathColumn, null, null, null)
                cursor?.use {
                    it.moveToFirst()
                    val columnIndex = it.getColumnIndex(filePathColumn[0])
                    val mediaPath = it.getString(columnIndex)
                    // uploadFile(mediaPath)
                    openProfileConfirmDialog(mediaPath, imageUri)
                }
            } else {
                showMessage("No image selected")
            }

        }
    }

    private fun setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                // The picture was taken successfully
                // Now, you can use the captured image URI
                // For example, you can use the imageUri to display the captured image
                // binding.imageView.setImageURI(imageUri)
                //openProfileConfirmDialog(imageUri.toString(), imageUri)

                openProfileConfirmDialog(fileProviderUri.path.toString(), fileProviderUri)
            } else {
                showMessage("No image captured")
            }
        }
    }


    private fun showMessage(msg: String) {
        Toast.makeText(this@ExpenseRegisterActivity, msg, Toast.LENGTH_SHORT).show()
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

    private fun setupExpenseEntry() {
        binding.expenceEntry.setBackgroundResource(R.drawable.card_border_on)
        binding.expenceHistory.setBackgroundResource(R.drawable.card_border_off)

        binding.entryContainer.visibility = View.VISIBLE
        binding.historyContainer.visibility = View.GONE

        binding.tringle1.isVisible = true
        binding.tringle2.isVisible = false
    }

    private fun prepareExpenseHistoryRCV() {

        binding.historyRcv.apply {
            layoutManager = LinearLayoutManager(
                this@ExpenseRegisterActivity,
                RecyclerView.VERTICAL, false
            )

        }
    }

    private fun setupViewHistory() {
        binding.expenceHistory.setBackgroundResource(R.drawable.card_border_on)
        binding.expenceEntry.setBackgroundResource(R.drawable.card_border_off)

        binding.historyContainer.visibility = View.VISIBLE
        binding.entryContainer.visibility = View.GONE

        binding.tringle1.isVisible = false
        binding.tringle2.isVisible = true
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


    //IMAGE UPLOAD

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted
                return false
            }
            return true

        } else {

            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted
                return false
            }
            return true

        }
    }


    private fun setupRequesrPermissionLauncher() {
        // Initialize the ActivityResultLauncher
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

                val allPermissionsGranted = permission.all { it.value }
                /*   permission = map<String,Boolean>! , permissions is a map where each key represents a permission, and the associated value is true if the permission was granted, or false if it was denied.

                    .all is a standard library function in Kotlin that checks if a given condition holds true for all elements in a collection. In this case, it's checking if the value for all permissions is true.

                    { it.value } is a lambda expression used as a condition. It's checking the value (which is a Boolean) of each permission in the map.
                    */

                if (allPermissionsGranted) {
                    openImageUploadBottomsheet()
                } else {
                    showPermissionDeniedSnackbar()

                }

            }

    }

    private fun showPermissionDeniedSnackbar() {
        val snackbar = Snackbar.make(
            this, this.findViewById(R.id.expence_container),
            "Permissions were denied", Snackbar.LENGTH_SHORT
        ).setAction("GRANT") {
            val myAppSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            myAppSettings.data = Uri.parse("package:" + this.packageName)
            myAppSettings.addCategory(Intent.CATEGORY_DEFAULT)
            myAppSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(myAppSettings)
        }

        snackbar.show()

        /*  Snackbar snackbar = Snackbar.make(requireActivity().findViewById(R.id.fragment_container),"Permissions were denied", Snackbar.LENGTH_LONG)
          .setAction("GRANT", new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + "com.mangtum"));
                  myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                  myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  startActivity(myAppSettings);
              }
          });*/
    }

    private fun openImageUploadBottomsheet() {
        val d_binding = BottomsheetImageUploadBinding.inflate(layoutInflater)

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(d_binding.root)
        // dialog.setCancelable(false)
        //dialog.setCanceledOnTouchOutside(false)

        d_binding.closeBottomsheet.setOnClickListener {
            dialog.dismiss()
        }
        d_binding.cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        d_binding.galleryIcon.setOnClickListener {

            galleryLauncher.launch("image/*")

            /* val intent = Intent(Intent.ACTION_PICK)
             intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
             startActivityForResult(intent, 2)*/

            // finally dismiss dialog
            dialog.dismiss()

        }


        d_binding.cameraIcon.setOnClickListener {
            // Create a temporary file to store the captured image
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                // Handle the error
                showMessage("Error creating image file")
                return@setOnClickListener
            }

            // Continue only if the temporary file was successfully created
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.retailone.pos.FileProvider",
                    it
                )
                fileProviderUri = photoURI

                // Launch the camera using the cameraLauncher
                cameraLauncher.launch(photoURI)
                // Finally, dismiss the dialog
                dialog.dismiss()
            }
        }


        dialog.show()
    }


    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!storageDir.exists()) {
            storageDir.mkdirs() // Create the directory if it doesn't exist
        }

        val imageFile = File(storageDir, "$imageFileName.jpg")

        // Save a file path for use with ACTION_VIEW intents
        val currentPhotoPath = imageFile.absolutePath
        return imageFile
    }

    private fun openProfileConfirmDialog(imageFilePath: String, selectedImageUri: Uri) {

        // no dialog direct set image
        isImgSelected = true
        invoiceFilePath = imageFilePath
        binding.invoiceImage.setImageURI(selectedImageUri)

    }

    private fun uploadFile(imageFilePath: String) {


        //val file = File(imageFilePath)

        val file = compressImage(imageFilePath)

        /*
                val fileExtension = MimeTypeMap.getFileExtensionFromUrl(imageFilePath)
                Toast.makeText(this, fileExtension.toString(), Toast.LENGTH_SHORT).show()

                val allowedExtensions = arrayOf("jpg", "jpeg", "png")


                if (!isAllowedExtension(fileExtension, allowedExtensions)) {
                    Toast.makeText(this, "Only JPEG, JPG, and PNG files are allowed", Toast.LENGTH_SHORT).show()
                    return
                }
        */

        // validation

        /*  val maxFileSizeBytes = 300 * 1024 // 300 KB

          if (file.length() > maxFileSizeBytes) {
              Toast.makeText(this, "File size should be less than 300 KB", Toast.LENGTH_SHORT).show()
              return
          }*/


        val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        val filePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
        viewmodel.callInvoiceUploadApi(filePart, this)


        //val bearerToken = "Bearer $token"

    }


    private fun compressImage(imageFilePath: String): File {
        // Decode the image file to a bitmap
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        val bitmap = BitmapFactory.decodeFile(imageFilePath, options)

        // Calculate the image quality to achieve the desired file size
        val desiredSize = 1024 * 1024 // 1MB
        val outputStream = ByteArrayOutputStream()
        var quality = 100
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        while (outputStream.size() > desiredSize && quality > 0) {
            outputStream.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        // Save the compressed image to a new file
        val compressedFile = File.createTempFile("compressed_image", ".jpg")
        val fileOutputStream = FileOutputStream(compressedFile)
        fileOutputStream.write(outputStream.toByteArray())
        fileOutputStream.close()

        return compressedFile
    }
}