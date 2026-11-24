package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GoldApiPrimaryResponse(
    val price: Double? = null,
    val symbol: String? = null,
)

@JsonClass(generateAdapter = true)
data class GoldPriceOrgItem(
    val xauPrice: Double? = null,
)

@JsonClass(generateAdapter = true)
data class GoldPriceOrgResponse(
    val items: List<GoldPriceOrgItem>? = null,
)

data class GoldSpotQuote(
    val ouncePriceUsd: BigDecimal,
    val sourceLabel: String,
)

sealed class OnlineFetchResult {
    data class Success(val quote: GoldSpotQuote) : OnlineFetchResult()
    data class OfflineOrError(val messageAr: String) : OnlineFetchResult()
}

class GoldPriceService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().build()

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun fetchLiveSpotPriceUsd(): OnlineFetchResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext OnlineFetchResult.OfflineOrError(
                "لا يوجد اتصال بالإنترنت حالياً. تم الإبقاء على آخر أسعار محفوظة محلياً للاستخدام الفوري."
            )
        }

        // Primary endpoint: api.gold-api.com
        val primary = tryFetchPrimary()
        if (primary != null) {
            return@withContext OnlineFetchResult.Success(primary)
        }

        // Secondary endpoint: data-asg.goldprice.org
        val secondary = tryFetchSecondary()
        if (secondary != null) {
            return@withContext OnlineFetchResult.Success(secondary)
        }

        OnlineFetchResult.OfflineOrError(
            "تعذر الوصول إلى خادم أسعار الذهب عبر الإنترنت. تم الإبقاء على النسخة المحلية المحفوظة دون أي تغيير."
        )
    }

    private fun tryFetchPrimary(): GoldSpotQuote? {
        return try {
            val request = Request.Builder()
                .url("https://api.gold-api.com/price/XAU")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val adapter = moshi.adapter(GoldApiPrimaryResponse::class.java)
                val parsed = adapter.fromJson(body)
                val price = parsed?.price
                if (price != null && price > 100.0) {
                    GoldSpotQuote(
                        ouncePriceUsd = BigDecimal.valueOf(price),
                        sourceLabel = "سعر الأونصة العالمي المباشر"
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun tryFetchSecondary(): GoldSpotQuote? {
        return try {
            val request = Request.Builder()
                .url("https://data-asg.goldprice.org/dbXRates/USD")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val adapter = moshi.adapter(GoldPriceOrgResponse::class.java)
                val parsed = adapter.fromJson(body)
                val price = parsed?.items?.firstOrNull()?.xauPrice
                if (price != null && price > 100.0) {
                    GoldSpotQuote(
                        ouncePriceUsd = BigDecimal.valueOf(price),
                        sourceLabel = "سعر الأونصة العالمي المباشر"
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
