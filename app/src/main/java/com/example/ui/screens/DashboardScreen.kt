package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Product
import com.example.data.SaleRecord
import com.example.ui.theme.DonsaelAmber
import com.example.ui.theme.DonsaelBlue
import com.example.ui.theme.DonsaelGreen
import com.example.ui.theme.DonsaelRed
import com.example.ui.theme.DonsaelTeal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    products: List<Product>,
    sales: List<SaleRecord>,
    creditSales: List<SaleRecord>,
    exchangeRate: Double,
    messageOfTheDay: String,
    modifier: Modifier = Modifier
) {
    // Current year & month calculation
    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR)
    val currentMonth = now.get(Calendar.MONTH)

    // Finalized sales (Comptant + Fully Paid Credits)
    val finalizedSales = sales.filter { it.paymentType == "COMPTANT" || it.creditStatus == "PAID" }

    // Annual Sales (Hors ventes à crédit en cours)
    val calSale = Calendar.getInstance()
    val annualSales = finalizedSales.filter {
        calSale.timeInMillis = it.timestamp
        calSale.get(Calendar.YEAR) == currentYear
    }
    var annualRevenueHtg = 0.0
    var annualProfitHtg = 0.0
    for (s in annualSales) {
        val rev = if (s.paymentCurrency == "HTG") s.totalAmount else s.totalAmount * exchangeRate
        val cost = if (s.paymentCurrency == "HTG") s.purchaseCostTotal else s.purchaseCostTotal * exchangeRate
        annualRevenueHtg += rev
        annualProfitHtg += (rev - cost)
    }

    // Current Month Performance
    val monthSales = finalizedSales.filter {
        calSale.timeInMillis = it.timestamp
        calSale.get(Calendar.YEAR) == currentYear && calSale.get(Calendar.MONTH) == currentMonth
    }
    var monthRevenueHtg = 0.0
    var monthProfitHtg = 0.0
    for (s in monthSales) {
        val rev = if (s.paymentCurrency == "HTG") s.totalAmount else s.totalAmount * exchangeRate
        val cost = if (s.paymentCurrency == "HTG") s.purchaseCostTotal else s.purchaseCostTotal * exchangeRate
        monthRevenueHtg += rev
        monthProfitHtg += (rev - cost)
    }
    val avgCartHtg = if (monthSales.isNotEmpty()) monthRevenueHtg / monthSales.size else 0.0

    // Stock value total and by category
    var stockElectromenagerHtg = 0.0
    var stockElectroniqueHtg = 0.0
    var stockDiversHtg = 0.0

    for (p in products) {
        val valHtg = if (p.currency == "HTG") p.sellingPrice * p.quantity else (p.sellingPrice * p.quantity) * exchangeRate
        when {
            p.category.contains("électroménager", ignoreCase = true) -> stockElectromenagerHtg += valHtg
            p.category.contains("électronique", ignoreCase = true) -> stockElectroniqueHtg += valHtg
            else -> stockDiversHtg += valHtg
        }
    }
    val totalStockHtg = stockElectromenagerHtg + stockElectroniqueHtg + stockDiversHtg

    // Client credits summary
    val pendingCredits = creditSales.filter { it.creditStatus != "PAID" && it.creditRemainingBalance > 0 }
    var totalCreditRemainingHtg = 0.0
    val debtorClients = pendingCredits.mapNotNull { it.customerName }.distinct().size

    for (c in pendingCredits) {
        val rem = if (c.paymentCurrency == "HTG") c.creditRemainingBalance else c.creditRemainingBalance * exchangeRate
        totalCreditRemainingHtg += rem
    }

    // Low stock alerts (<= 4 units)
    val lowStockProducts = products.filter { it.quantity <= it.alertThreshold }

    // Recent Sales
    val recentSales = sales.take(5)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DCM Corporate Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        tonalElevation = 1.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_dcm_icon),
                            contentDescription = "Logo DCM",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DONSAEL (DCM)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "COMMUNICATION & MULTISERVICES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DonsaelTeal,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Belle-Vue, L'Asile, Nippes, Haïti",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Message of the Day Banner
        if (messageOfTheDay.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Annonce",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = messageOfTheDay,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Section Title: Performance Annuelle & Mensuelle
        item {
            Text(
                text = "Indicateurs Commerciaux & Financiers",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // KPI Grid Row 1 (Annual Revenue & Annual Profit)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Chiffre d'Affaires Annuel",
                    subtitle = "Hors ventes à crédit en cours",
                    amountHtg = annualRevenueHtg,
                    amountUsd = annualRevenueHtg / exchangeRate,
                    icon = Icons.Default.MonetizationOn,
                    color = DonsaelBlue,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Bénéfice Net Annuel",
                    subtitle = "Marge nette totale",
                    amountHtg = annualProfitHtg,
                    amountUsd = annualProfitHtg / exchangeRate,
                    icon = Icons.Default.TrendingUp,
                    color = DonsaelGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // KPI Grid Row 2 (Month Performance: Sales Count & Avg Cart)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Ventes du Mois",
                    subtitle = "${monthSales.size} transactions ce mois-ci",
                    amountHtg = monthRevenueHtg,
                    amountUsd = monthRevenueHtg / exchangeRate,
                    icon = Icons.Default.PointOfSale,
                    color = DonsaelTeal,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Panier Moyen (Mois)",
                    subtitle = "Moyenne par client",
                    amountHtg = avgCartHtg,
                    amountUsd = avgCartHtg / exchangeRate,
                    icon = Icons.Default.ShoppingCart,
                    color = DonsaelAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Stock Value Section (By Category)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory, contentDescription = "Stock", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Évaluation Totale du Stock",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "Taux: 1$ = $exchangeRate G",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.US, "%,.2f HTG", totalStockHtg),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.US, "Équiv. %,.2f USD", totalStockHtg / exchangeRate),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = DonsaelGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${products.size} Références",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DonsaelGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Detail by category
                    StockCategoryRow(
                        category = "Matériels Électroniques",
                        amountHtg = stockElectroniqueHtg,
                        amountUsd = stockElectroniqueHtg / exchangeRate
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StockCategoryRow(
                        category = "Appareils Électroménagers",
                        amountHtg = stockElectromenagerHtg,
                        amountUsd = stockElectromenagerHtg / exchangeRate
                    )
                    if (stockDiversHtg > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        StockCategoryRow(
                            category = "Articles & Divers",
                            amountHtg = stockDiversHtg,
                            amountUsd = stockDiversHtg / exchangeRate
                        )
                    }
                }
            }
        }

        // Client Credits Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, contentDescription = "Crédits", tint = DonsaelAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Crédits Clients en Cours",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "$debtorClients client(s) concerné(s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DonsaelAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.US, "%,.2f HTG", totalCreditRemainingHtg),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.US, "Équiv. %,.2f USD restant à recevoir", totalCreditRemainingHtg / exchangeRate),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = if (totalCreditRemainingHtg > 0) DonsaelAmber.copy(alpha = 0.15f) else DonsaelGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (totalCreditRemainingHtg > 0) "${pendingCredits.size} Crédits actifs" else "Aucun impayé",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalCreditRemainingHtg > 0) DonsaelAmber else DonsaelGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Low Stock Alerts (Critical <= 4 units)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Alerte", tint = DonsaelRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Alertes de Stock Faible (≤ 4 unités)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DonsaelRed
                    )
                }
                Surface(
                    color = DonsaelRed.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "${lowStockProducts.size}",
                        color = DonsaelRed,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (lowStockProducts.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tous les stocks sont à des niveaux optimaux (> 4 unités).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(lowStockProducts) { product ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "${product.category} • Emplacement : ${product.location}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Prix : ${product.sellingPrice} ${product.currency}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            color = DonsaelRed,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Reste : ${product.quantity}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recent Sales
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = "Ventes", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ventes Récentes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        if (recentSales.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aucune transaction enregistrée pour l'instant.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(recentSales) { sale ->
                val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = sale.reference,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${sdf.format(Date(sale.timestamp))} • Vendeur : ${sale.sellerFullName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Mode : ${sale.paymentMethod} (${sale.paymentType})",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%.2f %s", sale.totalAmount, sale.paymentCurrency),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (sale.paymentType == "CREDIT") DonsaelAmber else DonsaelGreen
                            )
                            Surface(
                                color = if (sale.paymentType == "CREDIT") DonsaelAmber.copy(alpha = 0.15f) else DonsaelGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (sale.paymentType == "CREDIT") "Crédit" else "Comptant",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sale.paymentType == "CREDIT") DonsaelAmber else DonsaelGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    subtitle: String,
    amountHtg: Double,
    amountUsd: Double,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = String.format(Locale.US, "%,.0f HTG", amountHtg),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = String.format(Locale.US, "%,.2f USD", amountUsd),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StockCategoryRow(
    category: String,
    amountHtg: Double,
    amountUsd: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = String.format(Locale.US, "%,.2f HTG", amountHtg),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = String.format(Locale.US, "%,.2f USD", amountUsd),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
