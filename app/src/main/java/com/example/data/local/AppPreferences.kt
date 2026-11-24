package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.CurrencyOption
import com.example.domain.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "abu_ishaq_prefs")

data class AppSettingsState(
    val activeCurrencyCode: String = CurrencyOption.USD.code,
    val lastSyncMillis: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_SAVED,
    val statusNote: String = "الأسعار المحفوظة محلياً جاهزة للاستخدام بدون إنترنت",
    val nextSaleSeq: Int = 1001,
    val nextPurchaseSeq: Int = 1001,
    val sarRate: String = "3.75",
    val yerRate: String = "535.00",
    val isDarkTheme: Boolean = true,
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val ACTIVE_CURRENCY = stringPreferencesKey("active_currency")
        val LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")
        val SYNC_STATUS = stringPreferencesKey("sync_status")
        val STATUS_NOTE = stringPreferencesKey("status_note")
        val NEXT_SALE_SEQ = intPreferencesKey("next_sale_seq")
        val NEXT_PURCHASE_SEQ = intPreferencesKey("next_purchase_seq")
        val SAR_RATE = stringPreferencesKey("sar_rate")
        val YER_RATE = stringPreferencesKey("yer_rate")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    val settingsFlow: Flow<AppSettingsState> = context.dataStore.data.map { prefs ->
        AppSettingsState(
            activeCurrencyCode = prefs[Keys.ACTIVE_CURRENCY] ?: CurrencyOption.USD.code,
            lastSyncMillis = prefs[Keys.LAST_SYNC_MILLIS] ?: 0L,
            syncStatus = SyncStatus.fromName(prefs[Keys.SYNC_STATUS] ?: SyncStatus.LOCAL_SAVED.name),
            statusNote = prefs[Keys.STATUS_NOTE]
                ?: "الأسعار المحفوظة محلياً جاهزة للاستخدام بدون إنترنت",
            nextSaleSeq = prefs[Keys.NEXT_SALE_SEQ] ?: 1001,
            nextPurchaseSeq = prefs[Keys.NEXT_PURCHASE_SEQ] ?: 1001,
            sarRate = prefs[Keys.SAR_RATE] ?: "3.75",
            yerRate = prefs[Keys.YER_RATE] ?: "535.00",
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
        )
    }

    suspend fun getSettingsOnce(): AppSettingsState = settingsFlow.first()

    suspend fun updateSyncMetadata(
        lastSyncMillis: Long,
        syncStatus: SyncStatus,
        statusNote: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_MILLIS] = lastSyncMillis
            prefs[Keys.SYNC_STATUS] = syncStatus.name
            prefs[Keys.STATUS_NOTE] = statusNote
        }
    }

    suspend fun setActiveCurrency(currencyCode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_CURRENCY] = currencyCode
        }
    }

    suspend fun updateExchangeRates(sarRate: String, yerRate: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAR_RATE] = sarRate
            prefs[Keys.YER_RATE] = yerRate
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DARK_THEME] = isDark
        }
    }

    suspend fun consumeNextSaleSeq(): Int {
        var used = 1001
        context.dataStore.edit { prefs ->
            used = prefs[Keys.NEXT_SALE_SEQ] ?: 1001
            prefs[Keys.NEXT_SALE_SEQ] = used + 1
        }
        return used
    }

    suspend fun consumeNextPurchaseSeq(): Int {
        var used = 1001
        context.dataStore.edit { prefs ->
            used = prefs[Keys.NEXT_PURCHASE_SEQ] ?: 1001
            prefs[Keys.NEXT_PURCHASE_SEQ] = used + 1
        }
        return used
    }

    suspend fun restoreSettings(
        activeCurrencyCode: String,
        lastSyncMillis: Long,
        syncStatus: String,
        nextSaleSeq: Int,
        nextPurchaseSeq: Int,
        sarRate: String,
        yerRate: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_CURRENCY] = activeCurrencyCode
            prefs[Keys.LAST_SYNC_MILLIS] = lastSyncMillis
            prefs[Keys.SYNC_STATUS] = syncStatus
            prefs[Keys.NEXT_SALE_SEQ] = nextSaleSeq.coerceAtLeast(1001)
            prefs[Keys.NEXT_PURCHASE_SEQ] = nextPurchaseSeq.coerceAtLeast(1001)
            prefs[Keys.SAR_RATE] = sarRate
            prefs[Keys.YER_RATE] = yerRate
            prefs[Keys.STATUS_NOTE] = "تمت استعادة البيانات والأسعار من النسخة الاحتياطية"
        }
    }
}
