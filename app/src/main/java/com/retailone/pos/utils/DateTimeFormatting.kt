package com.retailone.pos.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DateTimeFormatting {



    companion object {
        fun formatOrderdate(inputDate: String,zone: String): String {
           lateinit var timezone :String

            if (zone == "IST"){
                timezone = "Asia/Kolkata"
            }else if(zone == "CAT"){
                timezone = "Africa/Lusaka"
            }else{
                timezone = "Africa/Lusaka"
            }

            try {
               // val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.getDefault())
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
                inputFormat.timeZone = TimeZone.getTimeZone("Asia/Kolkata")

                val outputFormat = SimpleDateFormat("ddMMM [hh:mma]", Locale.ENGLISH)
                outputFormat.timeZone = TimeZone.getTimeZone(timezone)


                val date = inputFormat.parse(inputDate)
                return outputFormat.format(date)
            } catch (e: ParseException) {
                // Print the input date string to help diagnose the issue
                println("Error parsing date. Input date string: $inputDate")
                e.printStackTrace()
                return "Error parsing date"
            }
        }


        fun formatApprovedate(inputDate: String,zone: String): String {

//            timezone = when (zone) {
//                "IST" -> "Asia/Kolkata"
//                "CAT" -> "Africa/Lusaka"
//                else -> "Africa/Lusaka"  // Default to CAT if zone isn't recognized
//            }

            var timezone = "Asia/Kolkata"

            if (zone == "IST"){
                timezone = "Asia/Kolkata"
            }else if(zone == "CAT"){
                timezone = "Africa/Lusaka"
            }else{
                timezone = "Africa/Lusaka"
            }

            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone(timezone)

                val outputFormat = SimpleDateFormat("ddMMM [hh:mma]", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getTimeZone(timezone)


                val date = inputFormat.parse(inputDate)
                return outputFormat.format(date)
            } catch (e: ParseException) {
                // Print the input date string to help diagnose the issue
                println("Error parsing date. Input date string: $inputDate")
                e.printStackTrace()
                return "Error parsing date"
            }

        }

        fun formatReceivedate(inputDate: String,zone: String): String {

            var timezone = "Asia/Kolkata"

            if (zone == "IST"){
                timezone = "Asia/Kolkata"
            }else if(zone == "CAT"){
                timezone = "Africa/Lusaka"
            }else{
                timezone = "Africa/Lusaka"
            }

            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                //inputFormat.timeZone = TimeZone.getTimeZone("Africa/Lusaka")
                inputFormat.timeZone = TimeZone.getTimeZone(timezone)


                val outputFormat = SimpleDateFormat("ddMMM [hh:mma]", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getTimeZone(timezone)


                val date = inputFormat.parse(inputDate)
                return outputFormat.format(date)
            } catch (e: ParseException) {
                // Print the input date string to help diagnose the issue
                println("Error parsing date. Input date string: $inputDate")
                e.printStackTrace()
                return "Error parsing date"
            }

        }

        fun formatDispatchDate(inputDate: String, timezone: String): String {

            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("ddMMM", Locale.getDefault())

                val date = inputFormat.parse(inputDate)
                return outputFormat.format(date)
            } catch (e: ParseException) {
                // Print the input date string to help diagnose the issue
                println("Error parsing date. Input date string: $inputDate")
                e.printStackTrace()
                return "Error parsing date"
            }

        }


        fun formatGlobalTime(inputDate: String,zone: String): String {
            var timezone = "Asia/Kolkata"

            if (zone == "IST"){
                timezone = "Asia/Kolkata"
            }else if(zone == "CAT"){
                timezone = "Africa/Lusaka"
            }else{
                timezone = "Africa/Lusaka"
            }

            try {
                // val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.getDefault())
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")

                val outputFormat = SimpleDateFormat("ddMMM [hh:mma]", Locale.ENGLISH)
                outputFormat.timeZone = TimeZone.getTimeZone(timezone)


                val date = inputFormat.parse(inputDate)
                return outputFormat.format(date)
            } catch (e: ParseException) {
                // Print the input date string to help diagnose the issue
                println("Error parsing date. Input date string: $inputDate")
                e.printStackTrace()
                return "Error parsing date"
            }
        }

        fun formatSaleReturndate(inputDate: String,zone: String): String {
            lateinit var timezone :String

            if (zone == "IST"){
                timezone = "Asia/Kolkata"
            }else if(zone == "CAT"){
                timezone = "Africa/Lusaka"
            }else{
                timezone = "Africa/Lusaka"
            }

            try {
                // val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.getDefault())
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")

                val outputFormat = SimpleDateFormat("ddMMM [hh:mma]", Locale.ENGLISH)
                outputFormat.timeZone = TimeZone.getTimeZone(timezone)


                val date = inputFormat.parse(inputDate)
                return outputFormat.format(date)
            } catch (e: ParseException) {
                // Print the input date string to help diagnose the issue
                println("Error parsing date. Input date string: $inputDate")
                e.printStackTrace()
                return "Error parsing date"
            }
        }


        fun formatReturndate(inputDate: String,zone: String): String {
            lateinit var timezone :String

            if (zone == "IST"){
                timezone = "Asia/Kolkata"
            }else if(zone == "CAT"){
                timezone = "Africa/Lusaka"
            }else{
                timezone = "Africa/Lusaka"
            }

            try {
                // val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.getDefault())
                val inputFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.ENGLISH)
                inputFormat.timeZone = TimeZone.getTimeZone("Africa/Lusaka")

                val outputFormat = SimpleDateFormat("ddMMM [hh:mma]", Locale.ENGLISH)
                outputFormat.timeZone = TimeZone.getTimeZone(timezone)


                val date = inputFormat.parse(inputDate)
                return outputFormat.format(date)
            } catch (e: ParseException) {
                // Print the input date string to help diagnose the issue
                println("Error parsing date. Input date string: $inputDate")
                e.printStackTrace()
                return "Error parsing date"
            }
        }
    }


}