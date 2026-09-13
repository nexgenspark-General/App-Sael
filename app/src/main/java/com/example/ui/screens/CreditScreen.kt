package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SaleRecord
import com.example.ui.theme.DonsaelAmber
import com.example.ui.theme.DonsaelGreen
import com.example.ui.theme.DonsaelRed
import com.example.ui.theme.DonsaelTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CreditFilterStatus(val label: String) {
    ALL("Tous"),
    RECENT_7("Crédits récents (7j+)"),
    PAID("Déjà payés"),
    UNPAID("Non payés")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditScreen(
    creditSales: List<SaleRecord>,
    exchangeRate: Double,
    onPayCredit: (SaleRecord, Double, String, String, String?) -> Unit,
    onReprintReceipt: (SaleRecord) -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(CreditFilterStatus.ALL) }
    var payingSale by remember { mutableStateOf<SaleRecord?>(null) }

    val now = System.currentTimeMillis()
    val sevenDaysMillis = 7L * 24L * 3600L * 1000L

    val filteredCredits = remember(creditSales, searchQuery, activeFilter) {
        creditSales.filter { sale ->
            val matchSearch = searchQuery.isBlank() ||
                    (sale.customerName?.contains(searchQuery, ignoreCase = true) == true) ||
                    (sale.customerPhone?.contains(searchQuery, ignoreCase = true) == true) ||
                    sale.reference.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (activeFilter) {
                CreditFilterStatus.ALL -> true
                CreditFilterStatus.RECENT_7 -> (now - sale.timestamp) >= sevenDaysMillis
                CreditFilterStatus.PAID -> sale.creditStatus == "PAID" || sale.creditRemainingBalance <= 0.01
                CreditFilterStatus.UNPAID -> sale.creditStatus != "PAID" && sale.creditRemainingBalance > 0.01
            }

            matchSearch && matchFilter
        }
    }

    // Totals
    val totalUnpaidHtg = creditSales.filter { it.creditStatus != "PAID" }.sumOf {
        if (it.paymentCurrency == "HTG") it.creditRemainingBalance else it.creditRemainingBalance * exchangeRate
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("credit_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Suivi des Crédits Clients",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = String.format(Locale.US, "Solde global impayé : %,.2f HTG (%,.2f USD)", totalUnpaidHtg, totalUnpaidHtg / exchangeRate),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DonsaelAmber
                )
            }

            OutlinedButton(
                onClick = onExportPdf,
                modifier = Modifier.testTag("export_credits_pdf_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export PDF", fontSize = 11.5.sp)
            }
        }

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher par client, tél ou réf...") },
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
                .padding(horizontal = 14.dp)
                .testTag("credit_search_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Status Filter Chips (Tous, Récents 7+, Déjà payés, Non payés)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CreditFilterStatus.values().forEach { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { activeFilter = filter },
                    label = { Text(filter.label, fontSize = 11.sp) },
                    modifier = Modifier.testTag("credit_filter_${filter.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // List of Credits
        if (filteredCredits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun crédit correspondant.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredCredits, key = { it.id }) { sale ->
                    val isPaid = sale.creditStatus == "PAID" || sale.creditRemainingBalance <= 0.01
                    val isOverdue = !isPaid && sale.creditDueDate != null && sale.creditDueDate < now

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("credit_card_${sale.id}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = sale.customerName ?: "Client Inconnu",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Tél : ${sale.customerPhone ?: "-"} | ${sale.customerPhone2 ?: "-"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!sale.customerAddress.isNullOrBlank()) {
                                        Text(
                                            text = "Adresse : ${sale.customerAddress}",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = if (isPaid) DonsaelGreen.copy(alpha = 0.15f) else if (isOverdue) DonsaelRed.copy(alpha = 0.15f) else DonsaelAmber.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isPaid) "DÉJÀ PAYÉ" else if (isOverdue) "EN RETARD" else "NON PAYÉ",
                                        color = if (isPaid) DonsaelGreen else if (isOverdue) DonsaelRed else DonsaelAmber,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.5.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                            // Amounts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Montant total : ${String.format(Locale.US, "%.2f %s", sale.totalAmount, sale.paymentCurrency)}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Acompte versé : ${String.format(Locale.US, "%.2f %s", sale.creditDepositAmount, sale.paymentCurrency)}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Solde restant dû :",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2f %s", sale.creditRemainingBalance, sale.paymentCurrency),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isPaid) DonsaelGreen else DonsaelRed
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Due date & user
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val dueStr = if (sale.creditDueDate != null && sale.creditDueDate > 0) sdf.format(Date(sale.creditDueDate)) else "Non spécifiée"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Échéance : $dueStr • Opérateur : ${sale.sellerFullName}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row {
                                    if (!isPaid) {
                                        Button(
                                            onClick = { payingSale = sale },
                                            modifier = Modifier.testTag("pay_credit_button_${sale.id}")
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = "Régler", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Régler", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment settlement dialog
    if (payingSale != null) {
        CreditRepaymentDialog(
            sale = payingSale!!,
            onDismiss = { payingSale = null },
            onConfirm = { amount, method, note ->
                onPayCredit(payingSale!!, amount, payingSale!!.paymentCurrency, method, note)
                payingSale = null
            }
        )
    }
}

@Composable
fun CreditRepaymentDialog(
    sale: SaleRecord,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, note: String?) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", sale.creditRemainingBalance)) }
    var selectedMethod by remember { mutableStateOf("Espèces") }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Règlement du Crédit - ${sale.customerName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Solde restant dû : ${String.format(Locale.US, "%.2f %s", sale.creditRemainingBalance, sale.paymentCurrency)}",
                    fontWeight = FontWeight.Bold,
                    color = DonsaelAmber,
                    fontSize = 13.5.sp
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Montant remboursé (${sale.paymentCurrency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("credit_pay_amount_input")
                )

                Column {
                    Text(text = "Mode de paiement :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    PaymentMethodDropdown(
                        selectedMethod = selectedMethod,
                        onSelectMethod = { selectedMethod = it }
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note ou référence (Facultatif)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = DonsaelRed, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMsg = "Veuillez entrer un montant valide supérieur à 0."
                        return@Button
                    }
                    onConfirm(amt, selectedMethod, note.ifBlank { null })
                },
                modifier = Modifier.testTag("confirm_repayment_button")
            ) {
                Text("Enregistrer le Versement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
