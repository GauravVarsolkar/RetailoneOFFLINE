package com.retailone.pos.ui.Activity.DashboardActivity

import NumberFormatter
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.gson.Gson
import com.retailone.pos.R
import com.retailone.pos.databinding.ActivityPointofSaleDetailsBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.AddNewCustomerModel.AddNewCustReq
import com.retailone.pos.models.GetCustomerModel.getCustomerReq
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.PointofsaleModel.PosAddToCartModel.PosAddToCartRes
import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSaleReq
import com.retailone.pos.models.PointofsaleModel.PosSaleModel.PosSalesItem
import com.retailone.pos.models.PosSalesDetailsModel.PosSalesDetails
import com.retailone.pos.models.PosSalesDetailsModel.ReceiptType
import com.retailone.pos.ui.Activity.MPOSDashboardActivity
import com.retailone.pos.utils.FunUtils
import com.retailone.pos.utils.PrinterUtil
import com.retailone.pos.viewmodels.DashboardViewodel.PointofSaleViewmodel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object InvoiceSubmissionTracker {
    private val submitted = mutableSetOf<String>()
    private fun key(storeId: String, invoice: String): String =
        "${storeId.trim()}::${invoice.trim().uppercase()}"
    @Synchronized fun alreadySubmitted(storeId: String, invoice: String) =
        submitted.contains(key(storeId, invoice))
    @Synchronized fun markSubmitted(storeId: String, invoice: String) {
        submitted.add(key(storeId, invoice))
    }
    @Synchronized fun clear() = submitted.clear()
}

class PointofSaleDetailsActivity : AppCompatActivity() {
    lateinit var binding: ActivityPointofSaleDetailsBinding

    private val pmtmethod_list = arrayOf("Cash", "M-Money")

    lateinit var posAddToCartRes: PosAddToCartRes
    lateinit var pos_viewmodel: PointofSaleViewmodel

    var pos_saledata: PosSaleReq? = null

    var payment_type = ""
    lateinit var localizationData: LocalizationData
    var storeid = ""
    var store_manager_id = ""

    var cnamex = ""
    var cmobx = ""
    var ctpinx = ""
    var cidx = 0
    var total_amountx = 0.0
    var spotDiscountPercent = 0.0

    val SALE_LIMIT = 100000.0

    private var printerUtil: PrinterUtil? = null

    private var receiptTypeList = mutableListOf<ReceiptType>()
    private var selectedReceiptType: ReceiptType? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPointofSaleDetailsBinding.inflate(layoutInflater)
        pos_viewmodel = ViewModelProvider(this)[PointofSaleViewmodel::class.java]
        localizationData = LocalizationHelper(this).getLocalizationData()
        setContentView(binding.root)

        enableBackButton()
        printerUtil = PrinterUtil(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            posAddToCartRes = intent.getParcelableExtra("saleitem", PosAddToCartRes::class.java)!!
        } else {
            posAddToCartRes = intent.getParcelableExtra<PosAddToCartRes>("saleitem")!!
        }

        cmobx = intent.getStringExtra("c_mobile").toString()
        cnamex = intent.getStringExtra("c_name").toString()
        cidx = intent.getIntExtra("c_id", 0)
        ctpinx = intent.getStringExtra("c_tpin").toString()
        total_amountx = intent.getDoubleExtra("total_amount", 0.0)
        spotDiscountPercent = intent.getDoubleExtra("spot_discount_percent", 0.0)

        if (cidx != 0) {
            binding.apply {
                nameInput.keyListener = null
                mobileInput.keyListener = null
                tinInput.keyListener = null
                existingcust.isChecked = true
            }
        } else {
            binding.newcust.isChecked = true
        }

        if (isEligibleNewCustomer(total_amountx, cidx)) {
            binding.nameLayout.hint = "Customer Name *"
            binding.mobileLayout.hint = "Customer Mobile no*"
            binding.tinLayout.hint = "Customer TIN no *"
        } else {
            binding.nameLayout.hint = "Customer Name (Optional)"
            binding.mobileLayout.hint = "Customer Mobile no (Optional)"
            binding.tinLayout.hint = "Customer TIN no (Optional)"
        }

        if (cidx != 0) {
            binding.nameLayout.hint = "Customer Name"
            binding.mobileLayout.hint = "Customer Mobile no"
            binding.tinLayout.hint = "Customer TIN no"
        }

        if (cidx == 0 && total_amountx < SALE_LIMIT) {
            binding.toggle.isClickable = true
            binding.existingcust.isClickable = true
            binding.newcust.isClickable = true
        }

        binding.toggle.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.newcust) {
                binding.nameLayout.hint = "Customer Name (Optional)"
                binding.mobileLayout.hint = "Customer Mobile no (Optional)"
                binding.tinLayout.hint = "Customer TIN/TPIN no (Optional)"
            } else {
                binding.nameLayout.hint = "Customer Name *"
                binding.mobileLayout.hint = "Customer Mobile no*"
                binding.tinLayout.hint = "Customer TIN/TPIN no (Optional)"
            }
        }

        binding.nameInput.text = Editable.Factory.getInstance().newEditable(cnamex)
        binding.mobileInput.text = Editable.Factory.getInstance().newEditable(cmobx)
        binding.tinInput.text = Editable.Factory.getInstance().newEditable(ctpinx)

        lifecycleScope.launch {
            storeid = LoginSession.getInstance(this@PointofSaleDetailsActivity).getStoreID().first()
                .toString()
            store_manager_id =
                LoginSession.getInstance(this@PointofSaleDetailsActivity).getStoreManagerID()
                    .first().toString()
        }

        binding.apply {
            subtotal.text = NumberFormatter().formatPrice(
                posAddToCartRes.sub_total.toString(), localizationData
            )
            val spotAmount = posAddToCartRes.spot_discount_amount.toDoubleOrNull() ?: 0.0
            if (spotAmount > 0.0) {
                spotDiscountRow.isVisible = true
                spotDiscountPercentField.text = "(-) Spot discount ${posAddToCartRes.spot_discount_percentage}%"
                spotDiscountAmountValue.text = NumberFormatter().formatPrice(
                    posAddToCartRes.spot_discount_amount, localizationData
                )
            } else {
                spotDiscountRow.isVisible = false
            }
            taxfield.text = "(+) Tax ${posAddToCartRes.tax}"
            taxAmount.text = NumberFormatter().formatPrice(
                posAddToCartRes.tax_amount.toString(), localizationData
            )
            discountvalue.text = NumberFormatter().formatPrice(
                posAddToCartRes.discount_amount.toString(), localizationData
            )
            alltotalAmount.text = NumberFormatter().formatPrice(
                java.math.BigDecimal(posAddToCartRes.grand_total.toString()).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString(), localizationData
            )
        }

        pos_viewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        pos_viewmodel.posSaleLivedata.observe(this) { pos_sale_data ->
            if (pos_sale_data.status == 1) {
                try {
                    val storeIdTrim = (storeid ?: "").trim()
                    val invoiceTrim = pos_saledata?.invoice_id?.trim().orEmpty()
                    if (storeIdTrim.isNotEmpty() && invoiceTrim.isNotEmpty()) {
                        InvoiceSubmissionTracker.markSubmitted(storeIdTrim, invoiceTrim)
                    }

                    val _customer_name = binding.nameInput.text.toString()
                    val _mobile_no = binding.mobileInput.text.toString()
                    val _tin_tpin_no = binding.tinInput.text.toString()

                    if (isEligibleNewCustomer(total_amountx, cidx)) {
                        pos_viewmodel.callAddNewCustApi(
                            AddNewCustReq(
                                customer_name = _customer_name,
                                mobile_no = _mobile_no,
                                tin_tpin_no = _tin_tpin_no
                            ), this
                        )
                    } else if (total_amountx < SALE_LIMIT && cidx == 0 &&
                        _customer_name.trim().isNotEmpty() && _tin_tpin_no.trim().isNotEmpty()
                    ) {
                        pos_viewmodel.callAddNewCustApi(
                            AddNewCustReq(
                                customer_name = _customer_name,
                                mobile_no = _mobile_no,
                                tin_tpin_no = _tin_tpin_no
                            ), this
                        )
                    }
                } catch (e: Exception) {
                    Log.e("POS", "post-sale follow-ups", e)
                } finally {
                    showSucessDialog(pos_sale_data.message, pos_sale_data)
                }
            } else if (pos_sale_data.status == 2) {
                Toast.makeText(this, pos_sale_data.message, Toast.LENGTH_SHORT).show()
            }
        }
        pos_viewmodel.receiptTypeLiveData.observe(this) { response ->
            if (response.status == 1) {
                receiptTypeList.clear()
                receiptTypeList.addAll(response.data ?: emptyList())
                populateReceiptTypeDropdown()
            } else {
                showMessage("Failed to load receipt types")
            }
        }

        pos_viewmodel.addNewCustLivedata.observe(this) {
            // unchanged
        }

        for (item in pmtmethod_list) {
            val chip: Chip = getPaymentMethodChip(item)
            binding.paymentmethodChipgroup.addView(chip)
        }

        binding.linearcomplete.setOnClickListener {
            binding.linearcomplete.isEnabled = false
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                binding.linearcomplete.isEnabled = true
            }, 2000)

            validateDetails(
                binding.mobileInput.text.toString(),
                binding.nameInput.text.toString(),
                binding.tinInput.text.toString(),
                binding.invoiceNum.text.toString(),
                payment_type,
                posAddToCartRes
            )
        }

        binding.registerSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val intent = Intent(this@PointofSaleDetailsActivity, NewCustomerRegisterActivity::class.java)
                launchSomeActivity.launch(intent)
            }
        }

        setToolbarImage()
        fetchReceiptTypes()
        setupReceiptTypeDropdown()
    }

    private fun isEligibleNewCustomer(totalAmountx: Double, cidx: Int): Boolean {
        return totalAmountx >= SALE_LIMIT && cidx == 0
    }

    private val launchSomeActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val name = data?.getStringExtra("name")
                showMessage(name ?: "name")
            } else {
                binding.registerSwitch.isChecked = false
            }
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

    private fun showSucessDialog(msg: String, pos_sale_data: PosSalesDetails) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.pos_sucess_dialog)
        dialog.setCancelable(false)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
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
            val intent = Intent(this@PointofSaleDetailsActivity, MPOSDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        print_receipt.setOnClickListener {
            Log.d("xxx", Gson().toJson(pos_sale_data))
            printerUtil?.printReceiptData(pos_sale_data)
            dialog.dismiss()
            val intent = Intent(this@PointofSaleDetailsActivity, MPOSDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
        dialog.show()
    }

    // ✅ UPDATED: Handle both String ("@18%") and pass through for numeric tax
    private fun getTaxString(taxValue: Any): String {
        return when (taxValue) {
            is String -> {
                // Parse string like "@18%" or "18"
                val cleaned = taxValue.trim()
                    .replace(Regex("[^0-9.,]"), "")
                    .replace(',', '.')
                val normalized = buildString {
                    var dotSeen = false
                    for (ch in cleaned) {
                        if (ch.isDigit()) append(ch)
                        else if (ch == '.' && !dotSeen) { append('.'); dotSeen = true }
                    }
                }
                if (normalized.isEmpty() || normalized == ".") return "0"
                try {
                    normalized.toBigDecimal().stripTrailingZeros().toPlainString()
                } catch (_: Exception) {
                    "0"
                }
            }
            is Int -> taxValue.toString()
            is Double -> taxValue.toInt().toString()
            else -> "0"
        }
    }


    private fun validateDetails(
        mobileInput: String,
        nameInput: String,
        tinInput: String,
        invoiceNum: String,
        paymentType: String,
        data: PosAddToCartRes,
    ) {
        // === STRICT: Name & TIN are mandatory in all cases ===
    /*    if (nameInput.trim().isEmpty()) {
            showMessage("Please Enter Customer Name"); return
        }
        if (tinInput.trim().isEmpty()) {
            showMessage("Please Enter Customer TIN Number"); return
        }

        // Your existing validations
        if (mobileInput.trim().isNotEmpty() && mobileInput.trim().length != 9) {
            showMessage("Please Enter Valid Mobile Number"); return
        } else if (tinInput.trim().isNotEmpty() && tinInput.trim().length < 9) {
            showMessage("Please Enter Valid TIN Number"); return
        } else if (mobileInput.trim().isEmpty() && binding.toggle.checkedRadioButtonId == R.id.existingcust) {
            showMessage("Please Enter Customer Mobile Number"); return
        }

        if (paymentType.isBlank()) { showMessage("Please Enter Payment Type"); return }
        if (storeid == "") { showMessage("Could't fetch store info,Please try again.."); return }
        if (store_manager_id == "") { showMessage("Could't fetch store manager info,Please try again.."); return }
*/
        //pallab code
        /////////////////////////////////
        val invoiceTrim = invoiceNum.trim()
        val storeIdTrim = (storeid ?: "").trim()
        val item_list = buildFinalSalesItems(data)
        if (mobileInput.trim().isNotEmpty() && mobileInput.trim().length != 9) {
            showMessage("Please Enter Valid Mobile Number")
        } else if (tinInput.trim().isNotEmpty() && tinInput.trim().length < 9) {

            showMessage("Please Enter Valid TIN Number")
        } else if (tinInput.trim().isEmpty() && isEligibleNewCustomer(total_amountx, cidx)) {
            showMessage("Please Enter Customer TIN Number")
        }

        else if (mobileInput.trim().isEmpty() &&(binding.toggle.checkedRadioButtonId == R.id.existingcust ||isEligibleNewCustomer(total_amountx, cidx)) ) {
            showMessage("Please Enter Customer Mobile Number")
        }

        else if (nameInput.trim().isEmpty() && isEligibleNewCustomer(total_amountx, cidx)) {
            showMessage("Please Enter Customer Name")

            // if newcustomer sale lessthen store can change customer type

        } else if (tinInput.trim()
                .isEmpty() && binding.toggle.checkedRadioButtonId == R.id.existingcust && cidx == 0
        ) {
            showMessage("Please Enter Customer TIN Number")
        }

        /* else if (mobileInput.trim()
                 .isEmpty() && binding.toggle.checkedRadioButtonId == R.id.existingcust && cidx == 0
         ) {
             showMessage("Please Enter Customer Mobile Number")
         } */

        else if (nameInput.trim()
                .isEmpty() && binding.toggle.checkedRadioButtonId == R.id.existingcust && cidx == 0
        ) {
            showMessage("Please Enter Customer Name")
        }


        /*else if (nameInput.isBlank() || nameInput.isEmpty()) {
            showMessage("Please Enter Driver Name")
        }*/ else if (paymentType.isBlank() || paymentType.isEmpty()) {
            showMessage("Please Enter Payment Type")
        } else if (storeid == "") {
            showMessage("Could't fetch store info,Please try again..")
        }
        else if (selectedReceiptType == null) {
            showMessage("Please Select Receipt Type")
        }
        else if (store_manager_id == "") {
            showMessage("Could't fetch store manager info,Please try again..")
        }
        //////////////////////////

        /* else  if (invoiceTrim.isEmpty()) {
            showMessage("Please Enter Valid Invoice Number"); return
        } */

        else if (invoiceTrim.isNotEmpty() && InvoiceSubmissionTracker.alreadySubmitted(storeIdTrim, invoiceTrim)) {
            showMessage("This invoice number is already used for this store. Please use a new invoice number.")
            return
        }

        // === Build de-duplicated list without touching batch internals ===

       else  if (item_list.isEmpty()) {
            showMessage("No valid items to sell. Please add quantities.")
            return
        }




        else{
            val new_grand_total = removeThousandSeparator(data.grand_total)
            var amt_tndr = binding.amtEdit.text.toString()
            if (amt_tndr.isEmpty() || amt_tndr.isBlank()) amt_tndr = "0"
            pos_saledata = PosSaleReq(
                customer_name = nameInput,
                customer_mob_no = mobileInput,
                customer_id = cidx,
                payment_type = paymentType,
                sub_total = data.sub_total.toString(),
                tax = getTaxString(data.tax),
                tax_amount = data.tax_amount,
                discount_amount = data.discount_amount.toString(),
                subtotal_after_discount = data.sub_total_after_discount.toString(),
                grand_total = new_grand_total,
                store_id = storeid.toString(),
                sales_items = item_list,
                store_manager_id = store_manager_id,
                amount_tendered = amt_tndr,
                sale_date_time = getSaleDateTime(),
                tin_tpin_no = tinInput,
                invoice_id  = "", // Backend will generate this
                prc_no = if (invoiceTrim.isEmpty()) null else invoiceTrim,
                trxn_code = selectedReceiptType?.code ?: "",
                tax_details = null,
                tax_summery = null,
                discount_rate = 0,
                total_after_discount = 0,
                spot_discount_percentage = spotDiscountPercent,
                spot_discount_amount = data.spot_discount_amount
            )

            Log.d("nm", Gson().toJson(pos_saledata))

            val isExistingCustomer = binding.toggle.checkedRadioButtonId == R.id.existingcust
            if (isExistingCustomer) {
                if (!com.retailone.pos.utils.NetworkUtils.isInternetAvailable(this)) {
                    val mobile = binding.mobileInput.text.toString().trim()
                    val tpin = binding.tinInput.text.toString().trim()
                    val localHelper = com.retailone.pos.localstorage.SharedPreference.CustomerLocalHelper(this)
                    val customers = localHelper.getCustomers()
                    val matchedCustomer = customers.find { 
                        (mobile.isNotEmpty() && it.mobile_no == mobile) || 
                        (tpin.isNotEmpty() && it.tin_tpin_no == tpin) 
                    }
                    if (matchedCustomer == null) {
                        showMessage("Customer does not exist in Offline Records.")
                        return
                    } else {
                        pos_saledata = pos_saledata?.copy(customer_id = matchedCustomer.id)
                        pos_viewmodel.callposSaleApiPatched(pos_saledata!!, this)
                    }
                } else {
                    // pos_viewmodel.callposSaleApi(pos_saledata!!, this)
                    pos_viewmodel.callposSaleApiPatched(pos_saledata!!, this)
                }
            } else {
                if (!com.retailone.pos.utils.NetworkUtils.isInternetAvailable(this)) {
                    // NEW: If offline, we can't check if the customer already exists 
                    // via API, so just proceed with saving the offline sale directly.
                    pos_viewmodel.callposSaleApiPatched(pos_saledata!!, this)
                } else {
                    // avoid stacking observers on multiple clicks
                    pos_viewmodel.get_customer_liveData.removeObservers(this)
                    pos_viewmodel.callGetCustomerDetailsApi(
                        getCustomerReq(mobile_no = mobileInput, tin_tpin_no = tinInput),
                        this
                    )
                    pos_viewmodel.get_customer_liveData.observe(this) {
                        if (it.status == 1) {
                            showMessage("Customer Already Exists!")
                        } else {
                            // pos_viewmodel.callposSaleApi(pos_saledata!!, this)
                            pos_viewmodel.callposSaleApiPatched(pos_saledata!!, this)
                        }
                    }
                }
            }
        }



    }

    /**
     * De-duplicate by (product_id, distribution_pack_id). Before grouping:
     *  - Drop lines where both total == 0 and sum(batch.quantity) == 0.
     * When merging:
     *  - Filter OUT zero-quantity batches so the API never sees them.
     *  - If after filtering, both merged batch quantity and computed total are 0, skip the item.
     */
    private fun buildFinalSalesItems(data: PosAddToCartRes): List<PosSalesItem> {
        val src = data.data ?: return emptyList()

        // 1) Drop completely-zero lines early
        val nonZero = src.filter { line ->
            val totalVal = FunUtils.stringToDouble(line.total?.toString() ?: "0")
            val qtySum = (line.batch ?: emptyList()).sumOf { b ->
                FunUtils.stringToDouble(b.quantity?.toString() ?: "0")
            }
            (totalVal > 0.0) || (qtySum > 0.0)
        }
        if (nonZero.isEmpty()) return emptyList()

        // 2) Group and merge
        val grouped = nonZero.groupBy { Pair(it.product_id, it.distribution_pack_id) }
        val result = mutableListOf<PosSalesItem>()

        grouped.forEach { (_, items) ->
            val first = items.first()

            // Keep only batches with qty > 0 so we never send zero lines
            val mergedBatches = items
                .flatMap { it.batch ?: emptyList() }
                .filter { b -> FunUtils.stringToDouble(b.quantity?.toString() ?: "0") > 0.0 }

            val mergedQty = mergedBatches.sumOf { b ->
                FunUtils.stringToDouble(b.quantity?.toString() ?: "0")
            }

            val computedTotal = items.sumOf { FunUtils.stringToDouble(it.total?.toString() ?: "0") }

            // If after merge everything is still zero, skip
            if (mergedQty <= 0.0 && computedTotal <= 0.0) return@forEach

            // ✅ FIX: Sum per-item tax amounts and discounts across merged items for this product
            val mergedTaxRate = first.tax.toDouble()
            val mergedTaxAmount = items.sumOf { (it.tax_amount?.toDoubleOrNull() ?: 0.0) }
            val mergedDiscount = items.sumOf { it.discount }

            result.add(
                PosSalesItem(
                    product_id = first.product_id.toString(),
                    distribution_pack_id = first.distribution_pack_id.toString(),
                    whole_sale_price = first.price_without_discount.toString(),
                    total_amount = FunUtils.DtoString(computedTotal),
                    batch = mergedBatches,
                    product_name = first.product_name,
                    distribution_pack_name = first.distribution_pack.product_description,
                    uom = first.distribution_pack.uom,
                    tax = mergedTaxRate,    // ✅ Pass per-item tax rate for offline sales details
                    tax_amount = mergedTaxAmount,  // ✅ Pass per-item tax amount for offline sales details
                    discount = mergedDiscount // ✅ Pass per-item discount for offline sales details
                )
            )
        }
        return result
    }

    private fun getSaleDateTime(): String {
        val zone = localizationData.timezone
        val timezone = when (zone) {
            "IST" -> "Asia/Kolkata"
            "CAT" -> "Africa/Lusaka"
            else -> "Africa/Lusaka"
        }
        val calendar = Calendar.getInstance()
        val zambiaTimeZone = TimeZone.getTimeZone(timezone)
        calendar.timeZone = zambiaTimeZone
        val currentDateTime = calendar.time
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        dateFormat.timeZone = zambiaTimeZone
        return dateFormat.format(currentDateTime)
    }

    private fun getPaymentMethodChip(item: String): Chip {
        val chip = Chip(this)
        chip.setChipDrawable(ChipDrawable.createFromResource(this, R.xml.chipchoice_xml))
        chip.setChipDrawable(
            ChipDrawable.createFromAttributes(this, null, 0, R.style.custom_choice_chips)
        )
        chip.typeface = Typeface.create(
            ResourcesCompat.getFont(this, R.font.avenirnextltpro_medium),
            Typeface.BOLD
        )
        chip.text = item
        chip.setTextColor(this.getColorStateList(R.color.color_active_inctive_text))
        chip.setOnClickListener { payment_type = item }
        if (item == "Cash") {
            chip.isChecked = true
            payment_type = item
        }
        return chip
    }

    private fun enableBackButton() {
        setSupportActionBar(binding.toolbar)
        val actionbar = supportActionBar
        actionbar!!.title = "New Activity"
        actionbar.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.svg_back_arrow_white)
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@PointofSaleDetailsActivity, msg, Toast.LENGTH_SHORT).show()
    }

    fun removeThousandSeparator(input: String): String {
        return input.replace(Regex("[^\\d.]"), "")
    }
    private fun setupReceiptTypeDropdown() {
        binding.receiptTypeDropdown.setOnItemClickListener { parent, view, position, id ->
            selectedReceiptType = receiptTypeList[position]
            Log.d("ReceiptType", "Selected: ${selectedReceiptType?.name} (ID: ${selectedReceiptType?.id})")
        }
    }

    private fun fetchReceiptTypes() {
        pos_viewmodel.callGetReceiptTypesApi(this)
    }

    private fun populateReceiptTypeDropdown() {
        val receiptTypeNames = receiptTypeList.map { it.name }

        // Use a non-filtering adapter to ensure all items are always visible in the dropdown
        val adapter = object : android.widget.ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            receiptTypeNames
        ) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = receiptTypeNames
                        results.count = receiptTypeNames.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        binding.receiptTypeDropdown.setAdapter(adapter)

        // Set default to "Normal" (code "N"), or fall back to the first item if "Normal" doesn't exist
        val defaultType = receiptTypeList.find { it.code == "N" } ?: receiptTypeList.firstOrNull()
        defaultType?.let {
            binding.receiptTypeDropdown.setText(it.name, false)
            selectedReceiptType = it
        }
    }




    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
