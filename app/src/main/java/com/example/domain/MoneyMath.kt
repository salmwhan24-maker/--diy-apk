package com.example.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CurrencyOption(
    val code: String,
    val labelAr: String,
    val symbol: String,
) {
    USD("USD", "دولار أمريكي", "$"),
    SAR("SAR", "ريال سعودي", "ر.س"),
    YER("YER", "ريال يمني", "ر.ي");

    companion object {
        fun fromCode(code: String): CurrencyOption =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
    }
}

enum class SyncStatus(val labelAr: String) {
    LOCAL_SAVED("يعمل من النسخة المحلية المحفوظة"),
    ONLINE_UPDATED("تم التحديث عبر الإنترنت"),
    MANUAL_EDITED("معدل ومحفوظ محلياً");

    companion object {
        fun fromName(name: String): SyncStatus =
            entries.firstOrNull { it.name == name } ?: LOCAL_SAVED
    }
}

enum class InvoiceType(val labelAr: String, val badgeAr: String) {
    SALE("فاتورة بيع", "بيع"),
    PURCHASE("فاتورة شراء", "شراء");

    companion object {
        fun fromName(name: String): InvoiceType =
            entries.firstOrNull { it.name == name } ?: SALE
    }
}

data class CalculatedGoldPrice(
    val id: String,
    val nameAr: String,
    val purityRatio: BigDecimal,
    val sellPricePerGram: BigDecimal,
    val buyPricePerGram: BigDecimal,
    val sortOrder: Int,
)

data class DraftInvoiceItem(
    val localKey: Long,
    val categoryType: String,
    val karatId: String,
    val karatLabel: String,
    val description: String,
    val weightInput: String,
    val pricePerGramInput: String,
    val workmanshipInput: String,
) {
    val weightGrams: BigDecimal get() = MoneyMath.parseDecimal(weightInput)
    val pricePerGram: BigDecimal get() = MoneyMath.parseDecimal(pricePerGramInput)
    val workmanship: BigDecimal get() = MoneyMath.parseDecimal(workmanshipInput)
    val goldValue: BigDecimal get() = MoneyMath.calculateGoldValue(weightGrams, pricePerGram)
    val lineTotal: BigDecimal get() = MoneyMath.calculateLineTotal(weightGrams, pricePerGram, workmanship)
}

data class InvoiceTotals(
    val totalWeightGrams: BigDecimal,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val netTotal: BigDecimal,
)

object MoneyMath {
    val TROY_OUNCE_GRAMS = BigDecimal("31.1034768")
    private val STANDARD_SYMBOLS = DecimalFormatSymbols(Locale.US)

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '٠' -> sb.append('0')
                '١' -> sb.append('1')
                '٢' -> sb.append('2')
                '٣' -> sb.append('3')
                '٤' -> sb.append('4')
                '٥' -> sb.append('5')
                '٦' -> sb.append('6')
                '٧' -> sb.append('7')
                '٨' -> sb.append('8')
                '٩' -> sb.append('9')
                '٫', ',' -> sb.append('.')
                '−', '–' -> sb.append('-')
                else -> sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    fun parseDecimal(input: String): BigDecimal {
        val cleaned = normalizeDigits(input)
        if (cleaned.isEmpty() || cleaned == "." || cleaned == "-") return BigDecimal.ZERO
        return try {
            val parsed = BigDecimal(cleaned)
            if (parsed < BigDecimal.ZERO) BigDecimal.ZERO else parsed
        } catch (_: NumberFormatException) {
            BigDecimal.ZERO
        }
    }

    fun sanitizeDecimalInput(input: String): String {
        val normalized = normalizeDigits(input)
        val sb = StringBuilder()
        var dotSeen = false
        for (ch in normalized) {
            if (ch in '0'..'9') {
                sb.append(ch)
            } else if (ch == '.' && !dotSeen) {
                dotSeen = true
                if (sb.isEmpty()) sb.append('0')
                sb.append('.')
            }
        }
        return sb.toString()
    }

    fun calculateGoldValue(weightGrams: BigDecimal, pricePerGram: BigDecimal): BigDecimal {
        if (weightGrams <= BigDecimal.ZERO || pricePerGram <= BigDecimal.ZERO) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
        return weightGrams.multiply(pricePerGram).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateLineTotal(
        weightGrams: BigDecimal,
        pricePerGram: BigDecimal,
        workmanship: BigDecimal,
    ): BigDecimal {
        val goldVal = calculateGoldValue(weightGrams, pricePerGram)
        val work = if (workmanship < BigDecimal.ZERO) BigDecimal.ZERO else workmanship
        return goldVal.add(work).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateRawGoldPriceByPurity(
        pure24PricePerGram: BigDecimal,
        purityPerThousand: BigDecimal,
    ): BigDecimal {
        if (pure24PricePerGram <= BigDecimal.ZERO || purityPerThousand <= BigDecimal.ZERO) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
        val ratio = purityPerThousand.divide(BigDecimal("1000"), 6, RoundingMode.HALF_UP)
        return pure24PricePerGram.multiply(ratio).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateInvoiceTotals(items: List<DraftInvoiceItem>, discount: BigDecimal): InvoiceTotals {
        var totalWeight = BigDecimal.ZERO
        var subtotal = BigDecimal.ZERO
        for (item in items) {
            totalWeight = totalWeight.add(item.weightGrams)
            subtotal = subtotal.add(item.lineTotal)
        }
        val safeDiscount = if (discount < BigDecimal.ZERO) BigDecimal.ZERO else discount
        val net = subtotal.subtract(safeDiscount).let {
            if (it < BigDecimal.ZERO) BigDecimal.ZERO else it
        }
        return InvoiceTotals(
            totalWeightGrams = totalWeight.setScale(3, RoundingMode.HALF_UP),
            subtotal = subtotal.setScale(2, RoundingMode.HALF_UP),
            discount = safeDiscount.setScale(2, RoundingMode.HALF_UP),
            netTotal = net.setScale(2, RoundingMode.HALF_UP),
        )
    }

    fun formatAmount(amount: BigDecimal): String {
        val formatter = DecimalFormat("#,##0.00", STANDARD_SYMBOLS)
        return formatter.format(amount.setScale(2, RoundingMode.HALF_UP))
    }

    fun formatMoney(amount: BigDecimal, currencyCode: String): String {
        val currency = CurrencyOption.fromCode(currencyCode)
        return "${formatAmount(amount)} ${currency.symbol}"
    }

    fun formatGrams(grams: BigDecimal): String {
        val scaled = grams.setScale(3, RoundingMode.HALF_UP)
        val formatter = DecimalFormat("#,##0.###", STANDARD_SYMBOLS)
        return "${formatter.format(scaled)} جم"
    }

    fun formatPlainDecimal(value: BigDecimal, scale: Int = 2): String {
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString()
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd  hh:mm a", Locale("ar", "YE"))
        // Ensure Western digits for unambiguous financial reading
        val formatted = sdf.format(Date(millis))
        return normalizeDigits(formatted)
    }

    fun formatDateOnly(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        return sdf.format(Date(millis))
    }

    fun calculateSpotPricesFromOunce(
        ouncePriceUsd: BigDecimal,
        currencyCode: String,
        exchangeRate: BigDecimal,
    ): List<CalculatedGoldPrice> {
        val rate = if (currencyCode == CurrencyOption.USD.code || exchangeRate <= BigDecimal.ZERO) {
            BigDecimal.ONE
        } else {
            exchangeRate
        }
        val pure24Spot = ouncePriceUsd
            .multiply(rate)
            .divide(TROY_OUNCE_GRAMS, 4, RoundingMode.HALF_UP)

        fun makeEntry(
            id: String,
            nameAr: String,
            purity: BigDecimal,
            sellMargin: BigDecimal,
            buyMargin: BigDecimal,
            order: Int,
        ): CalculatedGoldPrice {
            val base = pure24Spot.multiply(purity)
            val sell = base.multiply(sellMargin).setScale(2, RoundingMode.HALF_UP)
            val buy = base.multiply(buyMargin).setScale(2, RoundingMode.HALF_UP)
            return CalculatedGoldPrice(
                id = id,
                nameAr = nameAr,
                purityRatio = purity,
                sellPricePerGram = sell,
                buyPricePerGram = buy,
                sortOrder = order,
            )
        }

        return listOf(
            makeEntry("k24", "ذهب عيار 24 (نقي)", BigDecimal("0.9999"), BigDecimal("1.015"), BigDecimal("0.985"), 1),
            makeEntry("k22", "ذهب عيار 22", BigDecimal("0.9167"), BigDecimal("1.020"), BigDecimal("0.980"), 2),
            makeEntry("k21", "ذهب عيار 21", BigDecimal("0.8750"), BigDecimal("1.025"), BigDecimal("0.975"), 3),
            makeEntry("k18", "ذهب عيار 18", BigDecimal("0.7500"), BigDecimal("1.030"), BigDecimal("0.970"), 4),
            makeEntry("raw", "ذهب خام / تبر", BigDecimal("0.9950"), BigDecimal("1.010"), BigDecimal("0.980"), 5),
            makeEntry("scrap21", "ذهب كسر عيار 21", BigDecimal("0.8750"), BigDecimal("1.005"), BigDecimal("0.960"), 6),
        )
    }

    fun defaultOfflinePrices(currencyCode: String = CurrencyOption.USD.code, exchangeRate: BigDecimal = BigDecimal.ONE): List<CalculatedGoldPrice> {
        // Sensible initial offline spot reference (~$2,660 / troy oz) ready immediately out-of-the-box
        return calculateSpotPricesFromOunce(BigDecimal("2660.00"), currencyCode, exchangeRate)
    }
}
