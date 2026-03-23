package com.retailone.pos.ui.Activity.DashboardActivity

import ImageCompressor
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.retailone.pos.R
import com.retailone.pos.adapter.MaterialReceivedAdapter
import com.retailone.pos.databinding.ActivityMaterialReceivingBinding
import com.retailone.pos.localstorage.DataStore.LoginSession
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper
import com.retailone.pos.localstorage.SharedPreference.OrganisationDetailsHelper
import com.retailone.pos.models.LocalizationModel.LocalizationData
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatInvItem
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvInvReq
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvItem
import com.retailone.pos.models.MaterialRcvModel.MaterialRcvInv.MatRcvOrderItem
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsList
import com.retailone.pos.models.StockRequisitionModel.PastReqDetailsModel.PastReqDetailsRes
import com.retailone.pos.ui.Activity.MPOSDashboardActivity
import com.retailone.pos.utils.DateTimeFormatting
import com.retailone.pos.viewmodels.DashboardViewodel.MaterialReceivingViewmodel
import com.retailone.pos.viewmodels.DashboardViewodel.StockRequisitionViewmodel
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


class MaterialReceivingActivity : AppCompatActivity() {
    lateinit var  binding:ActivityMaterialReceivingBinding
    lateinit var matrcvViewmodel: MaterialReceivingViewmodel
    lateinit var stockRequisitionViewmodel: StockRequisitionViewmodel


    lateinit var matrcvdAdapter:MaterialReceivedAdapter
    private var matReceivedList = mutableListOf<MatRcvItem>()

    var storeid = ""
    lateinit var  pastReqDetailsRes: PastReqDetailsRes
    lateinit var  localizationData: LocalizationData

    lateinit var mat_rcv_inv_req: MatRcvInvReq

    lateinit var fileProviderUri: Uri
    private var PERMISSIONS: Array<String> = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    //private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>

    private lateinit var imageCompressor: ImageCompressor


    var isImgSelected = false
    var invoiceFilePath = ""




    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMaterialReceivingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableBackButton()

        matrcvViewmodel = ViewModelProvider(this)[MaterialReceivingViewmodel::class.java]
        stockRequisitionViewmodel = ViewModelProvider(this)[StockRequisitionViewmodel::class.java]
        localizationData = LocalizationHelper(this).getLocalizationData()



        val loginSession= LoginSession.getInstance(this)
        lifecycleScope.launch {
            storeid = loginSession.getStoreID().first().toString()
            //default search
           // pos_viewmodel.callSearchStoreProductApi("",storeid,this@PointOfSaleActivity)
        }
       imageCompressor = ImageCompressor(this)


        prepareRecycleview()
      //  setupGalleryLauncher()
        setupCameraLauncher()

        if (Build.VERSION.SDK_INT >= 33) {
            PERMISSIONS = arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
        }

        setupRequesrPermissionLauncher()



        binding.scanstn.setOnClickListener {

            if (checkPermission()) {
                openImageUploadBottomsheet()
            } else {
                requestPermissionLauncher.launch(PERMISSIONS)
            }
        }

        //val request_id = intent?.getStringExtra("request_id")
        val request_id = intent.getStringExtra("request_id")

        stockRequisitionViewmodel.callRequisitionDetailsApi(request_id.toString(),this)



        stockRequisitionViewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            if(it.isMessage)
                showMessage(it.message)
        }

        matrcvViewmodel.loadingLiveData.observe(this){
            binding.progress.isVisible = it.isProgress

            binding.relativeLayout.isVisible = !it.isProgress


            if(it.isMessage)
                showMessage(it.message)
        }

        matrcvViewmodel.material_received_submit_livedata.observe(this){
            //showMessage(it.message)
            showSucessDialog(it.message)
        }



        stockRequisitionViewmodel.past_req_details_livedata.observe(this){

            pastReqDetailsRes = it //requisition details

            matrcvdAdapter = MaterialReceivedAdapter(this,it.data[0],it.data[0].dispatch_date){
                matReceivedList = it.toMutableList()

                Log.d("rcvx",matReceivedList.toString())
            }
            binding.materiallistRcv.adapter = matrcvdAdapter

            setStatusWiseUI(it.data[0])
        }

        matrcvViewmodel.invoiceupload_liveData.observe(this) {

            if (it.status == 1) {
                //val updatedreqdata = expenseSubmitReq.copy(invoice = it.image_url)
                val final_mat_rcv_inv_req = mat_rcv_inv_req.copy(stn = it.image_url)

               Log.d("ex","hii"+ Gson().toJson(final_mat_rcv_inv_req).toString())
                matrcvViewmodel.callMaterialReceivedSubmitApi(final_mat_rcv_inv_req,this)

            } else {
                showMessage("STN upload failed")
            }

        }

        binding.nextlayout.setOnClickListener {

            val material_list = matReceivedList

            if (material_list.isNotEmpty()) {

                val material_inv_list = mutableListOf<MatInvItem>() //list for inventory item

                material_list.forEachIndexed { index, matReceivedItem ->
                    val prduct_info = pastReqDetailsRes.data[0].order_items[index]
                    material_inv_list.add(
                        MatInvItem(
                                 category_id = prduct_info.category_id,
                                 distribution_pack_id = prduct_info.distribution_pack_id.toInt(),
                                 product_id = prduct_info.product_id,
                                 quantity = material_list[index].received_quantity.toString(),
                                 supplier_id = prduct_info.product_details.supplier_id?:"0",
                            expiry_date = prduct_info.expiry_date,
                            //batch_no = prduct_info.batch_no

                            batch_no = material_list[index].batch_list

                            )
                        )
                    }

                validateDetails(request_id.toString(),binding.vehnameInput.text.toString(),binding.vehnumInput.text.toString(),matReceivedList,material_inv_list)

            }else{
                showMessage("please receive all the materials")
            }


            setToolbarImage()
        }






        /*
                matrcvViewmodel.getMaterialRcvData()
                matrcvViewmodel.material_rcv_LiveData.observe(this){
                    matrcvdAdapter.setMatRcvdData(it)

                }*/

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setStatusWiseUI(orderdata: PastReqDetailsList) {
        binding.orderId.text = "OrderID - ${orderdata.order_id?:""}"

        when (orderdata.status) {
            "0" -> {
                //textview.text = "Pending"
                binding.relativeLayout.isVisible = false
                binding.lorrydetailsCard.isVisible = false
                binding.orderStatus.text = "Status - Order waiting for approval"

            }

            "1" -> {
                //textview.text = "Approved"
                binding.relativeLayout.isVisible = false
                binding.lorrydetailsCard.isVisible = false
                binding.orderStatus.text = "Status - Order approved, waiting for dispatch"


            }

            "4" -> {
                //textview.text = "dispatched"
                if(isBeforeDispatchDate(orderdata.dispatch_date!!)){
                    binding.relativeLayout.isVisible = false
                    binding.lorrydetailsCard.isVisible = false
                    binding.orderStatus.text = "Status - Order dispatched"

                }else{
                    binding.relativeLayout.isVisible = true

                    binding.lorrydetailsCard.isVisible = true
                    binding.scanstn.isVisible = true

                    binding.vehnameInput.isFocusable = false
                    binding.vehnumInput.isFocusable = false
                    binding.vehnameInput.text = Editable.Factory.getInstance().newEditable(pastReqDetailsRes.data[0].driver_name?:"")
                    binding.vehnumInput.text = Editable.Factory.getInstance().newEditable(pastReqDetailsRes.data[0].vehicle_no?:"")
                    binding.orderStatus.text = "Status - Order dispatched"
                }


            }

            "2" -> {
                //textview.text = "Cancelled"
                binding.relativeLayout.isVisible = false
                binding.lorrydetailsCard.isVisible = false
                binding.orderStatus.text = "Status - Order Cancelled"

            }

            "3" -> {
                //textview.text = "Received"
                binding.relativeLayout.isVisible = false

                binding.lorrydetailsCard.isVisible = true
                binding.scanstn.isVisible = false

                binding.orderStatus.text = "Status - Order has been received successfully"

                binding.vehnumInput .text =  Editable.Factory.getInstance().newEditable( orderdata.vehicle_no?:"")
                binding.vehnameInput .text =  Editable.Factory.getInstance().newEditable( orderdata.driver_name?:"")
                binding.vehnumInput.isFocusable = false
                binding.vehnameInput.isFocusable = false

            }

            else -> {
                // textview.text = "."
                binding.relativeLayout.isVisible = false
                binding.lorrydetailsCard.isVisible = false
                binding.orderStatus.text = "Status -"
            }
        }

        orderdata.created_at?.let {
            binding.orderdate.isVisible = true
            binding.orderdate.text ="Ordered at: "+ DateTimeFormatting.formatOrderdate(it,localizationData.timezone)

        }
        orderdata.approve_date?.let {
            binding.approvedate.isVisible = true
            binding.approvedate.text ="Approved at: "+ DateTimeFormatting.formatApprovedate(it,localizationData.timezone)
        }

        orderdata.receive_date?.let {
            binding.receivedate.isVisible = true
            binding.receivedate.text ="Received at: "+ DateTimeFormatting.formatReceivedate(it,localizationData.timezone)
        }

    }



        @RequiresApi(Build.VERSION_CODES.O)
        fun isBeforeDispatchDate(dispatchDate: String): Boolean {
            // Define the date format
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            // Parse the dispatch date
            val dispatchDateTime = LocalDate.parse(dispatchDate, formatter)

            // Get the current date
            val currentDate = LocalDate.now()

            // Compare the current date with the dispatch date
            return currentDate.isBefore(dispatchDateTime)
        }


    fun createMatRcvOrderList(matReceivedList: List<MatRcvItem>): List<MatRcvOrderItem> {
        return matReceivedList.map { matItem ->
            // If the batch_list is not empty, sum up the received_quantity from the batches
            val totalReceivedQuantity = if (matItem.batch_list.isNotEmpty()) {
                matItem.batch_list.sumOf { batch -> batch.received_quantity }
            } else {
                matItem.received_quantity
            }

            // Create a MatRcvOrderItem with the summed received_quantity
            MatRcvOrderItem(
                id = matItem.id,
                received_quantity = totalReceivedQuantity
            )
        }
    }






    private fun validateDetails(
        id: String,
        vehname: String,
        vehnum: String,
        matList: MutableList<MatRcvItem>,
        material_inv_list: MutableList<MatInvItem>
    ) {
        if(matList.size == 0){
            showMessage("Please Receive at least one item")
        }else if( matList.all { it.received_quantity == 0 }){
            showMessage("all received item can't be empty or zero")
        }else if(vehnum.isBlank()||vehnum.isEmpty()){
            showMessage("Please Enter Vehicle Number")
        }else if(vehname.isBlank()||vehname.isEmpty()){
            showMessage("Please Enter Driver Name")
        }else if (!isImgSelected || invoiceFilePath == "") {
        showMessage("Please Scan STN")

    }
        else{

            val matRcvOrderList: List<MatRcvOrderItem> = createMatRcvOrderList(matReceivedList)

             mat_rcv_inv_req = MatRcvInvReq(driver_name = vehname, vehicle_no =  vehnum,
                purchase_request_id = id.toInt(), order_items = matRcvOrderList, products =material_inv_list, store_id = storeid,stn="" )

            val gson = Gson()
            val jsonString = gson.toJson(mat_rcv_inv_req)

            //Log.d("zzz",jsonString)

           showConfirmDialog(invoiceFilePath,mat_rcv_inv_req,this)
        }


    }


    private fun prepareRecycleview() {

        binding.materiallistRcv.apply {
            layoutManager = LinearLayoutManager(this@MaterialReceivingActivity,
                RecyclerView.VERTICAL,false)

        }
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

    private fun showSucessDialog(msg:String) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.sucess_dialog_layout)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(false)

        val confirm = dialog.findViewById<MaterialButton>(R.id.prefer_confirm)
        val logoutMsg = dialog.findViewById<TextView>(R.id.logout_msg)
        val logoutImg = dialog.findViewById<ImageView>(R.id.dialog_logo)

        logoutMsg.text = msg
        logoutMsg.textSize = 16F
        //logoutImg.setImageResource(R.drawable.svg_off)
        //logoutImg.scaleType = ImageView.ScaleType.FIT_CENTER

        confirm.setOnClickListener {
            dialog.dismiss()


            val intent = Intent(this@MaterialReceivingActivity, MPOSDashboardActivity::class.java)
            startActivity(intent)
            finish()

        }

        dialog.show()


    }

    private fun showConfirmDialog(
        filepath:String,
        materialReceivedReq: MatRcvInvReq,
        context: Context
    ) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.logout_dialog_layout)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(false)

        val cancel = dialog.findViewById<MaterialButton>(R.id.prefer_cancel)
        val confirm = dialog.findViewById<MaterialButton>(R.id.prefer_confirm)
        val logoutMsg = dialog.findViewById<TextView>(R.id.logout_msg)
        val logoutImg = dialog.findViewById<ImageView>(R.id.dialog_logo)

        logoutMsg.text = "Are you sure you want to receive the materials?"
        logoutMsg.textSize = 16F
        logoutImg.setImageResource(R.drawable.svg_attention)
        logoutImg.scaleType = ImageView.ScaleType.FIT_CENTER

        confirm.setOnClickListener {
            dialog.dismiss()
            //binding.relativeLayout.isVisible = false

            uploadFile(filepath)




            /// matrcvViewmodel.callMaterialReceivedSubmitApi(materialReceivedReq,context)

        }

        cancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()


    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this@MaterialReceivingActivity, msg, Toast.LENGTH_SHORT).show()
    }

    // image upload



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

        // Create a temporary file to store the captured image
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            // Handle the error
            showMessage("Error creating image file")
            return
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

    }
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

        binding.stnLayout.isVisible=true
        binding.stnImage.setImageURI(selectedImageUri)

    }

    private fun uploadFile(imageFilePath: String) {

      //  val imageCompressor = ImageCompressor(this)

//        imageCompressor.compressImageAsync("/path/to/image.jpg") { compressedFile ->
//            // This block is called when compression is done
//            // Use the compressed file
//            println("Compressed image saved at: ${compressedFile.path}")
//        }


        //val file = File(imageFilePath)

      ////  val file = compressImage(imageFilePath)

        imageCompressor.compressImageAsync(imageFilePath) { compressedFile ->
            // This block is called when compression is done
            // Use the compressed file
          //  println("Compressed image saved at: ${compressedFile.path}")

            val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), compressedFile)
            val filePart = MultipartBody.Part.createFormData("image", compressedFile.name, requestFile)
            matrcvViewmodel.callSTNUploadApi(filePart, this)
        }







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


//        val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
//        val filePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
//        matrcvViewmodel.callSTNUploadApi(filePart, this)


        //val bearerToken = "Bearer $token"

    }

    private fun compressImage(imageFilePath: String): File {
        // Decode the image file to a bitmap
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Reduce memory usage
        val bitmap = BitmapFactory.decodeFile(imageFilePath, options)

        // Compress image to fit within the desired size of 500KB
        val desiredSize = 512 * 1024 // 500KB
        val outputStream = ByteArrayOutputStream()
        var quality = 100
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        // Reduce quality in steps until the file size is below 500KB
        while (outputStream.size() > desiredSize && quality > 0) {
            outputStream.reset()
            quality -= 5 // Reduce quality gradually
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