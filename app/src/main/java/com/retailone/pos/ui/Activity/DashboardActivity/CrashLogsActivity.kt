package com.retailone.pos.ui.Activity.DashboardActivity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import com.retailone.pos.R
import java.io.File


class CrashLogsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_logs)

        // Get the TextView from the layout
        val crashLogsTextView = findViewById<TextView>(R.id.crashLogsTextView)

        // Read the crash logs and display them
        val crashLogs = readCrashLogs(this)
        crashLogsTextView.text = crashLogs
        crashLogsTextView.movementMethod = ScrollingMovementMethod()

    }

    // Method to read crash logs from file
    fun readCrashLogs(context: Context): String {
        val file = File(context.filesDir, "crash_log.txt")
        return if (file.exists()) {
            file.readText()
        } else {
            "No crash logs found."
        }
    }
}
