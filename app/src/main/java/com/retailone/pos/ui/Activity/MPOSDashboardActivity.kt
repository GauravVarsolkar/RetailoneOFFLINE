package com.retailone.pos.ui.Activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.retailone.pos.R
import com.retailone.pos.databinding.ActivityMposdashboardBinding
import com.retailone.pos.databinding.CustomerDetailsBottomsheetBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.CustomerLocalHelper
import com.retailone.pos.localstorage.SharedPreference.CustomerSessionHelper
import com.retailone.pos.localstorage.SharedPreference.InventoryStockHelper
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.localstorage.SharedPreference.SharedPrefHelper
import com.retailone.pos.localstorage.SharedPreference.TimeoutHelper
import com.retailone.pos.models.GetCustomerModel.getCustomerReq
import com.retailone.pos.repository.PosSaleRepository
import com.retailone.pos.ui.Activity.DashboardActivity.*
import com.retailone.pos.utils.CrashHandler
import com.retailone.pos.utils.NetworkUtils
import com.retailone.pos.viewmodels.DashboardViewodel.HomeDashboardViewmodel
import com.retailone.pos.viewmodels.MPOSLoginViewmodel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MPOSDashboardActivity : AppCompatActivity() {

    lateinit var binding: ActivityMposdashboardBinding
    lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    lateinit var drawer: DrawerLayout
    lateinit var loginSession: LoginSession
    lateinit var sharedPrefHelper: SharedPrefHelper
    lateinit var inventoryStockHelper: InventoryStockHelper
    lateinit var viewmodel: HomeDashboardViewmodel
    lateinit var loginViewmodel: MPOSLoginViewmodel
    lateinit var localizationHelper: LocalizationHelper
    lateinit var organisationDetailsHelper: OrganisationDetailsHelper

    private lateinit var posSaleRepository: PosSaleRepository
    private var isSyncing = false

    var storemanager_id = ""

    // Button state tracking
    private enum class SyncButtonState {
        IDLE,        // State 1: "SYNC" with arrow
        SYNCING,     // State 2: "SYNCING ITEMS" with rotating arrow
        SUCCESS,     // State 3: "SYNC Successful"
        IDLE_AGAIN   // State 4: Back to idle
    }

    private var currentState = SyncButtonState.IDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMposdashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        drawer = binding.drawer
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawer, R.string.nav_open, R.string.nav_close)
        drawer.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.svg_menu)

        val headerView = binding.navView.getHeaderView(0)
        val headerImageView = headerView.findViewById<ImageView>(R.id.header_imageView)

        loginSession = LoginSession.getInstance(this)
        sharedPrefHelper = SharedPrefHelper(this)
        inventoryStockHelper = InventoryStockHelper(this)
        localizationHelper = LocalizationHelper(this)
        organisationDetailsHelper = OrganisationDetailsHelper(this)

        viewmodel = ViewModelProvider(this)[HomeDashboardViewmodel::class.java]
        loginViewmodel = ViewModelProvider(this)[MPOSLoginViewmodel::class.java]

        posSaleRepository = PosSaleRepository(this)

        setupSyncButton()
        observePendingSalesCount()

        val crashHandler = CrashHandler(this)
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)

        lifecycleScope.launch {
            val storeid = loginSession.getStoreID().first().toInt()
            storemanager_id = loginSession.getStoreManagerID().first().toString()

            val timeouthelper = TimeoutHelper(this@MPOSDashboardActivity)

            if (!timeouthelper.isSessionValid()) {
                mposLogout()
            }

            viewmodel.callLocalizationApi(storeid, this@MPOSDashboardActivity)
            viewmodel.callOrganizationDetailsApi(storeid, this@MPOSDashboardActivity)
        }

        viewmodel.localization_liveData.observe(this) {
            localizationHelper.saveLocalizationData(it.data)
        }

        viewmodel.organization_liveData.observe(this) {
            organisationDetailsHelper.saveOrganisationData(it.data)
        }

        loginViewmodel.loadingLiveData.observe(this) {
            binding.progress.isVisible = it.isProgress
            if (it.isMessage) showMessage(it.message)
        }

        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.about_us -> Toast.makeText(this, "About us", Toast.LENGTH_SHORT).show()
                R.id.contact_us -> Toast.makeText(this, "Contact us", Toast.LENGTH_SHORT).show()
                R.id.log_data -> startActivity(Intent(this, CrashLogsActivity::class.java))
            }
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            true
        }

        // All your existing click listeners
        binding.poscard.setOnClickListener {
            lifecycleScope.launch {
                val cashupTime = loginSession.getCashupDateTime().first()
                if (isCashupOutdated(cashupTime)) {
                    showCashupPopup(cashupTime)
                } else {
                    customerBottomSheet()
                }
            }
        }

        binding.returncard.setOnClickListener {
            startActivity(Intent(this, ReturnSaleActivity::class.java))
        }

        binding.goodsRWcard.setOnClickListener {
            startActivity(Intent(this, proceedToDispatchActivity::class.java))
        }

        binding.stockcard.setOnClickListener {
            sharedPrefHelper.clearStockList()
            startActivity(Intent(this, StockRequisitionActivity::class.java))
        }

        binding.materialrcvCard.setOnClickListener {
            startActivity(Intent(this, MaterialRecivingItemsActivity::class.java))
        }

        binding.pdtInventoryCard.setOnClickListener {
            startActivity(Intent(this, ProductInventoryActivity::class.java))
        }

        binding.cashupcard.setOnClickListener {
            lifecycleScope.launch {
                val cashupTime = loginSession.getCashupDateTime().first()
                val intent = Intent(this@MPOSDashboardActivity, CashUpActivity::class.java)
                if (isCashupOutdated(cashupTime)) {
                    intent.putExtra("CASHUP_DATE_TIME", cashupTime)
                }
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }

        binding.salesPaymentCard.setOnClickListener {
            startActivity(Intent(this, SalesAndPaymentActivity::class.java))
        }

        binding.expensecard.setOnClickListener {
            lifecycleScope.launch {
                val cashupTime = loginSession.getCashupDateTime().first()
                val intent = Intent(this@MPOSDashboardActivity, ExpenseRegisterActivity::class.java)
                intent.putExtra("CASHUP_DATE_TIME", cashupTime)
                startActivity(intent)
            }
        }

        binding.profileCard.setOnClickListener {
            startActivity(Intent(this, ProfileAttendanceActivity::class.java))
        }

        binding.logout.setOnClickListener {
            showLogoutDialog()
        }

        val organisation_data = organisationDetailsHelper.getOrganisationData()

        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.fabicon)
            .fitCenter()
            .placeholder(R.drawable.mlogo)
            .error(R.drawable.mlogo)
            .into(binding.toolImage)

        Glide.with(this)
            .load(organisation_data.image_url + organisation_data.logo)
            .fitCenter()
            .placeholder(R.drawable.mlogo)
            .error(R.drawable.mlogo)
            .into(headerImageView)
    }

    // ✅ ENHANCED SYNC BUTTON SETUP
    private fun setupSyncButton() {
        binding.syncButtonContainer.setOnClickListener {
            if (!isSyncing && currentState == SyncButtonState.IDLE) {
                syncOfflineSales()
            }
        }
    }

    // ✅ OBSERVE PENDING SALES AND UPDATE BADGE
    private fun observePendingSalesCount() {
        lifecycleScope.launch {
            posSaleRepository.getPendingSalesCountFlow().collectLatest { count ->
                Log.d("SyncBadge", "Pending sales count: $count")

                if (count > 0) {
                    binding.tvSyncBadge.visibility = android.view.View.VISIBLE
                    binding.tvSyncBadge.text = if (count > 99) "99+" else count.toString()
                } else {
                    binding.tvSyncBadge.visibility = android.view.View.GONE
                }
            }
        }
    }

    // ✅ STATE 1 → STATE 2: IDLE TO SYNCING
    private fun transitionToSyncing() {
        currentState = SyncButtonState.SYNCING

        // Change text to "SYNCING ITEMS"
        binding.tvSyncText.text = "SYNCING ITEMS"

        // Expand button width
        val currentWidth = binding.syncButtonContainer.width
        val targetWidth = dpToPx(220) // Expanded width

        val widthAnimator = ValueAnimator.ofInt(currentWidth, targetWidth)
        widthAnimator.duration = 100
        widthAnimator.interpolator = AccelerateDecelerateInterpolator()
        widthAnimator.addUpdateListener { animator ->
            val params = binding.syncButtonContainer.layoutParams
            params.width = animator.animatedValue as Int
            binding.syncButtonContainer.layoutParams = params
        }
        widthAnimator.start()

        // Start rotating the icon
        val rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_sync)
        binding.ivSyncIcon.startAnimation(rotateAnim)
    }

    // ✅ STATE 2 → STATE 3: SYNCING TO SUCCESS
    private fun transitionToSuccess() {
        currentState = SyncButtonState.SUCCESS

        // Stop rotation
        binding.ivSyncIcon.clearAnimation()

        // Change background to green
        binding.syncButtonContainer.setBackgroundResource(R.drawable.bg_sync_button_green)

        // Change text to "SYNC Successful"
        binding.tvSyncText.text = "SYNC Successful"

        // Shrink button width back
        val currentWidth = binding.syncButtonContainer.width
        val targetWidth = dpToPx(220)

        val widthAnimator = ValueAnimator.ofInt(currentWidth, targetWidth)
        widthAnimator.duration = 400
        widthAnimator.interpolator = AccelerateDecelerateInterpolator()
        widthAnimator.addUpdateListener { animator ->
            val params = binding.syncButtonContainer.layoutParams
            params.width = animator.animatedValue as Int
            binding.syncButtonContainer.layoutParams = params
        }
        widthAnimator.start()

        // After 2 seconds, revert to idle
        lifecycleScope.launch {
            delay(2500)
            transitionToIdle()
        }
    }

    // ✅ STATE 3 → STATE 1: SUCCESS TO IDLE
    private fun transitionToIdle() {
        currentState = SyncButtonState.IDLE

        // Change background back to red
        binding.syncButtonContainer.setBackgroundResource(R.drawable.bg_sync_button_red)

        // Change text back to "SYNC"
        binding.tvSyncText.text = "SYNC"

        // Shrink button width to original
        val currentWidth = binding.syncButtonContainer.width
        val targetWidth = dpToPx(120) // Original width

        val widthAnimator = ValueAnimator.ofInt(currentWidth, targetWidth)
        widthAnimator.duration = 400
        widthAnimator.interpolator = AccelerateDecelerateInterpolator()
        widthAnimator.addUpdateListener { animator ->
            val params = binding.syncButtonContainer.layoutParams
            params.width = animator.animatedValue as Int
            binding.syncButtonContainer.layoutParams = params
        }
        widthAnimator.start()
    }

    // ✅ MAIN SYNC FUNCTION
    private fun syncOfflineSales() {
        if (!isInternetAvailable()) {
            showMessage("No internet connection. Please connect and try again.")
            return
        }
        // ✅ Check badge visibility (if badge is hidden, no pending sales)
        if (binding.tvSyncBadge.visibility == android.view.View.GONE) {
            showMessage("All sales are up to date")
            return
        }

        isSyncing = true
        transitionToSyncing()

        lifecycleScope.launch {
            try {
                delay(300)
                val success = posSaleRepository.syncOfflineSales()
                delay(1500)

                if (success) {
                    transitionToSuccess()
//                    showMessage("All sales synced successfully!")
                } else {
                    // Failed - revert to idle
                    binding.ivSyncIcon.clearAnimation()
                    transitionToIdle()
                    showMessage("Some sales failed to sync. Please try again.")
                }

            } catch (e: Exception) {
                binding.ivSyncIcon.clearAnimation()
                transitionToIdle()
                Log.e("SyncError", "Error syncing sales: ${e.message}")
                showMessage("Sync error: ${e.message}")
            } finally {
                isSyncing = false
            }
        }
    }

    // ✅ HELPER: Convert DP to PX
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // ... (Keep all your existing methods: isCashupOutdated, showCashupPopup,
    // customerBottomSheet, mposLogout, showLogoutDialog, etc.)

    private fun isCashupOutdated(cashupDateTime: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
            val cleanDate = cashupDateTime.trim().replace("\"", "")
            val cashupDate = formatter.parse(cleanDate) ?: return false

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -1)

            cashupDate.before(calendar.time)
        } catch (e: Exception) {
            false
        }
    }

    private fun showCashupPopup(cashupDateTime: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cashup, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            val intent = Intent(this, CashUpDetailsActivity::class.java)
            intent.putExtra("CASHUP_DATE_TIME", cashupDateTime)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            dialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun customerBottomSheet() {
        val d_binding = CustomerDetailsBottomsheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(d_binding.root)

        // ✅ ADD THIS: Hide skip button if offline
        if (!isInternetAvailable()) {
            d_binding.skipBtn.visibility = android.view.View.GONE
        } else {
            d_binding.skipBtn.visibility = android.view.View.VISIBLE
        }



        viewmodel.loadingLiveData.removeObservers(this)
        viewmodel.loadingLiveData.observe(this) {
            d_binding.progress.isVisible = it.isProgress
        }

        viewmodel.get_customer_liveData.removeObservers(this)
        viewmodel.get_customer_liveData.observe(this) {
            if (it.status == 1) {
                val customer = it.data
                val sessionHelper = CustomerSessionHelper(this)
                sessionHelper.saveLoggedInCustomer(
                    customerId = customer.id,
                    customerName = customer.customer_name,
                    mobile = customer.mobile_no
                )

                val intent = Intent(this, PointOfSaleActivity::class.java)
                intent.putExtra("c_id", customer.id)
                intent.putExtra("c_mobile", customer.mobile_no ?: "")
                intent.putExtra("c_name", customer.customer_name ?: "")
                intent.putExtra("c_tpin", customer.tin_tpin_no ?: "")
                startActivity(intent)

                if (dialog.isShowing) dialog.dismiss()
            } else {
                showMessage(it.message)
            }
        }

        d_binding.saveBtn.setOnClickListener {
            val inputValue = d_binding.mobileInput.text.toString().trim()

            if (inputValue.isEmpty() || inputValue.length < 9) {
                showMessage("Enter valid Mobile No or TIN")
                return@setOnClickListener
            }

            if (!isInternetAvailable()) {
                val localHelper = CustomerLocalHelper(this)
                val matchedCustomer = if (d_binding.toggle.checkedRadioButtonId == R.id.mobile_btn) {
                    localHelper.getCustomers().find { it.mobile_no == inputValue }
                } else {
                    localHelper.getCustomers().find { it.tin_tpin_no == inputValue }
                }

                if (matchedCustomer != null) {
                    val sessionHelper = CustomerSessionHelper(this)
                    sessionHelper.saveLoggedInCustomer(
                        customerId = matchedCustomer.id,
                        customerName = matchedCustomer.customer_name,
                        mobile = matchedCustomer.mobile_no
                    )

                    val intent = Intent(this, PointOfSaleActivity::class.java)
                    intent.putExtra("c_id", matchedCustomer.id)
                    intent.putExtra("c_mobile", matchedCustomer.mobile_no ?: "")
                    intent.putExtra("c_name", matchedCustomer.customer_name ?: "")
                    intent.putExtra("c_tpin", matchedCustomer.tin_tpin_no ?: "")
                    startActivity(intent)
                    dialog.dismiss()
                } else {
                    showMessage("Customer not found in offline records")
                }
                return@setOnClickListener
            }

            if (d_binding.toggle.checkedRadioButtonId == R.id.mobile_btn) {
                viewmodel.callGetCustomerDetailsApi(
                    getCustomerReq(mobile_no = inputValue, tin_tpin_no = ""),
                    this
                )
            } else {
                viewmodel.callGetCustomerDetailsApi(
                    getCustomerReq(mobile_no = "", tin_tpin_no = inputValue),
                    this
                )
            }
        }

        d_binding.skipBtn.setOnClickListener {
            if (!NetworkUtils.isInternetAvailable(this)) {
                Toast.makeText(
                    this,
                    "Cannot continue as new customer in offline mode.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (dialog.isShowing) dialog.dismiss()
            val intent = Intent(this, PointOfSaleActivity::class.java)
            intent.putExtra("c_id", 0)
            intent.putExtra("c_mobile", "")
            intent.putExtra("c_name", "")
            intent.putExtra("c_tpin", "")
            startActivity(intent)
        }

        dialog.show()
    }

    private fun mposLogout() {
        val device_id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        if (device_id.isEmpty()) {
            showMessage("Couldn't fetch device id")
        } else if (storemanager_id.isEmpty()) {
            showMessage("Couldn't fetch store manager info")
        } else {
            loginViewmodel.callLogoutApi(this, storemanager_id, device_id)
        }

        loginViewmodel.logoutLiveData.observe(this) {
            CoroutineScope(Dispatchers.IO).launch {
                loginSession.clearLoginSession()
                val intent = Intent(this@MPOSDashboardActivity, MPOSLoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showLogoutDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.logout_dialog_layout)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cancel = dialog.findViewById<MaterialButton>(R.id.prefer_cancel)
        val confirm = dialog.findViewById<MaterialButton>(R.id.prefer_confirm)
        val logoutMsg = dialog.findViewById<TextView>(R.id.logout_msg)
        val logoutImg = dialog.findViewById<ImageView>(R.id.dialog_logo)

        logoutMsg.text = "Are you sure you want to Logout ?"
        logoutImg.setImageResource(R.drawable.svg_off)

        confirm.setOnClickListener {
            dialog.dismiss()
            mposLogout()
        }

        cancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
