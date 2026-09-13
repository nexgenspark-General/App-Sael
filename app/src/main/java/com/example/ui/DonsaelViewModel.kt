package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AuditLogRecord
import com.example.data.CartItem
import com.example.data.ExportedDocumentRecord
import com.example.data.LoginResult
import com.example.data.PosRepository
import com.example.data.Product
import com.example.data.SaleItemRecord
import com.example.data.SaleRecord
import com.example.data.UserAccount
import com.example.pdf.PdfReportService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class ScreenTab(val title: String) {
    DASHBOARD("Tableau de bord"),
    POS("Caisse / POS"),
    INVENTORY("Inventaire"),
    CREDITS("Crédit Client"),
    SELLER_MANAGEMENT("Vendeur"),
    REPORTS("Rapports"),
    HISTORY("Historique"),
    SETTINGS("Paramètres"),
    DOWNLOADS("Téléchargements")
}

data class ToastNotification(
    val message: String,
    val isError: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

class DonsaelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = PosRepository(database)
    val pdfService = PdfReportService(application, repository)

    // Current Device Identifier (ensures single device per account)
    val deviceId: String = UUID.randomUUID().toString().take(8)

    // Auth State
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    // Login Conflict Modal State
    private val _deviceConflictMessage = MutableStateFlow<String?>(null)
    val deviceConflictMessage: StateFlow<String?> = _deviceConflictMessage.asStateFlow()

    // Navigation & UI State
    private val _activeTab = MutableStateFlow(ScreenTab.DASHBOARD)
    val activeTab: StateFlow<ScreenTab> = _activeTab.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _exchangeRate = MutableStateFlow(132.0)
    val exchangeRate: StateFlow<Double> = _exchangeRate.asStateFlow()

    private val _paperWidthMm = MutableStateFlow(80)
    val paperWidthMm: StateFlow<Int> = _paperWidthMm.asStateFlow()

    private val _messageOfTheDay = MutableStateFlow("Bienvenue chez DONSAEL COMMUNICATION ET MULTI-SERVICES !")
    val messageOfTheDay: StateFlow<String> = _messageOfTheDay.asStateFlow()

    private val _toast = MutableStateFlow<ToastNotification?>(null)
    val toast: StateFlow<ToastNotification?> = _toast.asStateFlow()

    // Data Streams from Room
    val products: StateFlow<List<Product>> = repository.activeProducts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val trashProducts: StateFlow<List<Product>> = repository.trashProducts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val sales: StateFlow<List<SaleRecord>> = repository.allSales.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val creditSales: StateFlow<List<SaleRecord>> = repository.creditSales.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val auditLogs: StateFlow<List<AuditLogRecord>> = repository.allLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val sellerAccount: StateFlow<UserAccount?> = repository.sellerUser.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val exportedDocs: StateFlow<List<ExportedDocumentRecord>> = repository.allDocuments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // POS Cart
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _posCurrency = MutableStateFlow("HTG") // "HTG" or "USD"
    val posCurrency: StateFlow<String> = _posCurrency.asStateFlow()

    // Last completed sale receipt
    private val _completedSale = MutableStateFlow<Pair<SaleRecord, List<SaleItemRecord>>?>(null)
    val completedSale: StateFlow<Pair<SaleRecord, List<SaleItemRecord>>?> = _completedSale.asStateFlow()

    init {
        // Observe configs to initialize settings
        viewModelScope.launch {
            repository.allConfigs.collect { configs ->
                for (c in configs) {
                    when (c.key) {
                        "exchange_rate_usd_to_htg" -> c.value.toDoubleOrNull()?.let { _exchangeRate.value = it }
                        "receipt_paper_width_mm" -> c.value.toIntOrNull()?.let { _paperWidthMm.value = it }
                        "message_of_the_day" -> _messageOfTheDay.value = c.value
                        "is_online" -> _isOnline.value = c.value == "true"
                    }
                }
            }
        }
    }

    fun showToast(message: String, isError: Boolean = false) {
        _toast.value = ToastNotification(message, isError)
    }

    fun clearToast() {
        _toast.value = null
    }

    fun dismissDeviceConflict() {
        _deviceConflictMessage.value = null
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleOnlineStatus() {
        val newStatus = !_isOnline.value
        _isOnline.value = newStatus
        viewModelScope.launch {
            repository.updateConfig("is_online", newStatus.toString(), _currentUser.value?.username ?: "Système")
            showToast(if (newStatus) "Mode En ligne activé 🟢" else "Mode Hors ligne activé 🔴 (Synchronisation en attente)")
        }
    }

    fun selectTab(tab: ScreenTab) {
        // Check permissions if user is SELLER
        val user = _currentUser.value
        if (user != null && user.role == "SELLER") {
            when (tab) {
                ScreenTab.POS -> if (!user.canAccessPos) {
                    showToast("Accès à la Caisse refusé par l'Administrateur", isError = true)
                    return
                }
                ScreenTab.INVENTORY -> if (!user.canAccessInventory) {
                    showToast("Accès à l'Inventaire refusé par l'Administrateur", isError = true)
                    return
                }
                ScreenTab.REPORTS -> if (!user.canAccessReports) {
                    showToast("Accès aux Rapports refusé par l'Administrateur", isError = true)
                    return
                }
                ScreenTab.CREDITS -> if (!user.canAccessCredits) {
                    showToast("Accès aux Crédits refusé par l'Administrateur", isError = true)
                    return
                }
                ScreenTab.HISTORY -> if (!user.canAccessHistory) {
                    showToast("Accès à l'Historique refusé par l'Administrateur", isError = true)
                    return
                }
                ScreenTab.SELLER_MANAGEMENT -> {
                    showToast("Ce module est réservé à l'Administrateur", isError = true)
                    return
                }
                else -> {}
            }
        }
        _activeTab.value = tab
    }

    // Authentication
    fun login(username: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = repository.login(username, pass, deviceId)) {
                is LoginResult.Success -> {
                    _currentUser.value = result.user
                    _deviceConflictMessage.value = null
                    _activeTab.value = ScreenTab.DASHBOARD
                    showToast("Connexion réussie ! Bienvenue ${result.user.fullName}")
                    onSuccess()
                }
                is LoginResult.DeviceConflict -> {
                    _deviceConflictMessage.value = result.message
                }
                is LoginResult.Blocked -> {
                    showToast("Compte suspendu : ${result.reason}", isError = true)
                }
                is LoginResult.Error -> {
                    showToast(result.message, isError = true)
                }
            }
        }
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logout(user.id)
                _currentUser.value = null
                _cart.value = emptyList()
                _completedSale.value = null
                showToast("Déconnexion effectuée")
            }
        }
    }

    // Exchange Rate
    fun updateExchangeRate(newRate: Double) {
        val user = _currentUser.value ?: return
        if (user.role == "SELLER" && !user.canModifyExchangeRate) {
            showToast("Vous n'avez pas l'autorisation de modifier le taux de change.", isError = true)
            return
        }
        _exchangeRate.value = newRate
        viewModelScope.launch {
            repository.updateConfig("exchange_rate_usd_to_htg", newRate.toString(), user.username)
            // Recompute cart prices
            updateCartCurrency(_posCurrency.value)
            showToast("Taux de change mis à jour : 1 USD = $newRate HTG")
        }
    }

    fun setPaperWidthMm(widthMm: Int) {
        _paperWidthMm.value = widthMm
        viewModelScope.launch {
            repository.updateConfig("receipt_paper_width_mm", widthMm.toString(), _currentUser.value?.username ?: "Admin")
            showToast("Format papier réglé à $widthMm mm")
        }
    }

    fun setMessageOfTheDay(msg: String) {
        _messageOfTheDay.value = msg
        viewModelScope.launch {
            repository.updateConfig("message_of_the_day", msg, _currentUser.value?.username ?: "Admin")
            showToast("Message du jour actualisé")
        }
    }

    // POS Cart Operations
    fun setPosCurrency(currency: String) {
        _posCurrency.value = currency
        updateCartCurrency(currency)
    }

    private fun updateCartCurrency(currency: String) {
        val rate = _exchangeRate.value
        _cart.value = _cart.value.map { item ->
            val unitConverted = if (item.product.currency == currency) {
                item.product.sellingPrice
            } else if (item.product.currency == "USD" && currency == "HTG") {
                item.product.sellingPrice * rate
            } else {
                item.product.sellingPrice / rate
            }
            item.copy(unitPriceInSaleCurrency = unitConverted)
        }
    }

    fun addToCart(product: Product) {
        val currentCart = _cart.value.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
        val currentQtyInCart = if (existingIndex >= 0) currentCart[existingIndex].quantity else 0

        // Real-time stock check
        if (currentQtyInCart + 1 > product.quantity) {
            showToast("Stock insuffisant ! Quantité disponible : ${product.quantity}", isError = true)
            return
        }

        val rate = _exchangeRate.value
        val curr = _posCurrency.value
        val unitConverted = if (product.currency == curr) {
            product.sellingPrice
        } else if (product.currency == "USD" && curr == "HTG") {
            product.sellingPrice * rate
        } else {
            product.sellingPrice / rate
        }

        if (existingIndex >= 0) {
            currentCart[existingIndex] = currentCart[existingIndex].copy(quantity = currentQtyInCart + 1)
        } else {
            currentCart.add(CartItem(product, 1, unitConverted))
        }
        _cart.value = currentCart
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        val currentCart = _cart.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = currentCart[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                currentCart.removeAt(index)
            } else {
                // Stock limit check
                if (newQty > item.product.quantity) {
                    showToast("Stock insuffisant ! Maximum disponible : ${item.product.quantity}", isError = true)
                    return
                }
                currentCart[index] = item.copy(quantity = newQty)
            }
            _cart.value = currentCart
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun finalizeSale(
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
        onComplete: (SaleRecord) -> Unit
    ) {
        val user = _currentUser.value ?: return
        val currentItems = _cart.value
        if (currentItems.isEmpty()) {
            showToast("Le panier est vide", isError = true)
            return
        }

        val curr = _posCurrency.value
        val rate = _exchangeRate.value
        val total = currentItems.sumOf { it.quantity * it.unitPriceInSaleCurrency }
        val changeGiven = (amountTendered - total).coerceAtLeast(0.0)

        viewModelScope.launch {
            try {
                val sale = repository.processSale(
                    items = currentItems,
                    paymentType = paymentType,
                    paymentMethod = paymentMethod,
                    paymentCurrency = curr,
                    exchangeRate = rate,
                    totalAmount = total,
                    amountTendered = amountTendered,
                    changeGiven = changeGiven,
                    seller = user,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerPhone2 = customerPhone2,
                    customerAddress = customerAddress,
                    creditDepositAmount = creditDepositAmount,
                    creditDepositMethod = creditDepositMethod,
                    creditRemainingBalance = creditRemainingBalance,
                    creditDueDate = creditDueDate
                )

                val saleItems = repository.getSaleItems(sale.id)
                _completedSale.value = Pair(sale, saleItems)
                _cart.value = emptyList()

                // Generate Receipt PDF automatically
                val receiptFile = pdfService.generateSaleReceipt(
                    sale = sale,
                    items = saleItems,
                    paperWidthMm = _paperWidthMm.value,
                    currentUser = user
                )

                showToast("Vente ${sale.reference} enregistrée avec succès !")
                onComplete(sale)
            } catch (e: Exception) {
                showToast("Erreur lors de la vente : ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun dismissCompletedSale() {
        _completedSale.value = null
    }

    // Product Management
    fun addProduct(product: Product) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.addProduct(product, user.fullName)
            showToast("Produit '${product.name}' ajouté avec succès")
        }
    }

    fun updateProduct(product: Product) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateProduct(product, user.fullName)
            showToast("Produit '${product.name}' mis à jour")
        }
    }

    fun softDeleteProduct(product: Product) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.softDeleteProduct(product, user.fullName)
            showToast("'${product.name}' déplacé dans la corbeille")
        }
    }

    fun restoreProduct(product: Product) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.restoreProduct(product, user.fullName)
            showToast("'${product.name}' restauré avec succès")
        }
    }

    fun permanentDeleteProduct(product: Product) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.permanentDeleteProduct(product, user.fullName)
            showToast("'${product.name}' définitivement supprimé")
        }
    }

    // Credit Payments
    fun payCredit(
        sale: SaleRecord,
        amount: Double,
        currency: String,
        method: String,
        note: String?
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.recordCreditPayment(
                sale = sale,
                amount = amount,
                currency = currency,
                method = method,
                recordedBy = user.fullName,
                note = note
            )
            showToast("Paiement de $amount $currency enregistré sur ${sale.reference}")
        }
    }

    // Seller Account Management (Admin only)
    fun createSeller(fullName: String, username: String, pass: String) {
        viewModelScope.launch {
            val success = repository.createSellerAccount(fullName, username, pass)
            if (success) {
                showToast("Compte vendeur '$username' créé avec succès")
            } else {
                showToast("Ce nom d'utilisateur existe déjà", isError = true)
            }
        }
    }

    fun updateSellerPermissions(seller: UserAccount) {
        viewModelScope.launch {
            repository.updateSellerUser(seller)
            showToast("Permissions du vendeur mises à jour")
        }
    }

    fun blockSeller(reason: String) {
        viewModelScope.launch {
            repository.blockSeller(reason)
            showToast("Vendeur suspendu")
        }
    }

    fun unblockSeller() {
        viewModelScope.launch {
            repository.unblockSeller()
            showToast("Vendeur réactivé")
        }
    }

    fun resetSellerPassword(pass: String) {
        viewModelScope.launch {
            repository.resetSellerPassword(pass)
            showToast("Mot de passe du vendeur réinitialisé")
        }
    }

    fun clearSellerDevice(userId: Long) {
        viewModelScope.launch {
            repository.clearDeviceLock(userId)
            showToast("Appareil dissocié avec succès")
        }
    }

    // PDF Actions
    fun exportInventoryPdf() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                val file = pdfService.generateInventoryPdf(
                    products = products.value,
                    exchangeRate = _exchangeRate.value,
                    currentUser = user
                )
                showToast("PDF Inventaire généré avec succès")
                pdfService.openOrSharePdf(file)
            } catch (e: Exception) {
                showToast("Erreur export PDF : ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun exportFinancialReportPdf(periodLabel: String, currencyFilter: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                val file = pdfService.generateFinancialReportPdf(
                    sales = sales.value,
                    periodLabel = periodLabel,
                    currencyFilter = currencyFilter,
                    exchangeRate = _exchangeRate.value,
                    currentUser = user
                )
                showToast("Rapport financier exporté en PDF")
                pdfService.openOrSharePdf(file)
            } catch (e: Exception) {
                showToast("Erreur export PDF : ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun reprintSaleReceipt(sale: SaleRecord) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                val items = repository.getSaleItems(sale.id)
                val file = pdfService.generateSaleReceipt(
                    sale = sale,
                    items = items,
                    paperWidthMm = _paperWidthMm.value,
                    currentUser = user
                )
                showToast("Reçu régénéré (${sale.reference})")
                pdfService.openOrSharePdf(file)
            } catch (e: Exception) {
                showToast("Erreur réimpression : ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun openDocument(doc: ExportedDocumentRecord) {
        val file = File(doc.filePath)
        if (file.exists()) {
            pdfService.openOrSharePdf(file)
        } else {
            showToast("Fichier introuvable sur l'appareil", isError = true)
        }
    }
}
