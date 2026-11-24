package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.InvoiceWithItems
import com.example.domain.InvoiceType
import com.example.domain.MoneyMath
import com.example.ui.theme.PurchaseAmberLight
import com.example.ui.theme.SaleEmeraldLight
import com.example.util.InvoiceDocumentRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

@Composable
fun InvoicePreviewScreen(
    invoiceWithItems: InvoiceWithItems?,
    onShowMessage: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (invoiceWithItems == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "جاري تحميل بيانات الفاتورة...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val invoice = invoiceWithItems.invoice
    val items = invoiceWithItems.items
    val type = InvoiceType.fromName(invoice.type)
    val badgeColor = if (type == InvoiceType.SALE) SaleEmeraldLight else PurchaseAmberLight

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        InvoiceDocumentRenderer.renderPdfBytes(context, invoiceWithItems)
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    }
                    onShowMessage("تم حفظ الفاتورة بصيغة PDF بنجاح.", false)
                } catch (_: Exception) {
                    onShowMessage("تعذر حفظ ملف PDF.", true)
                }
            }
        }
    }

    val savePngLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        InvoiceDocumentRenderer.renderPngBytes(context, invoiceWithItems)
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    }
                    onShowMessage("تم حفظ الفاتورة كصورة PNG بنجاح.", false)
                } catch (_: Exception) {
                    onShowMessage("تعذر حفظ الصورة.", true)
                }
            }
        }
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
            // Formal Official Paper Invoice Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.8.dp, Color(0xFFC59B27)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Official Shop Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1A1510),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "أبو إسحاق لبيع وشراء الذهب الخام والمجوهرات",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFF2D06B),
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = "هاتف: 779944094",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                )
                            }
                        }

                        // Invoice Metadata Strip
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFAF4E4),
                            border = BorderStroke(1.dp, Color(0xFFE0CFA2)),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeColor,
                                    ) {
                                        Text(
                                            text = "${type.labelAr} رسمية",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                    Text(
                                        text = "رقم: ${invoice.invoiceNumber}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1F1A13),
                                    )
                                }

                                Text(
                                    text = "التاريخ والوقت: ${MoneyMath.formatDateTime(invoice.createdAtMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4F4639),
                                )

                                val partyTitle = if (type == InvoiceType.SALE) "العميل" else "البائع"
                                val partyVal = invoice.partyName.ifBlank { "عميل نقدي" }
                                val phoneSuffix = if (invoice.partyPhone.isNotBlank()) " (${invoice.partyPhone})" else ""
                                Text(
                                    text = "$partyTitle: $partyVal$phoneSuffix",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF1F1A13),
                                )
                            }
                        }

                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2A2218), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "الصنف",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF2D06B),
                                modifier = Modifier.weight(1.5f),
                            )
                            Text(
                                text = "الوزن",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF2D06B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.9f),
                            )
                            Text(
                                text = "السعر",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF2D06B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.9f),
                            )
                            Text(
                                text = "الإجمالي",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF2D06B),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.1f),
                            )
                        }

                        // Table Rows
                        items.forEachIndexed { idx, item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 1) Color(0xFFFBF8F1) else Color.White)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            text = "${item.categoryType} • ${item.karatLabel}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF1F1A13),
                                        )
                                        if (item.description.isNotBlank()) {
                                            Text(
                                                text = item.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF635846),
                                            )
                                        }
                                        val work = MoneyMath.parseDecimal(item.workmanshipAmount)
                                        if (work > BigDecimal.ZERO) {
                                            Text(
                                                text = "مصنعية: ${MoneyMath.formatMoney(work, invoice.currencyCode)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF7A5E14),
                                            )
                                        }
                                    }
                                    Text(
                                        text = MoneyMath.formatGrams(MoneyMath.parseDecimal(item.weightGrams)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1F1A13),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.9f),
                                    )
                                    Text(
                                        text = MoneyMath.formatAmount(MoneyMath.parseDecimal(item.pricePerGram)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1F1A13),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.9f),
                                    )
                                    Text(
                                        text = MoneyMath.formatMoney(
                                            MoneyMath.parseDecimal(item.lineTotalAmount),
                                            invoice.currencyCode,
                                        ),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1F1A13),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.1f),
                                    )
                                }
                                HorizontalDivider(
                                    color = Color(0xFFEADFC5),
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }

                        if (invoice.notes.isNotBlank()) {
                            Text(
                                text = "ملاحظات: ${invoice.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5A5042),
                            )
                        }

                        // Totals Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFAF4E4),
                            border = BorderStroke(1.dp, Color(0xFFD8C28B)),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                PaperRow("إجمالي الوزن", MoneyMath.formatGrams(MoneyMath.parseDecimal(invoice.totalWeightGrams)))
                                PaperRow(
                                    "المجموع الفرعي",
                                    MoneyMath.formatMoney(MoneyMath.parseDecimal(invoice.subtotalAmount), invoice.currencyCode),
                                )
                                val disc = MoneyMath.parseDecimal(invoice.discountAmount)
                                if (disc > BigDecimal.ZERO) {
                                    PaperRow(
                                        "الخصم",
                                        "- ${MoneyMath.formatMoney(disc, invoice.currencyCode)}",
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFC59B27))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "الصافي النهائي",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1F1A13),
                                    )
                                    Text(
                                        text = MoneyMath.formatMoney(
                                            MoneyMath.parseDecimal(invoice.netTotalAmount),
                                            invoice.currencyCode,
                                        ),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF966E08),
                                    )
                                }
                            }
                        }

                        Text(
                            text = "أبو إسحاق لبيع وشراء الذهب الخام والمجوهرات • 779944094",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B604E),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Print / Share / Save Actions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { InvoiceDocumentRenderer.printInvoice(context, invoiceWithItems) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .height(48.dp)
                                .testTag("print_invoice_btn"),
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("طباعة الفاتورة / حفظ PDF عبر الطابعة")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val uri = withContext(Dispatchers.IO) {
                                                InvoiceDocumentRenderer.createShareablePdfUri(context, invoiceWithItems)
                                            }
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                this.type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة PDF"))
                                        } catch (_: Exception) {
                                            onShowMessage("تعذر مشاركة ملف PDF.", true)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("share_pdf_btn"),
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشاركة PDF")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val uri = withContext(Dispatchers.IO) {
                                                InvoiceDocumentRenderer.createShareablePngUri(context, invoiceWithItems)
                                            }
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                this.type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة كصورة"))
                                        } catch (_: Exception) {
                                            onShowMessage("تعذر مشاركة الصورة.", true)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("share_image_btn"),
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشاركة كصورة")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    savePdfLauncher.launch("فاتورة_${invoice.invoiceNumber}.pdf")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                                colors = ButtonDefaults.outlinedButtonColors(),
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ PDF")
                            }

                            OutlinedButton(
                                onClick = {
                                    savePngLauncher.launch("فاتورة_${invoice.invoiceNumber}.png")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ صورة")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PaperRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4E4537),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1F1A13),
        )
    }
}
