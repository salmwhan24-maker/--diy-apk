package com.example

import com.example.domain.AppBackupPayload
import com.example.domain.BackupGoldPriceDto
import com.example.domain.BackupInvoiceDto
import com.example.domain.BackupInvoiceItemDto
import com.example.domain.BackupSettingsDto
import com.example.domain.DraftInvoiceItem
import com.example.domain.MoneyMath
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ExampleUnitTest {
    @Test
    fun `arabic digits and decimal precision are parsed accurately`() {
        val parsed = MoneyMath.parseDecimal("١٢٫٣٧٥")
        assertEquals(BigDecimal("12.375"), parsed)
    }

    @Test
    fun `gold line and invoice totals have zero floating point rounding errors`() {
        val item1 = DraftInvoiceItem(
            localKey = 1L,
            categoryType = "مجوهرات",
            karatId = "k21",
            karatLabel = "ذهب عيار 21",
            description = "خاتم",
            weightInput = "12.375",
            pricePerGramInput = "76.43",
            workmanshipInput = "15.00",
        )
        // 12.375 * 76.43 = 945.82125 -> 945.82 + 15.00 = 960.82
        assertEquals(BigDecimal("945.82"), item1.goldValue)
        assertEquals(BigDecimal("960.82"), item1.lineTotal)

        val totals = MoneyMath.calculateInvoiceTotals(listOf(item1), BigDecimal("10.82"))
        assertEquals(BigDecimal("12.375"), totals.totalWeightGrams)
        assertEquals(BigDecimal("960.82"), totals.subtotal)
        assertEquals(BigDecimal("950.00"), totals.netTotal)
    }

    @Test
    fun `spot prices from troy ounce produce ordered karats`() {
        val prices = MoneyMath.calculateSpotPricesFromOunce(
            ouncePriceUsd = BigDecimal("2660.00"),
            currencyCode = "USD",
            exchangeRate = BigDecimal.ONE,
        )
        assertEquals(6, prices.size)
        val k24 = prices.first { it.id == "k24" }
        val k21 = prices.first { it.id == "k21" }
        assertTrue(k24.sellPricePerGram > k24.buyPricePerGram)
        assertTrue(k24.sellPricePerGram > k21.sellPricePerGram)
    }

    @Test
    fun `backup json payload serializes and deserializes cleanly`() {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(AppBackupPayload::class.java)
        val payload = AppBackupPayload(
            exportedAtMillis = 1700000000000L,
            settings = BackupSettingsDto("USD", 1700000000000L, "LOCAL_SAVED", 1002, 1001, "3.75", "535.00"),
            goldPrices = listOf(
                BackupGoldPriceDto("k21", "ذهب عيار 21", "0.8750", "77.00", "74.00", 3, 1700000000000L)
            ),
            invoices = listOf(
                BackupInvoiceDto(
                    invoiceNumber = "S-1001",
                    type = "SALE",
                    createdAtMillis = 1700000000000L,
                    partyName = "خالد",
                    partyPhone = "770000000",
                    notes = "",
                    currencyCode = "USD",
                    subtotalAmount = "770.00",
                    discountAmount = "0.00",
                    netTotalAmount = "770.00",
                    totalWeightGrams = "10.000",
                    items = listOf(
                        BackupInvoiceItemDto("مجوهرات", "k21", "ذهب عيار 21", "سوار", "10.000", "77.00", "0.00", "770.00")
                    )
                )
            )
        )
        val json = adapter.toJson(payload)
        assertTrue(json.contains("أبو إسحاق لبيع وشراء الذهب الخام والمجوهرات"))
        assertTrue(json.contains("779944094"))
        val restored = adapter.fromJson(json)
        assertNotNull(restored)
        assertEquals("S-1001", restored?.invoices?.first()?.invoiceNumber)
    }
}
