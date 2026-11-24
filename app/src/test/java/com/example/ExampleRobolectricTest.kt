package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.remote.GoldPriceService
import com.example.data.repository.BackupImportResult
import com.example.data.repository.GoldRepository
import com.example.domain.DraftInvoiceItem
import com.example.domain.InvoiceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: GoldRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GoldRepository(
            db = db,
            prefs = AppPreferences(context),
            service = GoldPriceService(context),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app strings match official brand identity`() {
        assertEquals("ذهب أبو إسحاق", context.getString(R.string.app_name))
        assertEquals("أبو إسحاق لبيع وشراء الذهب الخام والمجوهرات", context.getString(R.string.brand_full_title))
        assertEquals("779944094", context.getString(R.string.brand_phone))
        assertEquals("من صنع وتطوير م/سالم وهان", context.getString(R.string.developer_credit))
    }

    @Test
    fun `offline seeding, invoice saving, and json backup roundtrip work completely offline`() = runTest {
        repository.ensureSeeded()
        val seededPrices = repository.pricesFlow.first()
        assertEquals(6, seededPrices.size)

        val item = DraftInvoiceItem(
            localKey = 1L,
            categoryType = "ذهب خام",
            karatId = "raw",
            karatLabel = "ذهب خام / تبر",
            description = "سبيكة",
            weightInput = "25.500",
            pricePerGramInput = "85.00",
            workmanshipInput = "0",
        )
        val savedId = repository.createAndSaveInvoice(
            type = InvoiceType.PURCHASE,
            partyName = "أحمد علي",
            partyPhone = "770000000",
            notes = "شراء فوري",
            currencyCode = "USD",
            items = listOf(item),
            discount = BigDecimal.ZERO,
        )
        assertTrue(savedId > 0L)

        val invoices = repository.invoicesFlow.first()
        assertEquals(1, invoices.size)
        assertEquals("2167.50", invoices.first().invoice.netTotalAmount)

        val backupJson = repository.exportBackupJson()
        assertTrue(backupJson.contains("أبو إسحاق لبيع وشراء الذهب الخام والمجوهرات"))
        assertTrue(backupJson.contains("779944094"))

        repository.deleteInvoice(savedId)
        assertEquals(0, repository.invoicesFlow.first().size)

        val importResult = repository.importBackupJson(backupJson, replaceExisting = true)
        assertTrue(importResult is BackupImportResult.Success)
        assertEquals(1, repository.invoicesFlow.first().size)
    }
}
