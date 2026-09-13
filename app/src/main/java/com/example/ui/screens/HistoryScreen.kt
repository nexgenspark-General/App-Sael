package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.data.AuditLogRecord
import com.example.data.SaleRecord
import com.example.ui.theme.DonsaelAmber
import com.example.ui.theme.DonsaelGreen
import com.example.ui.theme.DonsaelTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    sales: List<SaleRecord>,
    auditLogs: List<AuditLogRecord>,
    onReprintReceipt: (SaleRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Ventes Récentes, 1: Journal d'Audit
    var searchQuery by remember { mutableStateOf("") }

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    val filteredSales = remember(sales, searchQuery) {
        sales.filter { sale ->
            searchQuery.isBlank() ||
                    sale.reference.contains(searchQuery, ignoreCase = true) ||
                    (sale.customerName?.contains(searchQuery, ignoreCase = true) == true) ||
                    sale.sellerFullName.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredLogs = remember(auditLogs, searchQuery) {
        auditLogs.filter { log ->
            searchQuery.isBlank() ||
                    log.actionType.contains(searchQuery, ignoreCase = true) ||
                    log.username.contains(searchQuery, ignoreCase = true) ||
                    log.details.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        // Tab row: Ventes vs Audit
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Historique des Ventes (${sales.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Journal d'Audit (${auditLogs.size})", fontWeight = FontWeight.Bold) }
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (selectedTab == 0) "Rechercher par référence, client, vendeur..." else "Rechercher une action, utilisateur...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Recherche") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Effacer")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("history_search_input")
        )

        if (selectedTab == 0) {
            // Sales History
            if (filteredSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune vente enregistrée.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredSales, key = { it.id }) { sale ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().testTag("history_sale_${sale.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = sale.reference, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "${sdf.format(Date(sale.timestamp))} • Par ${sale.sellerFullName}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        color = if (sale.paymentType == "CREDIT") DonsaelAmber.copy(alpha = 0.15f) else DonsaelGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (sale.paymentType == "CREDIT") "CRÉDIT" else "COMPTANT",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (sale.paymentType == "CREDIT") DonsaelAmber else DonsaelGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Montant : ${String.format(Locale.US, "%.2f %s", sale.totalAmount, sale.paymentCurrency)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Règlement : ${sale.paymentMethod}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (sale.paymentType == "CREDIT") {
                                            Text(
                                                text = "Client : ${sale.customerName ?: "-"} (Reste : ${String.format(Locale.US, "%.2f %s", sale.creditRemainingBalance, sale.paymentCurrency)})",
                                                fontSize = 10.5.sp,
                                                color = DonsaelAmber
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onReprintReceipt(sale) },
                                        modifier = Modifier.testTag("reprint_sale_${sale.id}")
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = "Réimprimer", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reçu", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Audit Log
            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun événement dans le journal d'audit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().testTag("audit_log_${log.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = "Audit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = log.actionType, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    }
                                    Text(
                                        text = sdf.format(Date(log.timestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(text = log.details, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "Auteur : ${log.username}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
