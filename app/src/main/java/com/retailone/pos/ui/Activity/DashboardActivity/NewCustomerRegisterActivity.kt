package com.retailone.pos.ui.Activity.DashboardActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.retailone.pos.R
import com.retailone.pos.databinding.ActivityNewCustomerRegisterBinding


class NewCustomerRegisterActivity : AppCompatActivity() {
    lateinit var  binding :ActivityNewCustomerRegisterBinding
    lateinit var pdfPickerLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewCustomerRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //enableBackButton()

        pdfPickerLauncher = registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val pdfUri = data.data
                    val pdfName: String ?= getFileName(pdfUri!!)
                    binding.invoiceText .setText(pdfName)
                    // Do something with the selected PDF file
                }
            } else {
                Toast.makeText(this@NewCustomerRegisterActivity, "No PDF selected", Toast.LENGTH_SHORT)
                    .show()
            }
        }



        binding.saveBtn.setOnClickListener {
            val name = binding.customerNameInput .text.toString().trim()

            //put data to intent to get in previous activity
            val intent = Intent()
            intent.putExtra("name", name)
            setResult(RESULT_OK,intent)
            finish()
        }

        binding.invoiceLayout.setOnClickListener {
            openPdfPicker();


        }
    }


    private fun openPdfPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.setType("application/pdf")
        pdfPickerLauncher.launch(intent)
    }


    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result
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
}