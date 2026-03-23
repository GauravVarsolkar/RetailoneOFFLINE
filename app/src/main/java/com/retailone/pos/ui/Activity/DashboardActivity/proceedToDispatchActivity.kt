package com.retailone.pos.ui.Activity.DashboardActivity



import ReturnedProduct
import StockReturn
import StockReturnAdapter
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.retailone.pos.R
import com.retailone.pos.adapter.PastRequDetailsAdapter
import com.retailone.pos.adapter.PastRequisitionAdapter
import com.retailone.pos.adapter.ProductReorderAlertAdapter
import com.retailone.pos.adapter.ProductReturnAlertAdapter
import com.retailone.pos.databinding.ActivityPrceedtoDispatchBinding
import com.retailone.pos.databinding.ActivityStockRequisitionBinding
import com.retailone.pos.databinding.BottomsheetPastRequisitionLayoutBinding
import com.retailone.pos.databinding.BottomsheetReturnproductLayoutBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.GoodsToWarehouseModel.ReturnStocks.ProductModel
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsList
import com.retailone.pos.utils.DateTimeFormatting
import com.retailone.pos.viewmodels.DashboardViewodel.MaterialReceivingViewmodel
import com.retailone.pos.viewmodels.DashboardViewodel.StockRequisitionViewmodel
import com.retailone.pos.viewmodels.DashboardViewodel.StockReturnViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class proceedToDispatchActivity : AppCompatActivity() {
    lateinit var  binding:ActivityPrceedtoDispatchBinding

    private lateinit var viewModel: StockReturnViewModel
    private  var storeid = ""

    //GoodsReturnToWarehouseActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPrceedtoDispatchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableBackButton()

        viewModel = ViewModelProvider(this)[StockReturnViewModel::class.java]
        binding.dispatchRcv.layoutManager = LinearLayoutManager(this)

        viewModel.fetchStockReturns(this)


      /*  viewModel.stockReturns.observe(this) {
            val allItems = it.data // DO NOT FILTER
            val adapter = StockReturnAdapter(allItems) { item ->
                Toast.makeText(this, "Dispatch clicked for ID: ${item.id}", Toast.LENGTH_SHORT).show()
            }
            binding.dispatchRcv.layoutManager = LinearLayoutManager(this)
            binding.dispatchRcv.adapter = adapter
        }*/


        viewModel.loading.observe(this) {
            binding.progressBar.isVisible = it
        }
        viewModel.stockReturns.observe(this) {
            val approvedList = it.data
            val adapter = StockReturnAdapter(
                approvedList,
                onDispatchClicked = { selectedItem ->
                    val intent = Intent(this, ConfirmReturnReceiptActivity::class.java)
                    intent.putExtra("return_id", selectedItem.id)
                    intent.putExtra("status", selectedItem.status)
                    intent.putExtra("date", selectedItem.requested_date)
                    intent.putExtra("seal_no", selectedItem.products[0].seal_no)
                    intent.putParcelableArrayListExtra("product_list", ArrayList(selectedItem.products))
                    startActivity(intent)
                },
                /*onItemClicked = { selectedItem ->
                    showProductListBottomSheet(selectedItem.products)
                }*/
                onItemClicked = { selectedItem ->
                    showProductListBottomSheet(selectedItem) // ✅ pass whole selectedItem
                }

            )
            binding.dispatchRcv.adapter = adapter

           /* val adapter = StockReturnAdapter(approvedList) { selectedItem ->
                val intent = Intent(this, ConfirmReturnReceiptActivity::class.java)
                intent.putExtra("return_id", selectedItem.id)
                intent.putExtra("status",selectedItem.status)
                intent.putExtra("date",selectedItem.requested_date)
                intent.putParcelableArrayListExtra("product_list", ArrayList(selectedItem.products))
                startActivity(intent)
            }*/
           // binding.dispatchRcv.adapter = adapter
        }

        binding.relativeLayout.setOnClickListener {
            val intent = Intent(this@proceedToDispatchActivity,GoodsReturnToWarehouseActivity::class.java)
            //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
        }

    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    override fun onResume() {
        super.onResume()
        viewModel.fetchStockReturns(this) // Refresh the list on returning back
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










/* private fun showProductListBottomSheet(products: List<ReturnedProduct>) {
     val bottomSheetDialog = BottomSheetDialog(this)
     val bottomSheetBinding = BottomsheetPastRequisitionLayoutBinding.inflate(layoutInflater)
     bottomSheetDialog.setContentView(bottomSheetBinding.root)

     val adapter = ProductReturnAlertAdapter(products)
     bottomSheetBinding.detailsRcv.layoutManager = LinearLayoutManager(this)
     bottomSheetBinding.detailsRcv.adapter = adapter

     bottomSheetBinding.closeBottomsheet.setOnClickListener {
         bottomSheetDialog.dismiss()
     }

     bottomSheetDialog.show()
 }*/


    private fun getStatusText(status: Int): String {
        return when (status) {
            2 -> "Return Approved"
            3 -> "Rejected"
            4 -> "Dispatched"
            5 -> "Received"
            else -> "Pending"
        }
    }

    private fun showProductListBottomSheet(selectedItem: StockReturn) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetBinding =  BottomsheetReturnproductLayoutBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        bottomSheetBinding.detailsRcv.isNestedScrollingEnabled = true
        // Set item details
        bottomSheetBinding.tvReturnId.text = "Return ID: #${selectedItem.id}"

        bottomSheetBinding.tvStatus.text = "Status: ${getStatusText(selectedItem.status)}"
        bottomSheetBinding.tvRequestedDate.text = "Requested Date: ${formatDate(selectedItem.requested_date)}"


        val adapter = ProductReturnAlertAdapter(selectedItem.products)
        bottomSheetBinding.detailsRcv.layoutManager = LinearLayoutManager(this)
        bottomSheetBinding.detailsRcv.adapter = adapter

        bottomSheetBinding.closeBottomsheet.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }




    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // adjust pattern to match your string
            val outputFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr // fallback
        }
    }
}












