package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppSettingsState
import com.example.data.local.GoldPriceEntity
import com.example.data.local.InvoiceWithItems
import com.example.domain.InvoiceType
import com.example.domain.MoneyMath
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.PurchaseAmber
import com.example.ui.theme.SaleEmerald
import java.math.BigDecimal
import java.util.Calendar

@Composable
fun DashboardScreen(
    prices: List<GoldPriceEntity>,
    settings: AppSettingsState,
    invoices: List<InvoiceWithItems>,
    isSyncing: Boolean,
    onRefreshOnline: () -> Unit,
    onNewSaleInvoice: () -> Unit,
    onNewPurchaseInvoice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPricesAndCalc: () -> Unit,
    onOpenMenuAndAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayStats = remember(invoices, startOfToday) {
        val today = invoices.filter { it.invoice.createdAtMillis >= startOfToday }
        var salesTotal = BigDecimal.ZERO
        var purchasesTotal = BigDecimal.ZERO
        today.forEach { entry ->
            val net = MoneyMath.parseDecimal(entry.invoice.netTotalAmount)
            if (entry.invoice.type == InvoiceType.SALE.name) {
                salesTotal = salesTotal.add(net)
            } else {
                purchasesTotal = purchasesTotal.add(net)
            }
        }
        Triple(today.size, salesTotal, purchasesTotal)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero Editorial Identity Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1510)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(164.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_gold_banner),
                            contentDescription = "أبو إسحاق لبيع وشراء الذهب الخام والمجوهرات",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xE614110D),
                                            Color(0x9914110D),
                                            Color(0xD914110D),
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "أهلاً بكم في",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldBright,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "أبو إسحاق لبيع وشراء الذهب الخام والمجوهرات",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFFFDF8ED),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                HeroStatChip(
                                    label = "فواتير اليوم",
                                    value = "${todayStats.first}",
                                    modifier = Modifier.weight(1f),
                                )
                                HeroStatChip(
                                    label = "مبيعات اليوم",
                                    value = MoneyMath.formatMoney(todayStats.second, settings.activeCurrencyCode),
                                    modifier = Modifier.weight(1.3f),
                                )
                                HeroStatChip(
                                    label = "مشتريات اليوم",
                                    value = MoneyMath.formatMoney(todayStats.third, settings.activeCurrencyCode),
                                    modifier = Modifier.weight(1.3f),
                                )
                            }
                        }
                    }
                }
            }

            // Daily Gold Prices Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "أسعار الذهب اليومية",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            TextButton(
                                onClick = onOpenPricesAndCalc,
                                modifier = Modifier.testTag("dashboard_prices_details_btn"),
                            ) {
                                Text(
                                    text = "عرض الكل والحاسبة",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        SyncStatusBadge(syncStatus = settings.syncStatus)

                        Text(
                            text = if (settings.lastSyncMillis > 0L) {
                                "آخر تحديث: ${MoneyMath.formatDateTime(settings.lastSyncMillis)}"
                            } else {
                                "جاهز من النسخة المحلية المحفوظة"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Key Gold Prices Preview Table
                        val highlightedIds = listOf("k24", "k21", "k18", "raw")
                        val displayPrices = prices.filter { it.id in highlightedIds }
                            .ifEmpty { prices.take(4) }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "الصنف / العيار",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1.4f),
                                    )
                                    Text(
                                        text = "سعر البيع",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SaleEmerald,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = "سعر الشراء",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PurchaseAmber,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                displayPrices.forEachIndexed { idx, price ->
                                    if (idx > 0) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(vertical = 8.dp),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = price.nameAr,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1.4f),
                                        )
                                        Text(
                                            text = MoneyMath.formatMoney(
                                                MoneyMath.parseDecimal(price.sellPricePerGram),
                                                settings.activeCurrencyCode,
                                            ),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = MoneyMath.formatMoney(
                                                MoneyMath.parseDecimal(price.buyPricePerGram),
                                                settings.activeCurrencyCode,
                                            ),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }

                        // Sync Online CTA Button
                        Button(
                            onClick = onRefreshOnline,
                            enabled = !isSyncing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .height(50.dp)
                                .testTag("refresh_online_prices_btn"),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("جاري جلب الأسعار عبر الإنترنت...")
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تحديث الأسعار عبر الإنترنت",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Section
            item {
                Text(
                    text = "الوصول السريع",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PrimaryInvoiceActionCard(
                        title = "فاتورة بيع جديدة",
                        subtitle = "بيع ذهب خام ومجوهرات",
                        icon = Icons.Filled.PointOfSale,
                        accentColor = SaleEmerald,
                        onClick = onNewSaleInvoice,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_new_sale_btn"),
                    )
                    PrimaryInvoiceActionCard(
                        title = "فاتورة شراء جديدة",
                        subtitle = "شراء ذهب كسر وخام",
                        icon = Icons.Filled.AddShoppingCart,
                        accentColor = PurchaseAmber,
                        onClick = onNewPurchaseInvoice,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_new_purchase_btn"),
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryActionRow(
                        title = "سجل الفواتير",
                        subtitle = "عرض وبحث وطباعة ${invoices.size} فاتورة محفوظة محلياً",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        onClick = onOpenHistory,
                        modifier = Modifier.testTag("quick_history_btn"),
                    )
                    SecondaryActionRow(
                        title = "تفاصيل الذهب والأسعار والحاسبة",
                        subtitle = "أسعار العيارات والذهب الخام وتعديل الأسعار وحاسبة الجرامات",
                        icon = Icons.Filled.Calculate,
                        onClick = onOpenPricesAndCalc,
                        modifier = Modifier.testTag("quick_prices_calc_btn"),
                    )
                    SecondaryActionRow(
                        title = "القائمة والنسخ الاحتياطي وحول التطبيق",
                        subtitle = "تصدير واستيراد JSON محلي • م/سالم وهان",
                        icon = Icons.Filled.Menu,
                        onClick = onOpenMenuAndAbout,
                        modifier = Modifier.testTag("quick_menu_about_btn"),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC1B1712),
        border = BorderStroke(0.8.dp, GoldDeep.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD1C4AA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = GoldBright,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PrimaryInvoiceActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.16f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun SecondaryActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
