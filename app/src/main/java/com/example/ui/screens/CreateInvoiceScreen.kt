package com.example.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.domain.DraftInvoiceItem
import com.example.domain.InvoiceType
import com.example.domain.MoneyMath
import com.example.ui.DraftPrefill
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PurchaseAmber
import com.example.ui.theme.SaleEmerald
import java.math.BigDecimal

@Composable
fun CreateInvoiceScreen(
    initialType: InvoiceType,
    prefill: DraftPrefill?,
    prices: List<GoldPriceEntity>,
    settings: AppSettingsState,
    onConsumePrefill: () -> Unit,
    onShowValidationError: (String) -> Unit,
    onSaveInvoice: (
        type: InvoiceType,
        partyName: String,
        partyPhone: String,
        notes: String,
        currencyCode: String,
        items: List<DraftInvoiceItem>,
        discount: BigDecimal,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var invoiceType by remember(initialType) { mutableStateOf(prefill?.type ?: initialType) }
    var partyName by remember { mutableStateOf("") }
    var partyPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("0") }
    var nextKey by remember { mutableLongStateOf(2L) }

    fun defaultPriceFor(karatId: String, type: InvoiceType): String {
        val found = prices.firstOrNull { it.id == karatId } ?: prices.firstOrNull()
        return if (found != null) {
            if (type == InvoiceType.SALE) found.sellPricePerGram else found.buyPricePerGram
        } else "0"
    }

    val items = remember {
        mutableStateListOf(
            if (prefill != null) {
                DraftInvoiceItem(
                    localKey = 1L,
                    categoryType = prefill.categoryType,
                    karatId = prefill.karatId,
                    karatLabel = prefill.karatLabel,
                    description = "",
                    weightInput = prefill.weightInput,
                    pricePerGramInput = prefill.pricePerGramInput,
                    workmanshipInput = prefill.workmanshipInput,
                )
            } else {
                DraftInvoiceItem(
                    localKey = 1L,
                    categoryType = "مجوهرات",
                    karatId = "k21",
                    karatLabel = "ذهب عيار 21",
                    description = "",
                    weightInput = "",
                    pricePerGramInput = defaultPriceFor("k21", invoiceType),
                    workmanshipInput = "0",
                )
            }
        )
    }

    LaunchedEffect(prefill) {
        if (prefill != null) {
            onConsumePrefill()
        }
    }

    // Update default price if prices load slightly after composition and field is still 0
    LaunchedEffect(prices) {
        if (prices.isNotEmpty()) {
            for (i in items.indices) {
                if (MoneyMath.parseDecimal(items[i].pricePerGramInput) <= BigDecimal.ZERO) {
                    items[i] = items[i].copy(
                        pricePerGramInput = defaultPriceFor(items[i].karatId, invoiceType)
                    )
                }
            }
        }
    }

    val typeAccent = if (invoiceType == InvoiceType.SALE) SaleEmerald else PurchaseAmber
    val discountDec = MoneyMath.parseDecimal(discountInput)
    val totals = MoneyMath.calculateInvoiceTotals(items.toList(), discountDec)
    val nextSerialPreview = if (invoiceType == InvoiceType.SALE) {
        "S-${settings.nextSaleSeq}"
    } else {
        "P-${settings.nextPurchaseSeq}"
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Invoice Type & Official Serial Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ),
                    border = BorderStroke(1.3.dp, typeAccent.copy(alpha = 0.65f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FilterChip(
                                selected = invoiceType == InvoiceType.SALE,
                                onClick = {
                                    if (invoiceType != InvoiceType.SALE) {
                                        invoiceType = InvoiceType.SALE
                                        for (i in items.indices) {
                                            items[i] = items[i].copy(
                                                pricePerGramInput = defaultPriceFor(items[i].karatId, InvoiceType.SALE)
                                            )
                                        }
                                    }
                                },
                                label = { Text("فاتورة بيع") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("invoice_type_sale_chip"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaleEmerald.copy(alpha = 0.2f),
                                    selectedLabelColor = SaleEmerald,
                                ),
                            )
                            FilterChip(
                                selected = invoiceType == InvoiceType.PURCHASE,
                                onClick = {
                                    if (invoiceType != InvoiceType.PURCHASE) {
                                        invoiceType = InvoiceType.PURCHASE
                                        for (i in items.indices) {
                                            items[i] = items[i].copy(
                                                pricePerGramInput = defaultPriceFor(items[i].karatId, InvoiceType.PURCHASE)
                                            )
                                        }
                                    }
                                },
                                label = { Text("فاتورة شراء") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("invoice_type_purchase_chip"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurchaseAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = PurchaseAmber,
                                ),
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = typeAccent.copy(alpha = 0.1f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "الرقم التسلسلي: $nextSerialPreview",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = typeAccent,
                                )
                                Text(
                                    text = "هاتف المحل: 779944094",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = partyName,
                            onValueChange = { partyName = it },
                            label = {
                                Text(
                                    if (invoiceType == InvoiceType.SALE) "اسم العميل (اختياري)"
                                    else "اسم البائع (اختياري)"
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_party_name_input"),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = partyPhone,
                                onValueChange = { partyPhone = it },
                                label = { Text("رقم الهاتف (اختياري)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات (اختياري)") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
                            )
                        }
                    }
                }
            }

            // Line Items
            itemsIndexed(items, key = { _, item -> item.localKey }) { index, item ->
                InvoiceLineEditorCard(
                    index = index,
                    item = item,
                    prices = prices,
                    currencyCode = settings.activeCurrencyCode,
                    canDelete = items.size > 1,
                    onUpdate = { updated -> items[index] = updated },
                    onSelectKarat = { priceEntity ->
                        val autoPrice = if (invoiceType == InvoiceType.SALE) {
                            priceEntity.sellPricePerGram
                        } else {
                            priceEntity.buyPricePerGram
                        }
                        items[index] = items[index].copy(
                            karatId = priceEntity.id,
                            karatLabel = priceEntity.nameAr,
                            pricePerGramInput = autoPrice,
                        )
                    },
                    onDelete = { items.removeAt(index) },
                )
            }

            // Add Item Button
            item {
                OutlinedButton(
                    onClick = {
                        items.add(
                            DraftInvoiceItem(
                                localKey = nextKey++,
                                categoryType = "مجوهرات",
                                karatId = "k21",
                                karatLabel = "ذهب عيار 21",
                                description = "",
                                weightInput = "",
                                pricePerGramInput = defaultPriceFor("k21", invoiceType),
                                workmanshipInput = "0",
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("add_invoice_item_btn"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة صنف آخر للفاتورة")
                }
            }

            // Discount & Totals Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    ),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { discountInput = MoneyMath.sanitizeDecimalInput(it) },
                            label = { Text("الخصم (اختياري)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_discount_input"),
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SummaryLine("إجمالي الوزن", MoneyMath.formatGrams(totals.totalWeightGrams))
                        SummaryLine(
                            "المجموع الفرعي",
                            MoneyMath.formatMoney(totals.subtotal, settings.activeCurrencyCode),
                        )
                        if (totals.discount > BigDecimal.ZERO) {
                            SummaryLine(
                                "الخصم",
                                "- ${MoneyMath.formatMoney(totals.discount, settings.activeCurrencyCode)}",
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "الصافي النهائي",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = MoneyMath.formatMoney(totals.netTotal, settings.activeCurrencyCode),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Button(
                            onClick = {
                                val validItems = items.filter {
                                    it.weightGrams > BigDecimal.ZERO && it.pricePerGram > BigDecimal.ZERO
                                }
                                if (validItems.isEmpty()) {
                                    onShowValidationError("يرجى إدخال وزن وسعر جرام صحيح لصنف واحد على الأقل.")
                                } else {
                                    onSaveInvoice(
                                        invoiceType,
                                        partyName,
                                        partyPhone,
                                        notes,
                                        settings.activeCurrencyCode,
                                        validItems,
                                        discountDec,
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .height(52.dp)
                                .testTag("save_invoice_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = typeAccent,
                                contentColor = androidx.compose.ui.graphics.Color.White,
                            ),
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حفظ ${invoiceType.labelAr} ومعاينتها",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun InvoiceLineEditorCard(
    index: Int,
    item: DraftInvoiceItem,
    prices: List<GoldPriceEntity>,
    currencyCode: String,
    canDelete: Boolean,
    onUpdate: (DraftInvoiceItem) -> Unit,
    onSelectKarat: (GoldPriceEntity) -> Unit,
    onDelete: () -> Unit,
) {
    val categories = listOf("مجوهرات", "ذهب خام", "كسر")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
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
                Text(
                    text = "تفاصيل الصنف #${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "حذف الصنف",
                            tint = DangerRed,
                        )
                    }
                }
            }

            // Category chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = item.categoryType == cat,
                        onClick = { onUpdate(item.copy(categoryType = cat)) },
                        label = { Text(cat) },
                    )
                }
            }

            // Karat chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                prices.forEach { price ->
                    FilterChip(
                        selected = item.karatId == price.id,
                        onClick = { onSelectKarat(price) },
                        label = { Text(price.nameAr) },
                    )
                }
            }

            OutlinedTextField(
                value = item.description,
                onValueChange = { onUpdate(item.copy(description = it)) },
                label = { Text("وصف القطعة أو السبيكة (اختياري)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = item.weightInput,
                    onValueChange = {
                        onUpdate(item.copy(weightInput = MoneyMath.sanitizeDecimalInput(it)))
                    },
                    label = { Text("الوزن (جم)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_weight_input_$index"),
                )
                OutlinedTextField(
                    value = item.pricePerGramInput,
                    onValueChange = {
                        onUpdate(item.copy(pricePerGramInput = MoneyMath.sanitizeDecimalInput(it)))
                    },
                    label = { Text("سعر الجرام") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_price_input_$index"),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = item.workmanshipInput,
                    onValueChange = {
                        onUpdate(item.copy(workmanshipInput = MoneyMath.sanitizeDecimalInput(it)))
                    },
                    label = { Text("المصنعية (اختياري)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = "إجمالي الصنف",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = MoneyMath.formatMoney(item.lineTotal, currencyCode),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
