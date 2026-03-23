package com.retailone.pos.utils

object FunUtils {

   // clickitem.pack_product_description.trim().contains("loose oil", ignoreCase = true)

    //Imp in ReturnsaleItemAdapter  product_id will replace with category_id

    fun isLooseOil(catId: Int,desc:String): Boolean {
       // return catId == 0

       if(desc.trim().contains("loose oil", ignoreCase = true)) {
           return true
       }else {
           return  false
           //return  true
       }
    }

    fun DtoInt(input: Double): Int {
        return input.toInt()
    }

    // Convert Double to Double with 2 decimal places
    fun DtoDouble(input: Double): Double {
        return String.format("%.2f", input).toDouble()
    }

//    fun DtoString(input: Double): String {
//        return if (input % 1 == 0.0) {
//            input.toInt().toString()  // No decimal part, return as integer
//        } else {
//            String.format("%.2f", input)  // Return with 2 decimal places
//        }
//    }

    fun DtoString(input: Double): String {
        return if (input % 1 == 0.0) {
            input.toInt().toString()  // No decimal part, return as integer
        } else {
            // Format with two decimal places, but conditionally remove second digit if it's zero
            val formatted = String.format("%.2f", input)
            if (formatted.endsWith(".00")) {
                formatted.substring(0, formatted.indexOf("."))
            } else if (formatted.endsWith("0")) {
                formatted.substring(0, formatted.length - 1)
            } else {
                formatted
            }
        }
    }

    fun stringToDouble(input: String): Double {
        return try {
            val sanitizedInput = input.replace(",", "") // Remove thousand separators
            if (sanitizedInput.isBlank() || sanitizedInput == "." || sanitizedInput.toDoubleOrNull() == null) {
                0.0
            } else {
                sanitizedInput.toDouble()
            }
        } catch (e: NumberFormatException) {
            0.0 // Return 0.0 if the input cannot be parsed
        }
    }




    fun formatPrintPrice(numberString: String): String? {
        return try {
            // Remove commas from the string
            val sanitizedNumberString = numberString.replace(",", "")

            // Parse the string to a floating-point number
            val number = sanitizedNumberString.toFloat()

            // Check if the number has any decimal part
            if (number % 1 == 0f) {
                // If there's no decimal part (both digits after the decimal are zero), return as an integer
                number.toInt().toString()
            } else {
                // Else return the number with two decimal places
                String.format("%.2f", number)
            }
        } catch (e: NumberFormatException) {
            // Handle invalid number format (if the input string isn't a valid number)
            "--"
        }
    }



}