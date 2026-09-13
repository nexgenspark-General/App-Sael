package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.SaleRecord
import com.example.ui.theme.DonsaelAmber
import com.example.ui.theme.DonsaelBlue
import com.example.ui.theme.DonsaelGreen
import com.example.ui.theme.DonsaelRed
import com.example.ui.theme.DonsaelTeal
import java.util.Calendar
import java.util.Locale

enum class ReportPeriod(val label: String) {
    TODAY("Aujourd'hui"),
    THIS_MONTH("Ce Mois"),
    THIS_YEAR("Cette Année"),
    ALL_TIME("Tout l'Historique")
}

@Composable
fun ReportsScreen(
    sales: List<SaleRecord>,
    products: List<Product>,
    exchangeRate: Double,
    onExportPdf: (periodLabel: String, currencyFilter: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    var currencyFilter by remember { mutableStateOf("TOUT") } // "TOUT", "HTG", "USD"

    val now = Calendar.getInstance()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    val monthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    val yearStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    val filteredSales = remember(sales, selectedPeriod, currencyFilter) {
        sales.filter { sale ->
            val matchPeriod = when (selectedPeriod) {
                ReportPeriod.TODAY -> sale.timestamp >= todayStart
                ReportPeriod.THIS_MONTH -> sale.timestamp >= monthStart
                ReportPeriod.THIS_YEAR -> sale.timestamp >= yearStart
                ReportPeriod.ALL_TIME -> true
            }

            val matchCurrency = when (currencyFilter) {
                "HTG" -> sale.paymentCurrency == "HTG"
                "USD" -> sale.paymentCurrency == "USD"
                else -> true
            }

            matchPeriod && matchCurrency
        }
    }

    // Revenue and profit calculation
    var totalRevenueHtg = 0.0
    var totalProfitHtg = 0.0
    var totalSalesCount = filteredSales.size

    for (s in filteredSales) {
        val rev = if (s.paymentCurrency == "HTG") s.totalAmount else s.totalAmount * exchangeRate
        val cost = if (s.paymentCurrency == "HTG") s.purchaseCostTotal else s.purchaseCostTotal * exchangeRate
        totalRevenueHtg += rev
        totalProfitHtg += (rev - cost)
    }

    val avgBasketHtg = if (totalSalesCount > 0) totalRevenueHtg / totalSalesCount else 0.0

    // Payment methods breakdown
    val methodBreakdown = remember(filteredSales) {
        filteredSales.groupBy { it.paymentMethod }
            .mapValues { entry ->
                entry.value.sumOf { s ->
                    if (s.paymentCurrency == "HTG") s.totalAmount else s.totalAmount * exchangeRate
                }
            }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title & Export
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rapports Financiers & Analytiques",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Évaluation périodique et ventilation des marges",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { onExportPdf(selectedPeriod.label, currencyFilter) },
                    modifier = Modifier.testTag("export_report_pdf_btn")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exporter PDF", fontSize = 11.5.sp)
                }
            }
        }

        // Period filter pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReportPeriod.values().forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label, fontSize = 11.5.sp) }
                    )
                }
            }
        }

        // Summary KPI Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Performance (${selectedPeriod.label})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Chiffre d'Affaires :", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.US, "%,.2f HTG", totalRevenueHtg),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.US, "Équiv. %,.2f USD", totalRevenueHtg / exchangeRate),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Bénéfice Net :", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.US, "%,.2f HTG", totalProfitHtg),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = DonsaelGreen
                            )
                            Text(
                                text = String.format(Locale.US, "Équiv. %,.2f USD", totalProfitHtg / exchangeRate),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Nombre total de ventes :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$totalSalesCount vente(s)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Panier moyen par client :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%,.2f HTG", avgBasketHtg),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Methods Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Répartition par Mode de Règlement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (methodBreakdown.isEmpty()) {
                        Text(
                            text = "Aucune transaction pour cette période.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        methodBreakdown.forEach { (method, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = method, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = String.format(Locale.US, "%,.2f HTG", amount),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // NexGen Spark Branding Banner
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NexGen Spark — Partenaire Technologique",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Documents certifiés pour DONSAEL COMMUNICATION ET MULTI-SERVICES",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
