package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.InvoiceWithItems
import com.example.domain.InvoiceType
import com.example.domain.MoneyMath
import com.example.ui.DateFilterRange
import com.example.ui.InvoiceFilterType
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PurchaseAmber
import com.example.ui.theme.SaleEmerald
import java.math.BigDecimal

@Composable
fun InvoiceHistoryScreen(
    invoices: List<InvoiceWithItems>,
    searchQuery: String,
    filterType: InvoiceFilterType,
    dateRange: DateFilterRange,
    activeCurrencyCode: String,
    onSearchChange: (String) -> Unit,
    onFilterTypeChange: (InvoiceFilterType) -> Unit,
    onDateRangeChange: (DateFilterRange) -> Unit,
    onOpenInvoice: (Long) -> Unit,
    onDeleteInvoice: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<InvoiceWithItems?>(null) }

    val summary = remember(invoices) {
        var totalGrams = BigDecimal.ZERO
        var totalSales = BigDecimal.ZERO
        var totalPurchases = BigDecimal.ZERO
        invoices.forEach { entry ->
            totalGrams = totalGrams.add(MoneyMath.parseDecimal(entry.invoice.totalWeightGrams))
            val net = MoneyMath.parseDecimal(entry.invoice.netTotalAmount)
            if (entry.invoice.type == InvoiceType.SALE.name) {
                totalSales = totalSales.add(net)
            } else {
                totalPurchases = totalPurchases.add(net)
            }
        }
        Triple(totalGrams, totalSales, totalPurchases)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Search & Filter Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("ابحث برقم الفاتورة، اسم العميل، الصنف أو الملاحظات...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = "مسح البحث")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_search_input"),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            InvoiceFilterType.entries.forEach { ft ->
                                FilterChip(
                                    selected = filterType == ft,
                                    onClick = { onFilterTypeChange(ft) },
                                    label = { Text(ft.labelAr) },
                                    modifier = Modifier.testTag("filter_type_${ft.name}"),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DateFilterRange.entries.forEach { dr ->
                                FilterChip(
                                    selected = dateRange == dr,
                                    onClick = { onDateRangeChange(dr) },
                                    label = { Text(dr.labelAr) },
                                )
                            }
                        }
                    }
                }
            }

            // Filtered Summary Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HistorySummaryPill(
                        label = "عدد الفواتير",
                        value = "${invoices.size}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(0.8f),
                    )
                    HistorySummaryPill(
                        label = "المبيعات",
                        value = MoneyMath.formatMoney(summary.second, activeCurrencyCode),
                        color = SaleEmerald,
                        modifier = Modifier.weight(1.2f),
                    )
                    HistorySummaryPill(
                        label = "المشتريات",
                        value = MoneyMath.formatMoney(summary.third, activeCurrencyCode),
                        color = PurchaseAmber,
                        modifier = Modifier.weight(1.2f),
                    )
                }
            }

            if (invoices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = "لا توجد فواتير مطابقة للبحث",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "أنشئ فاتورة بيع أو شراء جديدة وسيتم حفظها هنا محلياً بدون إنترنت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(invoices, key = { it.invoice.id }) { entry ->
                    InvoiceHistoryCard(
                        entry = entry,
                        onOpen = { onOpenInvoice(entry.invoice.id) },
                        onDelete = { pendingDelete = entry },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("تأكيد حذف الفاتورة") },
            text = {
                Text("هل أنت متأكد من حذف الفاتورة رقم ${entry.invoice.invoiceNumber} من السجل المحلي؟ لا يمكن التراجع عن الحذف.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteInvoice(entry.invoice.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("إلغاء")
                }
            },
        )
    }
}

@Composable
private fun HistorySummaryPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InvoiceHistoryCard(
    entry: InvoiceWithItems,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val inv = entry.invoice
    val type = InvoiceType.fromName(inv.type)
    val accent = if (type == InvoiceType.SALE) SaleEmerald else PurchaseAmber

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = type.labelAr,
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Text(
                        text = inv.invoiceNumber,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Text(
                    text = MoneyMath.formatMoney(
                        MoneyMath.parseDecimal(inv.netTotalAmount),
                        inv.currencyCode,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = inv.partyName.ifBlank { "عميل نقدي" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = MoneyMath.formatGrams(MoneyMath.parseDecimal(inv.totalWeightGrams)),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = MoneyMath.formatDateTime(inv.createdAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpen,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "معاينة الفاتورة",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "حذف الفاتورة",
                            tint = DangerRed,
                        )
                    }
                }
            }
        }
    }
}
