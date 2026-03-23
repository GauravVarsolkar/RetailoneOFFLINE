import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Window
import android.widget.ProgressBar
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageCompressor(private val context: Context) {

    private lateinit var progressDialog: Dialog

    // Function to show a simple progress dialog
    private fun showProgressDialog() {
        progressDialog = Dialog(context)
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        progressDialog.setCancelable(false)
        progressDialog.setContentView(ProgressBar(context)) // Simple progress bar
        progressDialog.show()
    }

    // Function to hide the progress dialog
    private fun hideProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    // Compress the image asynchronously
    fun compressImageAsync(imageFilePath: String, onComplete: (File) -> Unit) {
        // Show the progress dialog
        showProgressDialog()

        // Start coroutine for image compression in background
        CoroutineScope(Dispatchers.IO).launch {
            val compressedFile = compressImage(imageFilePath)

            // Switch back to Main thread to update UI and hide progress dialog
            withContext(Dispatchers.Main) {
                hideProgressDialog()
                // Call onComplete callback with the result file
                onComplete(compressedFile)
            }
        }
    }

    // Compress image logic
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
        val compressedFile = File.createTempFile("compressed_image", ".jpg", context.cacheDir)
        val fileOutputStream = FileOutputStream(compressedFile)
        fileOutputStream.write(outputStream.toByteArray())
        fileOutputStream.close()

        return compressedFile
    }
}
