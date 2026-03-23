package com.retailone.pos.utils

import android.content.Context
import android.util.Log
import java.io.File

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Write crash log to a file
        saveCrashLogToFile(throwable)

        // You can also send logs to a server here if needed

        // Call the default exception handler to handle the crash as usual
        defaultHandler?.uncaughtException(thread, throwable)
    }


    private fun saveCrashLogToFile(throwable: Throwable) {
        try {
            // Get the existing crash logs
            val file = File(context.filesDir, "crash_log.txt")
            val existingLogs = if (file.exists()) file.readText() else ""

            // Create the new crash log entry
            val crashLog = Log.getStackTraceString(throwable)

            // Combine the new crash log with the existing logs
            val updatedLogs = "$crashLog\n$existingLogs"

            // Write the updated logs back to the file
            file.writeText(updatedLogs) // This will overwrite the file with the new content
        } catch (e: Exception) {
            // Handle any exceptions related to file writing
            e.printStackTrace()
        }
    }


//    private fun saveCrashLogToFile(throwable: Throwable) {
//        try {
//            val crashLog = Log.getStackTraceString(throwable)
//            val file = File(context.filesDir, "crash_log.txt")
//            file.appendText(crashLog + "\n")
//        } catch (e: Exception) {
//            // Handle any exceptions related to file writing
//        }
//    }
}
