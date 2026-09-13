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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.data.Product
import com.example.data.UserAccount
import com.example.ui.theme.DonsaelAmber
import com.example.ui.theme.DonsaelBlue
import com.example.ui.theme.DonsaelGreen
import com.example.ui.theme.DonsaelRed
import com.example.ui.theme.DonsaelTeal
import java.util.Locale

enum class QuickStockFilter(val label: String) {
    ALL("Tous les produits"),
    LOW_STOCK("Stock faible"),
    ELECTRONIC_LOW("Électroniques — Stock faible"),
    APPLIANCE_LOW("Électroménagers — Stock faible")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    products: List<Product>,
    trashProducts: List<Product>,
    currentUser: UserAccount?,
    onAddProduct: (Product) -> Unit,
    onUpdateProduct: (Product) -> Unit,
    onSoftDeleteProduct: (Product) -> Unit,
    onRestoreProduct: (Product) -> Unit,
    onPermanentDeleteProduct: (Product) -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCorbeilleMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeQuickFilter by remember { mutableStateOf(QuickStockFilter.ALL) }
    var selectedCategoryFilter by remember { mutableStateOf("Toutes catégories") }

    // Dialog state
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("Toutes catégories", "Appareil électronique", "Appareil électroménager", "Accessoires")

    val activeList = if (isCorbeilleMode) trashProducts else products

    val filteredList = remember(activeList, searchQuery, activeQuickFilter, selectedCategoryFilter, isCorbeilleMode) {
        activeList.filter { p ->
            val matchSearch = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategoryFilter == "Toutes catégories" || p.category.equals(selectedCategoryFilter, ignoreCase = true)

            val matchQuick = when (activeQuickFilter) {
                QuickStockFilter.ALL -> true
                QuickStockFilter.LOW_STOCK -> p.quantity <= p.alertThreshold
                QuickStockFilter.ELECTRONIC_LOW -> p.quantity <= p.alertThreshold && p.category.contains("électronique", ignoreCase = true)
                QuickStockFilter.APPLIANCE_LOW -> p.quantity <= p.alertThreshold && p.category.contains("électroménager", ignoreCase = true)
            }

            matchSearch && matchCat && matchQuick
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("inventory_screen")
    ) {
        // Top action bar: Title, Corbeille switch, Export PDF, Add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isCorbeilleMode) "Corbeille des Produits" else "Gestion de l'Inventaire",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${filteredList.size} article(s) affiché(s)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // PDF Export button
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier.testTag("export_inventory_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PDF", fontSize = 11.5.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Corbeille toggle button
                Surface(
                    color = if (isCorbeilleMode) DonsaelAmber.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isCorbeilleMode = !isCorbeilleMode }
                        .testTag("toggle_trash_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Corbeille",
                            tint = if (isCorbeilleMode) DonsaelAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        if (trashProducts.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${trashProducts.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorbeilleMode) DonsaelAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!isCorbeilleMode) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_product_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter", fontSize = 12.sp)
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher par nom d'article...") },
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
                .testTag("inventory_search_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Filters Pills (Page 6 specification: Tous les produits, Stock faible, Appareils électroniques — Stock faible, Appareils électroménagers — Stock faible)
        if (!isCorbeilleMode) {
            ScrollableTabRow(
                selectedTabIndex = activeQuickFilter.ordinal,
                edgePadding = 14.dp,
                divider = {},
                indicator = {}
            ) {
                QuickStockFilter.values().forEach { filter ->
                    val isSelected = activeQuickFilter == filter
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { activeQuickFilter = filter }
                            .testTag("quick_filter_${filter.name}")
                    ) {
                        Text(
                            text = filter.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Products List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCorbeilleMode) "La corbeille est vide." else "Aucun produit ne correspond aux filtres appliqués.",
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
                items(filteredList, key = { it.id }) { product ->
                    val isLowStock = product.quantity <= product.alertThreshold

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("product_card_${product.id}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (isLowStock) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = DonsaelRed,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Stock faible",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "ALERTE STOCK",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Actions (Edit / Soft Delete or Restore / Permanent Delete)
                                Row {
                                    if (!isCorbeilleMode) {
                                        IconButton(
                                            onClick = { editingProduct = product },
                                            modifier = Modifier.size(32.dp).testTag("edit_product_${product.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onSoftDeleteProduct(product) },
                                            modifier = Modifier.size(32.dp).testTag("delete_product_${product.id}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = DonsaelRed, modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { onRestoreProduct(product) },
                                            modifier = Modifier.size(32.dp).testTag("restore_product_${product.id}")
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = "Restaurer", tint = DonsaelGreen, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onPermanentDeleteProduct(product) },
                                            modifier = Modifier.size(32.dp).testTag("permanent_delete_${product.id}")
                                        ) {
                                            Icon(Icons.Default.DeleteForever, contentDescription = "Supprimer définitivement", tint = DonsaelRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${product.category} • Emplacement : ${product.location}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Prix d'achat : ${product.purchasePrice} ${product.currency}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Prix de vente : ${product.sellingPrice} ${product.currency}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Surface(
                                    color = if (isLowStock) DonsaelRed.copy(alpha = 0.15f) else DonsaelGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Stock disponible : ${product.quantity}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLowStock) DonsaelRed else DonsaelGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Product Modal
    if (showAddDialog) {
        ProductFormDialog(
            product = null,
            onDismiss = { showAddDialog = false },
            onSave = { newProd ->
                onAddProduct(newProd)
                showAddDialog = false
            }
        )
    }

    // Edit Product Modal
    if (editingProduct != null) {
        ProductFormDialog(
            product = editingProduct,
            onDismiss = { editingProduct = null },
            onSave = { updatedProd ->
                onUpdateProduct(updatedProd)
                editingProduct = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Appareil électronique") }
    var quantityText by remember { mutableStateOf(product?.quantity?.toString() ?: "5") }
    var currency by remember { mutableStateOf(product?.currency ?: "HTG") }
    var purchasePriceText by remember { mutableStateOf(product?.purchasePrice?.toString() ?: "1000") }
    var sellingPriceText by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "1500") }
    var location by remember { mutableStateOf(product?.location ?: "Magasin Principal") }
    var thresholdText by remember { mutableStateOf(product?.alertThreshold?.toString() ?: "4") }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (product == null) "Ajouter un Article" else "Modifier le Produit",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du produit *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Catégorie") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Emplacement") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantité en stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_quantity_input")
                    )

                    // Currency Toggle (HTG / USD)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Devise :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            CurrencyButton(text = "HTG", isSelected = currency == "HTG", onClick = { currency = "HTG" })
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyButton(text = "USD", isSelected = currency == "USD", onClick = { currency = "USD" })
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePriceText,
                        onValueChange = { purchasePriceText = it },
                        label = { Text("Prix d'achat") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it },
                        label = { Text("Prix de vente *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_selling_price_input")
                    )
                }

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = { Text("Seuil d'alerte critique (Défaut: 4)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    if (name.isBlank()) {
                        errorMsg = "Le nom du produit est requis."
                        return@Button
                    }
                    val qty = quantityText.toIntOrNull() ?: 0
                    val pPrice = purchasePriceText.toDoubleOrNull() ?: 0.0
                    val sPrice = sellingPriceText.toDoubleOrNull() ?: 0.0
                    val thresh = thresholdText.toIntOrNull() ?: 4

                    val saved = (product ?: Product(name = name, category = category, quantity = qty, currency = currency, purchasePrice = pPrice, sellingPrice = sPrice, alertThreshold = thresh, location = location)).copy(
                        name = name.trim(),
                        category = category.trim(),
                        quantity = qty,
                        currency = currency,
                        purchasePrice = pPrice,
                        sellingPrice = sPrice,
                        alertThreshold = thresh,
                        location = location.trim()
                    )
                    onSave(saved)
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
