package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.data.local.GoldPriceEntity
import com.example.domain.CurrencyOption
import com.example.domain.InvoiceType
import com.example.domain.MoneyMath
import com.example.ui.DraftPrefill
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.PurchaseAmber
import com.example.ui.theme.SaleEmerald
import java.math.BigDecimal

@Composable
fun PricesAndCalculatorScreen(
    prices: List<GoldPriceEntity>,
    settings: AppSettingsState,
    isSyncing: Boolean,
    onRefreshOnline: () -> Unit,
    onEditPrice: (id: String, sellInput: String, buyInput: String) -> Unit,
    onChangeCurrency: (String) -> Unit,
    onUpdateExchangeRates: (sarRate: String, yerRate: String) -> Unit,
    onCreateInvoiceFromCalc: (DraftPrefill) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingPrice by remember { mutableStateOf<GoldPriceEntity?>(null) }
    var showRatesDialog by remember { mutableStateOf(false) }

    // Instant Calculator State
    var selectedKaratId by remember { mutableStateOf("k21") }
    var isSaleMode by remember { mutableStateOf(true) }
    var weightInput by remember { mutableStateOf("10") }
    var rawPurityInput by remember { mutableStateOf("995") }
    var workmanshipInput by remember { mutableStateOf("0") }

    val selectedPriceEntity = remember(prices, selectedKaratId) {
        prices.firstOrNull { it.id == selectedKaratId } ?: prices.firstOrNull()
    }
    val k24PriceEntity = remember(prices) {
        prices.firstOrNull { it.id == "k24" }
    }

    val appliedGramPrice = remember(selectedPriceEntity, k24PriceEntity, isSaleMode, rawPurityInput, selectedKaratId) {
        val baseEntity = selectedPriceEntity
        if (baseEntity == null) {
            BigDecimal.ZERO
        } else if (selectedKaratId == "raw" && rawPurityInput.isNotBlank()) {
            val purity = MoneyMath.parseDecimal(rawPurityInput)
            val pure24 = k24PriceEntity?.let {
                MoneyMath.parseDecimal(if (isSaleMode) it.sellPricePerGram else it.buyPricePerGram)
            } ?: MoneyMath.parseDecimal(if (isSaleMode) baseEntity.sellPricePerGram else baseEntity.buyPricePerGram)
            if (purity > BigDecimal.ZERO && purity <= BigDecimal("1000")) {
                MoneyMath.calculateRawGoldPriceByPurity(pure24, purity)
            } else {
                MoneyMath.parseDecimal(if (isSaleMode) baseEntity.sellPricePerGram else baseEntity.buyPricePerGram)
            }
        } else {
            MoneyMath.parseDecimal(if (isSaleMode) baseEntity.sellPricePerGram else baseEntity.buyPricePerGram)
        }
    }

    val weightDec = MoneyMath.parseDecimal(weightInput)
    val workDec = MoneyMath.parseDecimal(workmanshipInput)
    val goldValue = MoneyMath.calculateGoldValue(weightDec, appliedGramPrice)
    val calcTotal = MoneyMath.calculateLineTotal(weightDec, appliedGramPrice, workDec)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Currency & Online Sync Control Card
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
                            Text(
                                text = "تفاصيل الأسعار والعملة",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            TextButton(onClick = { showRatesDialog = true }) {
                                Icon(
                                    imageVector = Icons.Filled.CurrencyExchange,
                                    contentDescription = "أسعار الصرف",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("أسعار الصرف")
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CurrencyOption.entries.forEach { option ->
                                val selected = settings.activeCurrencyCode == option.code
                                FilterChip(
                                    selected = selected,
                                    onClick = { onChangeCurrency(option.code) },
                                    label = { Text("${option.labelAr} (${option.symbol})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                    modifier = Modifier.testTag("currency_chip_${option.code}"),
                                )
                            }
                        }

                        SyncStatusBadge(syncStatus = settings.syncStatus)

                        Text(
                            text = if (settings.lastSyncMillis > 0L) {
                                "آخر تحديث: ${MoneyMath.formatDateTime(settings.lastSyncMillis)}"
                            } else {
                                "يعمل بالأسعار المحفوظة محلياً"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Button(
                            onClick = onRefreshOnline,
                            enabled = !isSyncing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .height(48.dp)
                                .testTag("prices_sync_btn"),
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري التحديث عبر الإنترنت...")
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تحديث الأسعار عبر الإنترنت")
                            }
                        }
                    }
                }
            }

            // Instant Gold Weight Calculator Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "حاسبة الذهب الفورية بالجرام",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        // Buy vs Sell Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FilterChip(
                                selected = isSaleMode,
                                onClick = { isSaleMode = true },
                                label = { Text("حساب بسعر البيع") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calc_mode_sell"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaleEmerald.copy(alpha = 0.2f),
                                    selectedLabelColor = SaleEmerald,
                                ),
                            )
                            FilterChip(
                                selected = !isSaleMode,
                                onClick = { isSaleMode = false },
                                label = { Text("حساب بسعر الشراء") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calc_mode_buy"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurchaseAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = PurchaseAmber,
                                ),
                            )
                        }

                        // Karat selector chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            prices.forEach { price ->
                                FilterChip(
                                    selected = selectedKaratId == price.id,
                                    onClick = { selectedKaratId = price.id },
                                    label = { Text(price.nameAr) },
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { weightInput = MoneyMath.sanitizeDecimalInput(it) },
                                label = { Text("الوزن (جرام)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calc_weight_input"),
                            )
                            OutlinedTextField(
                                value = workmanshipInput,
                                onValueChange = { workmanshipInput = MoneyMath.sanitizeDecimalInput(it) },
                                label = { Text("المصنعية (اختياري)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calc_workmanship_input"),
                            )
                        }

                        AnimatedVisibility(visible = selectedKaratId == "raw") {
                            OutlinedTextField(
                                value = rawPurityInput,
                                onValueChange = { rawPurityInput = MoneyMath.sanitizeDecimalInput(it) },
                                label = { Text("العيار الفعلي للذهب الخام بالألف (مثال: 995 أو 916)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                CalcResultRow(
                                    label = "سعر الجرام المطبق",
                                    value = MoneyMath.formatMoney(appliedGramPrice, settings.activeCurrencyCode),
                                )
                                CalcResultRow(
                                    label = "قيمة الذهب (${MoneyMath.formatGrams(weightDec)})",
                                    value = MoneyMath.formatMoney(goldValue, settings.activeCurrencyCode),
                                )
                                if (workDec > BigDecimal.ZERO) {
                                    CalcResultRow(
                                        label = "المصنعية",
                                        value = MoneyMath.formatMoney(workDec, settings.activeCurrencyCode),
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "الإجمالي الدقيق",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        text = MoneyMath.formatMoney(calcTotal, settings.activeCurrencyCode),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val entity = selectedPriceEntity ?: return@OutlinedButton
                                onCreateInvoiceFromCalc(
                                    DraftPrefill(
                                        type = if (isSaleMode) InvoiceType.SALE else InvoiceType.PURCHASE,
                                        categoryType = if (entity.id == "raw") "ذهب خام" else if (entity.id == "scrap21") "كسر" else "مجوهرات",
                                        karatId = entity.id,
                                        karatLabel = entity.nameAr,
                                        weightInput = weightInput.ifBlank { "1" },
                                        pricePerGramInput = MoneyMath.formatPlainDecimal(appliedGramPrice),
                                        workmanshipInput = workmanshipInput.ifBlank { "0" },
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("calc_to_invoice_btn"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSaleMode) "تحويل إلى فاتورة بيع" else "تحويل إلى فاتورة شراء",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            // Gold Categories Header
            item {
                Text(
                    text = "جدول أسعار العيارات والذهب الخام (اضغط تعديل للتغيير اليدوي)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            items(prices, key = { it.id }) { price ->
                GoldPriceItemCard(
                    price = price,
                    currencyCode = settings.activeCurrencyCode,
                    onEditClick = { editingPrice = price },
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    editingPrice?.let { item ->
        EditPriceDialog(
            price = item,
            currencyCode = settings.activeCurrencyCode,
            onDismiss = { editingPrice = null },
            onSave = { sell, buy ->
                onEditPrice(item.id, sell, buy)
                editingPrice = null
            },
        )
    }

    if (showRatesDialog) {
        ExchangeRatesDialog(
            currentSar = settings.sarRate,
            currentYer = settings.yerRate,
            onDismiss = { showRatesDialog = false },
            onSave = { sar, yer ->
                onUpdateExchangeRates(sar, yer)
                showRatesDialog = false
            },
        )
    }
}

@Composable
private fun CalcResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun GoldPriceItemCard(
    price: GoldPriceEntity,
    currencyCode: String,
    onEditClick: () -> Unit,
) {
    val purityPct = MoneyMath.parseDecimal(price.purityRatio)
        .multiply(BigDecimal("100"))
        .setScale(1, java.math.RoundingMode.HALF_UP)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = price.nameAr,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "النقاء المعتمد: $purityPct%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("edit_price_${price.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "تعديل سعر ${price.nameAr}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = SaleEmerald.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, SaleEmerald.copy(alpha = 0.35f)),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "سعر البيع للجرام",
                            style = MaterialTheme.typography.labelSmall,
                            color = SaleEmerald,
                        )
                        Text(
                            text = MoneyMath.formatMoney(
                                MoneyMath.parseDecimal(price.sellPricePerGram),
                                currencyCode,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = PurchaseAmber.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, PurchaseAmber.copy(alpha = 0.35f)),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "سعر الشراء للجرام",
                            style = MaterialTheme.typography.labelSmall,
                            color = PurchaseAmber,
                        )
                        Text(
                            text = MoneyMath.formatMoney(
                                MoneyMath.parseDecimal(price.buyPricePerGram),
                                currencyCode,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPriceDialog(
    price: GoldPriceEntity,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (sell: String, buy: String) -> Unit,
) {
    var sellInput by remember(price.id) { mutableStateOf(price.sellPricePerGram) }
    var buyInput by remember(price.id) { mutableStateOf(price.buyPricePerGram) }
    val currency = CurrencyOption.fromCode(currencyCode)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعديل ${price.nameAr}",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "الأسعار بالعملة النشطة (${currency.labelAr} - ${currency.symbol})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = sellInput,
                    onValueChange = { sellInput = MoneyMath.sanitizeDecimalInput(it) },
                    label = { Text("سعر البيع للجرام") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = buyInput,
                    onValueChange = { buyInput = MoneyMath.sanitizeDecimalInput(it) },
                    label = { Text("سعر الشراء للجرام") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(sellInput, buyInput) }) {
                Text("حفظ محلياً")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}

@Composable
private fun ExchangeRatesDialog(
    currentSar: String,
    currentYer: String,
    onDismiss: () -> Unit,
    onSave: (sar: String, yer: String) -> Unit,
) {
    var sarInput by remember { mutableStateOf(currentSar) }
    var yerInput by remember { mutableStateOf(currentYer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("أسعار الصرف مقابل الدولار ($)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "تُستخدم هذه الأسعار عند جلب سعر الأونصة العالمي من الإنترنت وتحويله إلى العملة المختارة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = sarInput,
                    onValueChange = { sarInput = MoneyMath.sanitizeDecimalInput(it) },
                    label = { Text("سعر 1 دولار بالريال السعودي (ر.س)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = yerInput,
                    onValueChange = { yerInput = MoneyMath.sanitizeDecimalInput(it) },
                    label = { Text("سعر 1 دولار بالريال اليمني (ر.ي)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(sarInput, yerInput) }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}
