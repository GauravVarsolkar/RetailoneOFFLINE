package com.retailone.pos.ui.Activity.DashboardActivity

import ReturnedProduct
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.retailone.pos.adapter.ConfirmReturnProductAdapter
import com.retailone.pos.adapter.StockListAdapter
import com.retailone.pos.databinding.ActivityConfirmreturnReceiptBinding
import com.retailone.pos.databinding.ActivityGoodsReturntoWarehouseBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.models.Dispatch.DispatchRequest
import com.retailone.pos.viewmodels.DashboardViewodel.ConfirmReturnViewModel
import com.retailone.pos.viewmodels.DashboardViewodel.GoodsReturntoWarehouseViewModel
import com.retailone.pos.viewmodels.DashboardViewodel.StockReturnViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ConfirmReturnReceiptActivity: AppCompatActivity()  {
    lateinit var  binding: ActivityConfirmreturnReceiptBinding
    private lateinit var viewModel: ConfirmReturnViewModel
    private var returnId: Int = -1
    private var date: String = ""
    private var status: Int = -1
    private  var sealnumber: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConfirmreturnReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)


        viewModel = ViewModelProvider(this)[ConfirmReturnViewModel::class.java]

        val productList = intent.getParcelableArrayListExtra<ReturnedProduct>("product_list")

        productList?.forEach {
            println("Product Name: ${it.product?.product_name}, Quantity: ${it.quantity}")
        }
        productList?.let {
            val adapter = ConfirmReturnProductAdapter(it)
            binding.rvProductList.layoutManager = LinearLayoutManager(this)
            binding.rvProductList.adapter = adapter
        }

        returnId = intent.getIntExtra("return_id", -1)
        status = intent.getIntExtra("status", -1)
        date = intent.getStringExtra("date") ?: ""
        sealnumber = intent.getStringExtra("seal_no")?: ""

        // ✅ Set text from previous activity
        binding.valueDate.text = formatDate(date)

        binding.valueStatus.text = getStatusText(status)
        binding.etSeal.setText(sealnumber)
        Toast.makeText(this, "Opened Dispatch for ID: $returnId", Toast.LENGTH_SHORT).show()

        binding.btnConfirm.setOnClickListener {
            val seal = binding.etSeal.text.toString().trim()
            val vehicle = binding.etVehicle.text.toString().trim()
            val driver = binding.etDriver.text.toString().trim()

            if (seal.isEmpty() || vehicle.isEmpty() || driver.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = DispatchRequest(
                id = returnId,
                seal_no = seal,
                vehicle_no = vehicle,
                driver_name = driver
            )

            viewModel.dispatchStock(request, this)
        }

        viewModel.dispatchLiveData.observe(this) { response ->
            Toast.makeText(this, response.message, Toast.LENGTH_LONG).show()
            if (response.success) {
                val intent = Intent(this, proceedToDispatchActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // or navigate back
            }
        }

        viewModel.loadingLiveData.observe(this) { progress ->
            binding.btnConfirm.isEnabled = !progress.isProgress
        }




    }
    private fun getStatusText(status: Int): String {
        return when (status) {
            0 -> "Pending"
            2 -> "Approved"
            3 -> "Rejected"
            4 -> "Dispatched"
            5 -> "Received"
            else -> "Unknown"
        }
    }
    private fun formatDate(inputDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Original format
            val outputFormat = SimpleDateFormat("yyyy/dd/MM", Locale.getDefault()) // Desired format
            val date = inputFormat.parse(inputDate)
            if (date != null) outputFormat.format(date) else inputDate
        } catch (e: Exception) {
            inputDate // Fallback to original if parsing fails
        }
    }


}