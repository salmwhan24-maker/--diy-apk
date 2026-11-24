package com.example.domain

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupGoldPriceDto(
    val id: String,
    val nameAr: String,
    val purityRatio: String,
    val sellPricePerGram: String,
    val buyPricePerGram: String,
    val sortOrder: Int,
    val updatedAtMillis: Long,
)

@JsonClass(generateAdapter = true)
data class BackupInvoiceItemDto(
    val categoryType: String,
    val karatId: String,
    val karatLabel: String,
    val description: String,
    val weightGrams: String,
    val pricePerGram: String,
    val workmanshipAmount: String,
    val lineTotalAmount: String,
)

@JsonClass(generateAdapter = true)
data class BackupInvoiceDto(
    val invoiceNumber: String,
    val type: String,
    val createdAtMillis: Long,
    val partyName: String,
    val partyPhone: String,
    val notes: String,
    val currencyCode: String,
    val subtotalAmount: String,
    val discountAmount: String,
    val netTotalAmount: String,
    val totalWeightGrams: String,
    val items: List<BackupInvoiceItemDto>,
)

@JsonClass(generateAdapter = true)
data class BackupSettingsDto(
    val activeCurrencyCode: String,
    val lastSyncMillis: Long,
    val syncStatus: String,
    val nextSaleSeq: Int,
    val nextPurchaseSeq: Int,
    val sarRate: String,
    val yerRate: String,
)

@JsonClass(generateAdapter = true)
data class AppBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAtMillis: Long,
    val shopTitle: String = "أبو إسحاق لبيع وشراء الذهب الخام والمجوهرات",
    val shopPhone: String = "779944094",
    val settings: BackupSettingsDto,
    val goldPrices: List<BackupGoldPriceDto>,
    val invoices: List<BackupInvoiceDto>,
)
