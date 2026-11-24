package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.local.AppSettingsState
import com.example.data.local.GoldPriceEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.remote.GoldPriceService
import com.example.data.remote.OnlineFetchResult
import com.example.domain.AppBackupPayload
import com.example.domain.BackupGoldPriceDto
import com.example.domain.BackupInvoiceDto
import com.example.domain.BackupInvoiceItemDto
import com.example.domain.BackupSettingsDto
import com.example.domain.CurrencyOption
import com.example.domain.DraftInvoiceItem
import com.example.domain.InvoiceType
import com.example.domain.MoneyMath
import com.example.domain.SyncStatus
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

sealed class SyncActionResult {
    data class Success(val messageAr: String, val ouncePriceUsd: BigDecimal) : SyncActionResult()
    data class OfflineFallback(val messageAr: String) : SyncActionResult()
}

sealed class BackupImportResult {
    data class Success(val invoicesCount: Int, val pricesCount: Int) : BackupImportResult()
    data class Error(val messageAr: String) : BackupImportResult()
}

class GoldRepository(
    private val db: AppDatabase,
    private val prefs: AppPreferences,
    private val service: GoldPriceService,
) {
    private val goldPriceDao = db.goldPriceDao()
    private val invoiceDao = db.invoiceDao()
    private val moshi = Moshi.Builder().build()

    val pricesFlow: Flow<List<GoldPriceEntity>> = goldPriceDao.observeAllPrices()
    val invoicesFlow: Flow<List<InvoiceWithItems>> = invoiceDao.observeAllInvoices()
    val settingsFlow: Flow<AppSettingsState> = prefs.settingsFlow

    fun observeInvoice(id: Long): Flow<InvoiceWithItems?> = invoiceDao.observeInvoiceById(id)

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (goldPriceDao.countPrices() == 0) {
            val settings = prefs.getSettingsOnce()
            val now = System.currentTimeMillis()
            val rate = getExchangeRateFor(settings.activeCurrencyCode, settings)
            val defaults = MoneyMath.defaultOfflinePrices(settings.activeCurrencyCode, rate)
                .map { calc ->
                    GoldPriceEntity(
                        id = calc.id,
                        nameAr = calc.nameAr,
                        purityRatio = calc.purityRatio.toPlainString(),
                        sellPricePerGram = MoneyMath.formatPlainDecimal(calc.sellPricePerGram),
                        buyPricePerGram = MoneyMath.formatPlainDecimal(calc.buyPricePerGram),
                        sortOrder = calc.sortOrder,
                        updatedAtMillis = now,
                    )
                }
            goldPriceDao.upsertAll(defaults)
            if (settings.lastSyncMillis == 0L) {
                prefs.updateSyncMetadata(
                    lastSyncMillis = now,
                    syncStatus = SyncStatus.LOCAL_SAVED,
                    statusNote = "الأسعار الافتراضية محفوظة محلياً وجاهزة للعمل بدون إنترنت",
                )
            }
        }
    }

    suspend fun refreshPricesOnline(): SyncActionResult = withContext(Dispatchers.IO) {
        ensureSeeded()
        when (val result = service.fetchLiveSpotPriceUsd()) {
            is OnlineFetchResult.Success -> {
                val settings = prefs.getSettingsOnce()
                val rate = getExchangeRateFor(settings.activeCurrencyCode, settings)
                val now = System.currentTimeMillis()
                val updated = MoneyMath.calculateSpotPricesFromOunce(
                    ouncePriceUsd = result.quote.ouncePriceUsd,
                    currencyCode = settings.activeCurrencyCode,
                    exchangeRate = rate,
                ).map { calc ->
                    GoldPriceEntity(
                        id = calc.id,
                        nameAr = calc.nameAr,
                        purityRatio = calc.purityRatio.toPlainString(),
                        sellPricePerGram = MoneyMath.formatPlainDecimal(calc.sellPricePerGram),
                        buyPricePerGram = MoneyMath.formatPlainDecimal(calc.buyPricePerGram),
                        sortOrder = calc.sortOrder,
                        updatedAtMillis = now,
                    )
                }
                goldPriceDao.upsertAll(updated)
                val formattedOz = MoneyMath.formatAmount(result.quote.ouncePriceUsd)
                prefs.updateSyncMetadata(
                    lastSyncMillis = now,
                    syncStatus = SyncStatus.ONLINE_UPDATED,
                    statusNote = "تم التحديث عبر الإنترنت (سعر الأونصة: $formattedOz $)",
                )
                SyncActionResult.Success(
                    messageAr = "تم تحديث أسعار الذهب عبر الإنترنت وحفظها محلياً بنجاح.",
                    ouncePriceUsd = result.quote.ouncePriceUsd,
                )
            }

            is OnlineFetchResult.OfflineOrError -> {
                val existing = prefs.getSettingsOnce()
                val lastTs = if (existing.lastSyncMillis > 0L) existing.lastSyncMillis else System.currentTimeMillis()
                prefs.updateSyncMetadata(
                    lastSyncMillis = lastTs,
                    syncStatus = SyncStatus.LOCAL_SAVED,
                    statusNote = result.messageAr,
                )
                SyncActionResult.OfflineFallback(result.messageAr)
            }
        }
    }

    suspend fun updateSinglePriceManually(
        id: String,
        sellPrice: BigDecimal,
        buyPrice: BigDecimal,
    ) = withContext(Dispatchers.IO) {
        val current = goldPriceDao.getAllPricesOnce().firstOrNull { it.id == id } ?: return@withContext
        val now = System.currentTimeMillis()
        goldPriceDao.upsert(
            current.copy(
                sellPricePerGram = MoneyMath.formatPlainDecimal(sellPrice),
                buyPricePerGram = MoneyMath.formatPlainDecimal(buyPrice),
                updatedAtMillis = now,
            )
        )
        prefs.updateSyncMetadata(
            lastSyncMillis = now,
            syncStatus = SyncStatus.MANUAL_EDITED,
            statusNote = "تم تعديل ${current.nameAr} يدوياً وحفظه محلياً",
        )
    }

    suspend fun changeCurrency(newCurrencyCode: String) = withContext(Dispatchers.IO) {
        val settings = prefs.getSettingsOnce()
        val oldCode = settings.activeCurrencyCode
        if (oldCode == newCurrencyCode) return@withContext

        val oldRate = getExchangeRateFor(oldCode, settings)
        val newRate = getExchangeRateFor(newCurrencyCode, settings)
        val factor = newRate.divide(oldRate, 8, RoundingMode.HALF_UP)

        val now = System.currentTimeMillis()
        val converted = goldPriceDao.getAllPricesOnce().map { entity ->
            val oldSell = MoneyMath.parseDecimal(entity.sellPricePerGram)
            val oldBuy = MoneyMath.parseDecimal(entity.buyPricePerGram)
            entity.copy(
                sellPricePerGram = MoneyMath.formatPlainDecimal(oldSell.multiply(factor)),
                buyPricePerGram = MoneyMath.formatPlainDecimal(oldBuy.multiply(factor)),
                updatedAtMillis = now,
            )
        }
        if (converted.isNotEmpty()) {
            goldPriceDao.upsertAll(converted)
        }
        prefs.setActiveCurrency(newCurrencyCode)
    }

    suspend fun updateExchangeRates(sarRateInput: String, yerRateInput: String) {
        val sar = MoneyMath.parseDecimal(sarRateInput)
        val yer = MoneyMath.parseDecimal(yerRateInput)
        if (sar > BigDecimal.ZERO && yer > BigDecimal.ZERO) {
            prefs.updateExchangeRates(
                sarRate = MoneyMath.formatPlainDecimal(sar),
                yerRate = MoneyMath.formatPlainDecimal(yer),
            )
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        prefs.setDarkTheme(isDark)
    }

    suspend fun createAndSaveInvoice(
        type: InvoiceType,
        partyName: String,
        partyPhone: String,
        notes: String,
        currencyCode: String,
        items: List<DraftInvoiceItem>,
        discount: BigDecimal,
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val prefix = if (type == InvoiceType.SALE) "S" else "P"
        var seq = if (type == InvoiceType.SALE) {
            prefs.consumeNextSaleSeq()
        } else {
            prefs.consumeNextPurchaseSeq()
        }
        var invoiceNumber = "$prefix-$seq"
        while (invoiceDao.existsByInvoiceNumber(invoiceNumber)) {
            seq++
            invoiceNumber = "$prefix-$seq"
        }

        val totals = MoneyMath.calculateInvoiceTotals(items, discount)
        val invoiceEntity = InvoiceEntity(
            invoiceNumber = invoiceNumber,
            type = type.name,
            createdAtMillis = now,
            partyName = partyName.trim(),
            partyPhone = partyPhone.trim(),
            notes = notes.trim(),
            currencyCode = currencyCode,
            subtotalAmount = MoneyMath.formatPlainDecimal(totals.subtotal),
            discountAmount = MoneyMath.formatPlainDecimal(totals.discount),
            netTotalAmount = MoneyMath.formatPlainDecimal(totals.netTotal),
            totalWeightGrams = MoneyMath.formatPlainDecimal(totals.totalWeightGrams, scale = 3),
        )

        val itemEntities = items.map { draft ->
            InvoiceItemEntity(
                invoiceId = 0L,
                categoryType = draft.categoryType,
                karatId = draft.karatId,
                karatLabel = draft.karatLabel,
                description = draft.description.trim(),
                weightGrams = MoneyMath.formatPlainDecimal(draft.weightGrams, scale = 3),
                pricePerGram = MoneyMath.formatPlainDecimal(draft.pricePerGram),
                workmanshipAmount = MoneyMath.formatPlainDecimal(draft.workmanship),
                lineTotalAmount = MoneyMath.formatPlainDecimal(draft.lineTotal),
            )
        }

        invoiceDao.insertInvoiceWithItems(invoiceEntity, itemEntities)
    }

    suspend fun deleteInvoice(id: Long) = withContext(Dispatchers.IO) {
        invoiceDao.deleteInvoiceById(id)
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        ensureSeeded()
        val settings = prefs.getSettingsOnce()
        val prices = goldPriceDao.getAllPricesOnce()
        val invoices = invoiceDao.getAllInvoicesOnce()

        val payload = AppBackupPayload(
            schemaVersion = 1,
            exportedAtMillis = System.currentTimeMillis(),
            settings = BackupSettingsDto(
                activeCurrencyCode = settings.activeCurrencyCode,
                lastSyncMillis = settings.lastSyncMillis,
                syncStatus = settings.syncStatus.name,
                nextSaleSeq = settings.nextSaleSeq,
                nextPurchaseSeq = settings.nextPurchaseSeq,
                sarRate = settings.sarRate,
                yerRate = settings.yerRate,
            ),
            goldPrices = prices.map {
                BackupGoldPriceDto(
                    id = it.id,
                    nameAr = it.nameAr,
                    purityRatio = it.purityRatio,
                    sellPricePerGram = it.sellPricePerGram,
                    buyPricePerGram = it.buyPricePerGram,
                    sortOrder = it.sortOrder,
                    updatedAtMillis = it.updatedAtMillis,
                )
            },
            invoices = invoices.map { inv ->
                BackupInvoiceDto(
                    invoiceNumber = inv.invoice.invoiceNumber,
                    type = inv.invoice.type,
                    createdAtMillis = inv.invoice.createdAtMillis,
                    partyName = inv.invoice.partyName,
                    partyPhone = inv.invoice.partyPhone,
                    notes = inv.invoice.notes,
                    currencyCode = inv.invoice.currencyCode,
                    subtotalAmount = inv.invoice.subtotalAmount,
                    discountAmount = inv.invoice.discountAmount,
                    netTotalAmount = inv.invoice.netTotalAmount,
                    totalWeightGrams = inv.invoice.totalWeightGrams,
                    items = inv.items.map { item ->
                        BackupInvoiceItemDto(
                            categoryType = item.categoryType,
                            karatId = item.karatId,
                            karatLabel = item.karatLabel,
                            description = item.description,
                            weightGrams = item.weightGrams,
                            pricePerGram = item.pricePerGram,
                            workmanshipAmount = item.workmanshipAmount,
                            lineTotalAmount = item.lineTotalAmount,
                        )
                    },
                )
            },
        )
        moshi.adapter(AppBackupPayload::class.java).indent("  ").toJson(payload)
    }

    suspend fun importBackupJson(
        rawJson: String,
        replaceExisting: Boolean,
    ): BackupImportResult = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(AppBackupPayload::class.java)
            val payload = adapter.fromJson(rawJson)
                ?: return@withContext BackupImportResult.Error("ملف النسخة الاحتياطية فارغ أو غير صالح.")

            if (replaceExisting) {
                invoiceDao.clearAllInvoices()
                goldPriceDao.clearAll()
            }

            if (payload.goldPrices.isNotEmpty()) {
                val priceEntities = payload.goldPrices.map { dto ->
                    GoldPriceEntity(
                        id = dto.id,
                        nameAr = dto.nameAr,
                        purityRatio = dto.purityRatio,
                        sellPricePerGram = dto.sellPricePerGram,
                        buyPricePerGram = dto.buyPricePerGram,
                        sortOrder = dto.sortOrder,
                        updatedAtMillis = dto.updatedAtMillis,
                    )
                }
                goldPriceDao.upsertAll(priceEntities)
            }

            var importedInvoicesCount = 0
            for (dto in payload.invoices) {
                if (!replaceExisting && invoiceDao.existsByInvoiceNumber(dto.invoiceNumber)) {
                    continue
                }
                val invoiceEntity = InvoiceEntity(
                    invoiceNumber = dto.invoiceNumber,
                    type = dto.type,
                    createdAtMillis = dto.createdAtMillis,
                    partyName = dto.partyName,
                    partyPhone = dto.partyPhone,
                    notes = dto.notes,
                    currencyCode = dto.currencyCode,
                    subtotalAmount = dto.subtotalAmount,
                    discountAmount = dto.discountAmount,
                    netTotalAmount = dto.netTotalAmount,
                    totalWeightGrams = dto.totalWeightGrams,
                )
                val items = dto.items.map { item ->
                    InvoiceItemEntity(
                        invoiceId = 0L,
                        categoryType = item.categoryType,
                        karatId = item.karatId,
                        karatLabel = item.karatLabel,
                        description = item.description,
                        weightGrams = item.weightGrams,
                        pricePerGram = item.pricePerGram,
                        workmanshipAmount = item.workmanshipAmount,
                        lineTotalAmount = item.lineTotalAmount,
                    )
                }
                invoiceDao.insertInvoiceWithItems(invoiceEntity, items)
                importedInvoicesCount++
            }

            prefs.restoreSettings(
                activeCurrencyCode = payload.settings.activeCurrencyCode,
                lastSyncMillis = payload.settings.lastSyncMillis,
                syncStatus = payload.settings.syncStatus,
                nextSaleSeq = payload.settings.nextSaleSeq,
                nextPurchaseSeq = payload.settings.nextPurchaseSeq,
                sarRate = payload.settings.sarRate,
                yerRate = payload.settings.yerRate,
            )

            BackupImportResult.Success(
                invoicesCount = importedInvoicesCount,
                pricesCount = payload.goldPrices.size,
            )
        } catch (e: Exception) {
            BackupImportResult.Error("تعذر قراءة ملف JSON: تأكد من اختيار ملف نسخة احتياطية صحيح.")
        }
    }

    private fun getExchangeRateFor(currencyCode: String, settings: AppSettingsState): BigDecimal {
        return when (CurrencyOption.fromCode(currencyCode)) {
            CurrencyOption.USD -> BigDecimal.ONE
            CurrencyOption.SAR -> MoneyMath.parseDecimal(settings.sarRate).let {
                if (it <= BigDecimal.ZERO) BigDecimal("3.75") else it
            }
            CurrencyOption.YER -> MoneyMath.parseDecimal(settings.yerRate).let {
                if (it <= BigDecimal.ZERO) BigDecimal("535.00") else it
            }
        }
    }
}
