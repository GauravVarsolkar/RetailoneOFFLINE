import com.retailone.pos.models.LocalizationModel.LocalizationData
import java.text.NumberFormat
import java.util.Locale

class NumberFormatter(private val locale: Locale = Locale.getDefault()) {

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(locale)

    fun formatPrice(value: String, localizationData: LocalizationData): String {
        if (localizationData.thousand_separator == "1") {
            try {
                val doubleValue = value.toDouble()
                numberFormat.minimumFractionDigits = 2
                numberFormat.maximumFractionDigits = 2
                return localizationData.currency + numberFormat.format(doubleValue)
            } catch (e: NumberFormatException) {
                // Handle the case where the input string is not a valid number
                return localizationData.currency + value
            }
        } else {
            return localizationData.currency + value
        }
    }

    fun formatWithThousandSeparator(value: Double): String {
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 2
        return numberFormat.format(value)
    }

    fun formatWithThousandSeparator(value: Long): String {
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 2
        return numberFormat.format(value)
    }
}
