//package com.retailone.pos.ui.Activity
//
//import android.content.DialogInterface
//import android.content.Intent
//import android.os.Bundle
//import android.view.View
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import com.common.apiutil.pos.RS232Reader
//import com.common.callback.IRSReaderListener
//import com.retailone.pos.databinding.ActivityFetchTotdataBinding
//
//class FetchTOTData : AppCompatActivity(), IRSReaderListener {
//    private var binding: ActivityFetchTotdataBinding? = null
//    private var clickCount = 0
//    private var last_issued_cmd: Last_CMD? = null
//    private val mRS232Reader: RS232Reader? = null
//    private var startTotalizer = 0f
//    private var endTotalizer = 0f
//    private val DU_Unit_Id = 1
//    private val Hose_No = 1
//    private var DU_STATUS = -1
//    private var startTotalizerString = ""
//
//    enum class Last_CMD {
//        STATUS, START_TOTALIZER, END_TOTALIZER, PRESET, LAST_TXN
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityFetchTotdataBinding.inflate(layoutInflater)
//        setContentView(binding!!.root)
//
//        val userStatus = intent.getStringExtra("USER_STATUS")
//
//        if ("LoggedIn" == userStatus) {
//            binding!!.ctstartTOT.visibility = View.VISIBLE
//            start_tot()
//            binding!!.btnReadTotalizer.setOnClickListener { v ->
//                clickCount++
//                // Show toast message
//                Toast.makeText(
//                    this@FetchTOTData,
//                    "Data fetching...",
//                    Toast.LENGTH_SHORT
//                ).show()
//                if (clickCount > 3) {
//                    binding!!.tvPleaseWait.visibility = View.GONE
//                    binding!!.etTotalizerValue.visibility = View.VISIBLE
//                    binding!!.btnReadTotalizer.visibility = View.INVISIBLE
//                    binding!!.proceedBtn.visibility = View.VISIBLE
//                }
//            }
//
//            binding!!.proceedBtn.setOnClickListener { v ->
//                startTotalizerString = binding!!.etTotalizerValue.text.toString().trim()
//                startTotalizer =
//                    if (startTotalizerString.isEmpty()) 0f else startTotalizerString.toFloat()
//                if (startTotalizer == 0f) {
//                    // Show an alert if the EditText is empty or invalid
//                    AlertDialog.Builder(this@FetchTOTData)
//                        .setTitle("Error")
//                        .setMessage("Please enter a valid start totalizer value")
//                        .setPositiveButton(
//                            "OK"
//                        ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
//                        .show()
//                } else {
//                    // Save the start totalizer value to SharedPreferences
//                    getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
//                        .putString("start_totalizer_value", startTotalizer.toString())
//                        .apply()
//
//                    // Navigate to the next screen
//                    val intent =
//                        Intent(this@FetchTOTData, MPOSDashboardActivity::class.java)
//                    startActivity(intent)
//                }
//            }
//        }
//    }
//
//    fun start_tot() {
//        try {
//            last_issued_cmd = Last_CMD.STATUS
//            mRS232Reader!!.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "0B0"))
//            clickCount = 0
//            if (DU_STATUS == 1) {
//                last_issued_cmd = Last_CMD.START_TOTALIZER
//                mRS232Reader.rsSend(HexToByteArr("F" + DU_Unit_Id + Hose_No + "9B0"))
//            } else {
//                binding!!.tvPleaseWait.text =
//                    "DU is in OFF state. Please restart the DU and try again..."
//            }
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//        }
//    }
//
//    fun HexToByteArr(inHex: String): ByteArray {
//        var inHex = inHex
//        var hexlen = inHex.length
//        val result: ByteArray
//        if (isOdd(hexlen) == 1) { // Odd
//            hexlen++
//            result = ByteArray(hexlen / 2)
//            inHex = "0$inHex"
//        } else { // Even
//            result = ByteArray(hexlen / 2)
//        }
//
//        var j = 0
//        var i = 0
//        while (i < hexlen) {
//            result[j] = HexToByte(inHex.substring(i, i + 2))
//            j++
//            i += 2
//        }
//        return result
//    }
//
//    fun isOdd(num: Int): Int {
//        return num and 0x1
//    }
//
//    fun HexToByte(inHex: String): Byte {
//        return inHex.toInt(16) as Byte
//    }
//
//    fun byteArrayToHex(a: ByteArray): String {
//        val sb = StringBuilder(a.size * 2)
//        for (b in a) sb.append(String.format("%02x", b))
//        return sb.toString()
//    }
//
//    override fun onRecvData(data: ByteArray) {
//        val msg = RSSerialActivity.byteArrayToHex(data)
//        if (last_issued_cmd == Last_CMD.STATUS && msg.length == 2) {
//            DU_STATUS = msg.substring(1, 2).toInt()
//        } else if (last_issued_cmd == Last_CMD.START_TOTALIZER && msg.length >= 20) {
//            var tot = msg.substring(4, 20)
//            tot = (tot.substring(15) + tot.substring(13, 14) + tot.substring(
//                11,
//                12
//            ) + tot.substring(9, 10)
//                    + tot.substring(7, 8) + tot.substring(5, 6) + tot.substring(
//                3,
//                4
//            ) + tot.substring(1, 2))
//            startTotalizer = (tot.toLong() / 100f)
//        } else if (last_issued_cmd == Last_CMD.END_TOTALIZER && msg.length >= 20) {
//            var tot = msg.substring(4, 20)
//            tot = (tot.substring(15) + tot.substring(13, 14) + tot.substring(
//                11,
//                12
//            ) + tot.substring(9, 10)
//                    + tot.substring(7, 8) + tot.substring(5, 6) + tot.substring(
//                3,
//                4
//            ) + tot.substring(1, 2))
//            endTotalizer = (tot.toLong() / 100f)
//        }
//    }
//}