package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsState
import com.example.data.local.GoldPriceEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.repository.BackupImportResult
import com.example.data.repository.GoldRepository
import com.example.data.repository.SyncActionResult
import com.example.domain.DraftInvoiceItem
import com.example.domain.InvoiceType
import com.example.domain.MoneyMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class InvoiceFilterType(val labelAr: String) {
    ALL("الكل"),
    SALE("فواتير البيع"),
    PURCHASE("فواتير الشراء"),
}

enum class DateFilterRange(val labelAr: String) {
    ALL("كل التواريخ"),
    TODAY("اليوم"),
    LAST_7_DAYS("آخر 7 أيام"),
    THIS_MONTH("هذا الشهر"),
}

data class DraftPrefill(
    val type: InvoiceType,
    val categoryType: String,
    val karatId: String,
    val karatLabel: String,
    val weightInput: String,
    val pricePerGramInput: String,
    val workmanshipInput: String,
)

data class FeedbackBanner(
    val message: String,
    val isWarningOrOffline: Boolean,
)

class GoldShopViewModel(
    private val repository: GoldRepository,
) : ViewModel() {

    val prices: StateFlow<List<GoldPriceEntity>> = repository.pricesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettingsState> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsState())

    val allInvoices: StateFlow<List<InvoiceWithItems>> = repository.invoicesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _feedback = MutableStateFlow<FeedbackBanner?>(null)
    val feedback: StateFlow<FeedbackBanner?> = _feedback.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(InvoiceFilterType.ALL)
    val filterType: StateFlow<InvoiceFilterType> = _filterType.asStateFlow()

    private val _dateRange = MutableStateFlow(DateFilterRange.ALL)
    val dateRange: StateFlow<DateFilterRange> = _dateRange.asStateFlow()

    private val _pendingPrefill = MutableStateFlow<DraftPrefill?>(null)
    val pendingPrefill: StateFlow<DraftPrefill?> = _pendingPrefill.asStateFlow()

    val filteredInvoices: StateFlow<List<InvoiceWithItems>> = combine(
        allInvoices,
        _searchQuery,
        _filterType,
        _dateRange,
    ) { invoices, query, typeFilter, range ->
        val q = MoneyMath.normalizeDigits(query).lowercase()
        val now = System.currentTimeMillis()
        val startOfToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val sevenDaysAgo = now - 7L * 24L * 60L * 60L * 1000L
        val thirtyDaysAgo = now - 30L * 24L * 60L * 60L * 1000L

        invoices.filter { entry ->
            val inv = entry.invoice
            val matchesType = when (typeFilter) {
                InvoiceFilterType.ALL -> true
                InvoiceFilterType.SALE -> inv.type == InvoiceType.SALE.name
                InvoiceFilterType.PURCHASE -> inv.type == InvoiceType.PURCHASE.name
            }
            val matchesRange = when (range) {
                DateFilterRange.ALL -> true
                DateFilterRange.TODAY -> inv.createdAtMillis >= startOfToday
                DateFilterRange.LAST_7_DAYS -> inv.createdAtMillis >= sevenDaysAgo
                DateFilterRange.THIS_MONTH -> inv.createdAtMillis >= thirtyDaysAgo
            }
            val matchesQuery = if (q.isBlank()) {
                true
            } else {
                inv.invoiceNumber.lowercase().contains(q) ||
                    inv.partyName.lowercase().contains(q) ||
                    inv.partyPhone.lowercase().contains(q) ||
                    inv.notes.lowercase().contains(q) ||
                    entry.items.any { item ->
                        item.categoryType.lowercase().contains(q) ||
                            item.karatLabel.lowercase().contains(q) ||
                            item.description.lowercase().contains(q)
                    }
            }
            matchesType && matchesRange && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun observeInvoice(id: Long): Flow<InvoiceWithItems?> = repository.observeInvoice(id)

    fun refreshPricesOnline() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            when (val result = repository.refreshPricesOnline()) {
                is SyncActionResult.Success -> {
                    _feedback.value = FeedbackBanner(
                        message = result.messageAr,
                        isWarningOrOffline = false,
                    )
                }
                is SyncActionResult.OfflineFallback -> {
                    _feedback.value = FeedbackBanner(
                        message = result.messageAr,
                        isWarningOrOffline = true,
                    )
                }
            }
            _isSyncing.value = false
        }
    }

    fun dismissFeedback() {
        _feedback.value = null
    }

    fun showFeedback(message: String, isWarning: Boolean = false) {
        _feedback.value = FeedbackBanner(message, isWarning)
    }

    fun updateSinglePrice(id: String, sellInput: String, buyInput: String) {
        val sell = MoneyMath.parseDecimal(sellInput)
        val buy = MoneyMath.parseDecimal(buyInput)
        if (sell <= BigDecimal.ZERO || buy <= BigDecimal.ZERO) {
            _feedback.value = FeedbackBanner("يرجى إدخال أسعار صالحة أكبر من صفر", true)
            return
        }
        viewModelScope.launch {
            repository.updateSinglePriceManually(id, sell, buy)
            _feedback.value = FeedbackBanner("تم حفظ تعديل السعر محلياً بنجاح", false)
        }
    }

    fun changeCurrency(currencyCode: String) {
        viewModelScope.launch {
            repository.changeCurrency(currencyCode)
            _feedback.value = FeedbackBanner("تم تحديث العملة النشطة وتحويل الأسعار المحفوظة", false)
        }
    }

    fun updateExchangeRates(sarRate: String, yerRate: String) {
        viewModelScope.launch {
            repository.updateExchangeRates(sarRate, yerRate)
            _feedback.value = FeedbackBanner("تم حفظ أسعار الصرف المرجعية محلياً", false)
        }
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(isDark)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(filter: InvoiceFilterType) {
        _filterType.value = filter
    }

    fun setDateRange(range: DateFilterRange) {
        _dateRange.value = range
    }

    fun setPrefill(prefill: DraftPrefill) {
        _pendingPrefill.value = prefill
    }

    fun consumePrefill(): DraftPrefill? {
        val current = _pendingPrefill.value
        _pendingPrefill.value = null
        return current
    }

    fun saveInvoice(
        type: InvoiceType,
        partyName: String,
        partyPhone: String,
        notes: String,
        currencyCode: String,
        items: List<DraftInvoiceItem>,
        discount: BigDecimal,
        onSaved: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val id = repository.createAndSaveInvoice(
                type = type,
                partyName = partyName,
                partyPhone = partyPhone,
                notes = notes,
                currencyCode = currencyCode,
                items = items,
                discount = discount,
            )
            _feedback.value = FeedbackBanner("تم حفظ ${type.labelAr} محلياً بنجاح", false)
            onSaved(id)
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
            _feedback.value = FeedbackBanner("تم حذف الفاتورة من السجل المحلي", false)
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(rawJson: String, replaceExisting: Boolean) {
        viewModelScope.launch {
            when (val res = repository.importBackupJson(rawJson, replaceExisting)) {
                is BackupImportResult.Success -> {
                    _feedback.value = FeedbackBanner(
                        message = "تمت استعادة ${res.invoicesCount} فاتورة و ${res.pricesCount} سعر ذهب بنجاح.",
                        isWarningOrOffline = false,
                    )
                }
                is BackupImportResult.Error -> {
                    _feedback.value = FeedbackBanner(
                        message = res.messageAr,
                        isWarningOrOffline = true,
                    )
                }
            }
        }
    }

    class Factory(private val repository: GoldRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoldShopViewModel(repository) as T
        }
    }
}
