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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CartItem
import com.example.data.Product
import com.example.data.SaleItemRecord
import com.example.data.SaleRecord
import com.example.ui.theme.DonsaelAmber
import com.example.ui.theme.DonsaelBlue
import com.example.ui.theme.DonsaelGreen
import com.example.ui.theme.DonsaelRed
import com.example.ui.theme.DonsaelTeal
import java.util.Locale

val PAYMENT_METHODS = listOf(
    "Espèces",
    "MonCash",
    "NatCash",
    "KPLAN",
    "BNC",
    "Unibank",
    "Sogebank",
    "Capital Bank",
    "Zelle",
    "Cash App",
    "Western Union",
    "MoneyGram"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    products: List<Product>,
    cart: List<CartItem>,
    currency: String,
    exchangeRate: Double,
    completedSale: Pair<SaleRecord, List<SaleItemRecord>>?,
    onSetCurrency: (String) -> Unit,
    onAddToCart: (Product) -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveFromCart: (Long) -> Unit,
    onClearCart: () -> Unit,
    onFinalizeSale: (
        paymentType: String,
        paymentMethod: String,
        amountTendered: Double,
        customerName: String?,
        customerPhone: String?,
        customerPhone2: String?,
        customerAddress: String?,
        creditDepositAmount: Double,
        creditDepositMethod: String?,
        creditRemainingBalance: Double,
        creditDueDate: Long?,
        onSuccess: (SaleRecord) -> Unit
    ) -> Unit,
    onDismissCompletedSale: () -> Unit,
    onReprintReceipt: (SaleRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var showCheckoutSheet by remember { mutableStateOf(false) }

    val categories = listOf("Tous", "Appareil électronique", "Appareil électroménager", "Accessoires")

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchCat = selectedCategory == "Tous" || p.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
    }

    val cartTotal = remember(cart, currency, exchangeRate) {
        cart.sumOf { it.quantity * it.unitPriceInSaleCurrency }
    }

    Box(modifier = modifier.fillMaxSize().testTag("pos_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
            // Top Row: Currency switch & Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Currency Toggle (HTG / USD)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("pos_currency_selector")
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        CurrencyButton(
                            text = "Gourdes (HTG)",
                            isSelected = currency == "HTG",
                            onClick = { onSetCurrency("HTG") }
                        )
                        CurrencyButton(
                            text = "Dollars (USD)",
                            isSelected = currency == "USD",
                            onClick = { onSetCurrency("USD") }
                        )
                    }
                }

                Text(
                    text = "1 USD = $exchangeRate HTG",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher un produit à encaisser...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Recherche") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("pos_search_input")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Categories horizontal pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.5.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Product Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    val isOutOfStock = product.quantity <= 0
                    val isLowStock = product.quantity in 1..product.alertThreshold
                    val qtyInCart = cart.find { it.product.id == product.id }?.quantity ?: 0

                    // Compute price in currently selected POS currency
                    val unitPriceInCurrency = if (product.currency == currency) {
                        product.sellingPrice
                    } else if (product.currency == "USD" && currency == "HTG") {
                        product.sellingPrice * exchangeRate
                    } else {
                        product.sellingPrice / exchangeRate
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isOutOfStock) { onAddToCart(product) }
                            .testTag("pos_product_${product.id}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Category & Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (product.category.contains("électronique")) "Électronique" else "Électroménager",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isLowStock) {
                                    Surface(
                                        color = DonsaelRed,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "≤4",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = product.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Price in current currency
                            Text(
                                text = String.format(Locale.US, "%.2f %s", unitPriceInCurrency, currency),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Stock remaining
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isOutOfStock) "Épuisé" else "Stock: ${product.quantity}",
                                    fontSize = 10.sp,
                                    color = if (isOutOfStock) DonsaelRed else if (isLowStock) DonsaelAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (qtyInCart > 0) {
                                    Surface(
                                        color = DonsaelTeal,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "$qtyInCart",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Bar / Cart Summary
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${cart.sumOf { it.quantity }} article(s) au panier",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f %s", cartTotal, currency),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (cart.isNotEmpty()) {
                        IconButton(
                            onClick = onClearCart,
                            modifier = Modifier.size(36.dp).testTag("clear_cart_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Vider le panier", tint = DonsaelRed)
                        }
                    }

                    Button(
                        onClick = { showCheckoutSheet = true },
                        enabled = cart.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_checkout_sheet_button")
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Encaisser", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Encaisser", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Checkout Bottom Sheet / Modal
        if (showCheckoutSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCheckoutSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                CheckoutSheetContent(
                    cart = cart,
                    currency = currency,
                    cartTotal = cartTotal,
                    exchangeRate = exchangeRate,
                    onUpdateQuantity = onUpdateQuantity,
                    onRemoveFromCart = onRemoveFromCart,
                    onFinalize = { pType, pMethod, amtTendered, cName, cPhone, cPhone2, cAddress, cDeposit, cDepMethod, cRem, cDue ->
                        onFinalizeSale(
                            pType, pMethod, amtTendered, cName, cPhone, cPhone2, cAddress, cDeposit, cDepMethod, cRem, cDue
                        ) {
                            showCheckoutSheet = false
                        }
                    },
                    onClose = { showCheckoutSheet = false }
                )
            }
        }

        // Completed Sale Receipt Dialog
        if (completedSale != null) {
            val sale = completedSale.first
            val items = completedSale.second
            ReceiptDialog(
                sale = sale,
                items = items,
                onDismiss = onDismissCompletedSale,
                onReprint = { onReprintReceipt(sale) }
            )
        }
    }
}

@Composable
fun CurrencyButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CheckoutSheetContent(
    cart: List<CartItem>,
    currency: String,
    cartTotal: Double,
    exchangeRate: Double,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveFromCart: (Long) -> Unit,
    onFinalize: (
        paymentType: String,
        paymentMethod: String,
        amountTendered: Double,
        customerName: String?,
        customerPhone: String?,
        customerPhone2: String?,
        customerAddress: String?,
        creditDepositAmount: Double,
        creditDepositMethod: String?,
        creditRemainingBalance: Double,
        creditDueDate: Long?
    ) -> Unit,
    onClose: () -> Unit
) {
    var paymentType by remember { mutableStateOf("COMPTANT") } // "COMPTANT" or "CREDIT"
    var selectedMethod by remember { mutableStateOf("Espèces") }
    var tenderedText by remember(cartTotal) { mutableStateOf(String.format(Locale.US, "%.2f", cartTotal)) }

    // Credit fields (as per page 7 specs)
    var customerName by remember { mutableStateOf("") }
    var customerPhone1 by remember { mutableStateOf("") }
    var customerPhone2 by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var creditDepositText by remember { mutableStateOf("0") }
    var creditDepositMethod by remember { mutableStateOf("Espèces") }
    var creditDueDateDays by remember { mutableStateOf("30") }

    val tenderedAmount = tenderedText.toDoubleOrNull() ?: 0.0
    val changeGiven = (tenderedAmount - cartTotal).coerceAtLeast(0.0)

    val creditDeposit = creditDepositText.toDoubleOrNull() ?: 0.0
    val creditRemaining = (cartTotal - creditDeposit).coerceAtLeast(0.0)

    var formError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("checkout_sheet_content")
    ) {
        // Title & Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Finalisation de la Vente",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Fermer")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Cart Items List
        Text(text = "Articles commandés (${cart.size}) :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))

        cart.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.product.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = String.format(Locale.US, "@ %.2f %s", item.unitPriceInSaleCurrency, currency),
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUpdateQuantity(item.product.id, -1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Moins", modifier = Modifier.size(14.dp))
                    }
                    Text(text = "${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    IconButton(
                        onClick = { onUpdateQuantity(item.product.id, 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = String.format(Locale.US, "%.2f %s", item.quantity * item.unitPriceInSaleCurrency, currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        modifier = Modifier.width(75.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "MONTANT TOTAL :", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = String.format(Locale.US, "%.2f %s", cartTotal, currency),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Payment Type: Comptant vs Crédit
        Text(text = "Type de règlement :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (paymentType == "COMPTANT") DonsaelGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { paymentType = "COMPTANT" }
                    .testTag("select_payment_comptant")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paymentType == "COMPTANT",
                        onClick = { paymentType = "COMPTANT" }
                    )
                    Text(text = "Comptant (Cash)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }

            Surface(
                color = if (paymentType == "CREDIT") DonsaelAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { paymentType = "CREDIT" }
                    .testTag("select_payment_credit")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paymentType == "CREDIT",
                        onClick = { paymentType = "CREDIT" }
                    )
                    Text(text = "Crédit Client", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scenario 1: Comptant
        if (paymentType == "COMPTANT") {
            Text(text = "Moyen de paiement comptant :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))

            PaymentMethodDropdown(
                selectedMethod = selectedMethod,
                onSelectMethod = { selectedMethod = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Tendered & Change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tenderedText,
                    onValueChange = { tenderedText = it },
                    label = { Text("Montant remis par le client ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("tendered_amount_input")
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Monnaie à rendre :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.2f %s", changeGiven, currency),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (changeGiven >= 0) DonsaelGreen else DonsaelRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Optional client info for Comptant
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nom du client (Facultatif)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = customerPhone1,
                    onValueChange = { customerPhone1 = it },
                    label = { Text("Téléphone (Facultatif)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Scenario 2: Crédit (Page 7 specification mandates: Nom du client, 2 numéros obligatoires, adresse, acompte, moyen de l'acompte, date prévue)
            Surface(
                color = DonsaelAmber.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Vente à crédit : Le nom, 2 numéros de téléphone et l'adresse sont obligatoires pour la constitution du dossier et du reçu officiel.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Nom complet du client *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("credit_customer_name_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customerPhone1,
                    onValueChange = { customerPhone1 = it },
                    label = { Text("Tél 1 (Obligatoire) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("credit_phone1_input")
                )
                OutlinedTextField(
                    value = customerPhone2,
                    onValueChange = { customerPhone2 = it },
                    label = { Text("Tél 2 (Obligatoire) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("credit_phone2_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customerAddress,
                onValueChange = { customerAddress = it },
                label = { Text("Adresse de résidence (Obligatoire) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("credit_address_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Acompte & Moyen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = creditDepositText,
                    onValueChange = { creditDepositText = it },
                    label = { Text("Acompte versé ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("credit_deposit_input")
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Moyen de l'acompte :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    PaymentMethodDropdown(
                        selectedMethod = creditDepositMethod,
                        onSelectMethod = { creditDepositMethod = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Due date in days
            OutlinedTextField(
                value = creditDueDateDays,
                onValueChange = { creditDueDateDays = it },
                label = { Text("Délai de remboursement prévu (jours)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Remaining balance display
            Surface(
                color = DonsaelAmber.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Solde restant dû :", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = String.format(Locale.US, "%.2f %s", creditRemaining, currency),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = DonsaelAmber
                    )
                }
            }
        }

        if (formError != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formError!!,
                color = DonsaelRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Validation button
        Button(
            onClick = {
                formError = null
                if (paymentType == "COMPTANT") {
                    if (tenderedAmount < cartTotal) {
                        formError = "Le montant remis doit être supérieur ou égal au montant total !"
                        return@Button
                    }
                    onFinalize(
                        "COMPTANT",
                        selectedMethod,
                        tenderedAmount,
                        customerName.ifBlank { null },
                        customerPhone1.ifBlank { null },
                        null,
                        null,
                        0.0,
                        null,
                        0.0,
                        null
                    )
                } else {
                    // Credit validation
                    if (customerName.isBlank()) {
                        formError = "Le nom du client est obligatoire pour une vente à crédit !"
                        return@Button
                    }
                    if (customerPhone1.isBlank() || customerPhone2.isBlank()) {
                        formError = "Les 2 numéros de téléphone sont obligatoires pour un crédit !"
                        return@Button
                    }
                    if (customerAddress.isBlank()) {
                        formError = "L'adresse du client est obligatoire pour un crédit !"
                        return@Button
                    }
                    val days = creditDueDateDays.toIntOrNull() ?: 30
                    val dueTimestamp = System.currentTimeMillis() + (days.toLong() * 24L * 3600L * 1000L)

                    onFinalize(
                        "CREDIT",
                        selectedMethod,
                        creditDeposit,
                        customerName,
                        customerPhone1,
                        customerPhone2,
                        customerAddress,
                        creditDeposit,
                        creditDepositMethod,
                        creditRemaining,
                        dueTimestamp
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("finalize_sale_button")
        ) {
            Text(
                text = if (paymentType == "COMPTANT") "Valider l'Encaissement & Imprimer" else "Valider le Crédit & Générer le Contrat",
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp
            )
        }
    }
}

@Composable
fun PaymentMethodDropdown(
    selectedMethod: String,
    onSelectMethod: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = selectedMethod, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Sélectionner moyen")
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PAYMENT_METHODS.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method, fontSize = 13.sp) },
                    onClick = {
                        onSelectMethod(method)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    sale: SaleRecord,
    items: List<SaleItemRecord>,
    onDismiss: () -> Unit,
    onReprint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = "Ticket", tint = DonsaelGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Reçu Officiel - ${sale.reference}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "DONSAEL COMMUNICATION ET MULTI-SERVICES", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Belle-Vue, L'Asile | Tél: 3753-2670 / 3224-4430", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Opérateur : ${sale.sellerFullName}", fontSize = 11.sp)
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                items.forEach { itm ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "${itm.quantity}x ${itm.productName}", fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                        Text(
                            text = String.format(Locale.US, "%.2f %s", itm.quantity * itm.unitPriceConverted, sale.paymentCurrency),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "TOTAL :", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = String.format(Locale.US, "%.2f %s", sale.totalAmount, sale.paymentCurrency),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (sale.paymentType == "COMPTANT") {
                    Text(text = "Mode : ${sale.paymentMethod}", fontSize = 11.sp)
                    Text(text = String.format(Locale.US, "Montant remis : %.2f %s", sale.amountTendered, sale.paymentCurrency), fontSize = 11.sp)
                    Text(text = String.format(Locale.US, "Monnaie rendue : %.2f %s", sale.changeGiven, sale.paymentCurrency), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = "Client : ${sale.customerName ?: "-"}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Tél : ${sale.customerPhone ?: "-"} / ${sale.customerPhone2 ?: "-"}", fontSize = 10.5.sp)
                    Text(text = "Adresse : ${sale.customerAddress ?: "-"}", fontSize = 10.5.sp)
                    Text(text = String.format(Locale.US, "Acompte versé : %.2f %s", sale.creditDepositAmount, sale.paymentCurrency), fontSize = 11.sp)
                    Text(
                        text = String.format(Locale.US, "Solde restant dû : %.2f %s", sale.creditRemainingBalance, sale.paymentCurrency),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DonsaelAmber
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Conçu et développé par NexGen Spark", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Belle-Vue, L'Asile | support@nexgns.net", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onReprint,
                modifier = Modifier.testTag("print_receipt_button")
            ) {
                Icon(Icons.Default.Print, contentDescription = "Imprimer", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Imprimer / Partager PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
