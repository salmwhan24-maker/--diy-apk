package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gold_prices")
data class GoldPriceEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val purityRatio: String,
    val sellPricePerGram: String,
    val buyPricePerGram: String,
    val sortOrder: Int,
    val updatedAtMillis: Long,
)
