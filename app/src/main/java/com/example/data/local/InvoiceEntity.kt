package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["invoiceNumber"], unique = true)]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val type: String, // SALE or PURCHASE
    val createdAtMillis: Long,
    val partyName: String,
    val partyPhone: String,
    val notes: String,
    val currencyCode: String,
    val subtotalAmount: String,
    val discountAmount: String,
    val netTotalAmount: String,
    val totalWeightGrams: String,
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val categoryType: String,
    val karatId: String,
    val karatLabel: String,
    val description: String,
    val weightGrams: String,
    val pricePerGram: String,
    val workmanshipAmount: String,
    val lineTotalAmount: String,
)

data class InvoiceWithItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId",
    )
    val items: List<InvoiceItemEntity>,
)
