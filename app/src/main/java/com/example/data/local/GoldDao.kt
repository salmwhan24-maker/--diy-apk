package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GoldPriceDao {
    @Query("SELECT * FROM gold_prices ORDER BY sortOrder ASC")
    fun observeAllPrices(): Flow<List<GoldPriceEntity>>

    @Query("SELECT * FROM gold_prices ORDER BY sortOrder ASC")
    suspend fun getAllPricesOnce(): List<GoldPriceEntity>

    @Query("SELECT COUNT(*) FROM gold_prices")
    suspend fun countPrices(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(prices: List<GoldPriceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(price: GoldPriceEntity)

    @Query("DELETE FROM gold_prices")
    suspend fun clearAll()
}

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY createdAtMillis DESC, id DESC")
    fun observeAllInvoices(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY createdAtMillis DESC, id DESC")
    suspend fun getAllInvoicesOnce(): List<InvoiceWithItems>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeInvoiceById(id: Long): Flow<InvoiceWithItems?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceByIdOnce(id: Long): InvoiceWithItems?

    @Query("SELECT EXISTS(SELECT 1 FROM invoices WHERE invoiceNumber = :number)")
    suspend fun existsByInvoiceNumber(number: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Transaction
    suspend fun insertInvoiceWithItems(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
    ): Long {
        val invoiceId = insertInvoice(invoice)
        if (items.isNotEmpty()) {
            insertItems(items.map { it.copy(invoiceId = invoiceId) })
        }
        return invoiceId
    }

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long)

    @Query("DELETE FROM invoices")
    suspend fun clearAllInvoices()
}
