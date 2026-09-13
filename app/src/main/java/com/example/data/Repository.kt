package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(private val database: AppDatabase) {
    val activeProducts: Flow<List<Product>> = database.productDao().getActiveProducts()
    val trashProducts: Flow<List<Product>> = database.productDao().getTrashProducts()
    val allSales: Flow<List<SaleRecord>> = database.saleDao().getAllSales()
    val creditSales: Flow<List<SaleRecord>> = database.saleDao().getCreditSales()
    val allLogs: Flow<List<AuditLogRecord>> = database.auditLogDao().getAllLogs()
    val allConfigs: Flow<List<SystemConfigRecord>> = database.systemConfigDao().getAllConfigs()
    val allDocuments: Flow<List<ExportedDocumentRecord>> = database.exportedDocumentDao().getAllDocuments()
    val sellerUser: Flow<UserAccount?> = database.userDao().getSellerUser()
    val allUsers: Flow<List<UserAccount>> = database.userDao().getAllUsers()

    suspend fun getUserByUsername(username: String): UserAccount? = withContext(Dispatchers.IO) {
        database.userDao().getUserByUsername(username)
    }

    suspend fun login(username: String, pass: String, currentDeviceId: String): LoginResult = withContext(Dispatchers.IO) {
        val user = database.userDao().getUserByUsername(username.trim())
            ?: return@withContext LoginResult.Error("Identifiant ou mot de passe incorrect.")

        if (user.password != pass.trim()) {
            return@withContext LoginResult.Error("Identifiant ou mot de passe incorrect.")
        }

        if (user.status == "BLOCKED") {
            val reason = user.blockReason ?: "Votre compte a été suspendu par l'administration."
            return@withContext LoginResult.Blocked(reason)
        }

        if (user.status == "DELETED") {
            return@withContext LoginResult.Error("Ce compte n'existe plus.")
        }

        // Single device check:
        if (user.activeDeviceId != null && user.activeDeviceId.isNotEmpty() && user.activeDeviceId != currentDeviceId) {
            return@withContext LoginResult.DeviceConflict("Vous êtes déjà connecté à ce compte depuis un autre appareil.")
        }

        // Update active device & last login
        val updated = user.copy(
            activeDeviceId = currentDeviceId,
            lastLoginTimestamp = System.currentTimeMillis()
        )
        database.userDao().updateUser(updated)

        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = user.username,
                actionType = "CONNEXION",
                details = "Connexion réussie de ${user.fullName} (${user.role})"
            )
        )

        LoginResult.Success(updated)
    }

    suspend fun logout(userId: Long) = withContext(Dispatchers.IO) {
        val user = database.userDao().getUserById(userId)
        if (user != null) {
            database.userDao().updateUser(user.copy(activeDeviceId = null))
            database.auditLogDao().insertLog(
                AuditLogRecord(
                    username = user.username,
                    actionType = "DECONNEXION",
                    details = "Déconnexion de ${user.fullName}"
                )
            )
        }
    }

    suspend fun clearDeviceLock(userId: Long) = withContext(Dispatchers.IO) {
        val user = database.userDao().getUserById(userId)
        if (user != null) {
            database.userDao().updateUser(user.copy(activeDeviceId = null))
            database.auditLogDao().insertLog(
                AuditLogRecord(
                    username = "Admin",
                    actionType = "MODERATION",
                    details = "Appareil dissocié pour l'utilisateur ${user.username}"
                )
            )
        }
    }

    suspend fun updateSellerUser(user: UserAccount) = withContext(Dispatchers.IO) {
        database.userDao().updateUser(user)
        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = "Admin",
                actionType = "PARAMETRES",
                details = "Mise à jour des permissions du vendeur ${user.username}"
            )
        )
    }

    suspend fun createSellerAccount(fullName: String, username: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        val existing = database.userDao().getUserByUsername(username)
        if (existing != null) return@withContext false

        database.userDao().insertUser(
            UserAccount(
                username = username,
                password = pass,
                fullName = fullName,
                role = "SELLER",
                status = "ACTIVE",
                canAccessPos = true,
                canAccessInventory = true,
                canAccessReports = false,
                canModifyExchangeRate = false,
                canAccessCredits = true,
                canAccessHistory = true
            )
        )
        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = "Admin",
                actionType = "UTILISATEUR",
                details = "Création du compte vendeur: $username ($fullName)"
            )
        )
        true
    }

    suspend fun blockSeller(reason: String) = withContext(Dispatchers.IO) {
        val seller = database.userDao().getUserByUsername("vendeur")
        if (seller != null) {
            database.userDao().updateUser(
                seller.copy(
                    status = "BLOCKED",
                    blockReason = reason,
                    activeDeviceId = null
                )
            )
            database.auditLogDao().insertLog(
                AuditLogRecord(
                    username = "Admin",
                    actionType = "MODERATION",
                    details = "Blocage du vendeur: $reason"
                )
            )
        }
    }

    suspend fun unblockSeller() = withContext(Dispatchers.IO) {
        val seller = database.userDao().getUserByUsername("vendeur")
        if (seller != null) {
            database.userDao().updateUser(
                seller.copy(
                    status = "ACTIVE",
                    blockReason = null
                )
            )
            database.auditLogDao().insertLog(
                AuditLogRecord(
                    username = "Admin",
                    actionType = "MODERATION",
                    details = "Déblocage du vendeur"
                )
            )
        }
    }

    suspend fun resetSellerPassword(newPass: String) = withContext(Dispatchers.IO) {
        val seller = database.userDao().getUserByUsername("vendeur")
        if (seller != null) {
            database.userDao().updateUser(seller.copy(password = newPass, activeDeviceId = null))
            database.auditLogDao().insertLog(
                AuditLogRecord(
                    username = "Admin",
                    actionType = "MODERATION",
                    details = "Réinitialisation du mot de passe du vendeur"
                )
            )
        }
    }

    // Products
    suspend fun addProduct(product: Product, actor: String): Long = withContext(Dispatchers.IO) {
        val id = database.productDao().insertProduct(product)
        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = actor,
                actionType = "CREATION_PRODUIT",
                details = "Ajout du produit: ${product.name} (Qté: ${product.quantity}, ${product.sellingPrice} ${product.currency})"
            )
        )
        id
    }

    suspend fun updateProduct(product: Product, actor: String) = withContext(Dispatchers.IO) {
        database.productDao().updateProduct(product)
        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = actor,
                actionType = "MODIFICATION_PRODUIT",
                details = "Modification du produit: ${product.name} (Qté: ${product.quantity})"
            )
        )
    }

    suspend fun softDeleteProduct(product: Product, actor: String) = withContext(Dispatchers.IO) {
        database.productDao().softDeleteProduct(product.id)
        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = actor,
                actionType = "SUPPRESSION_PRODUIT",
                details = "Déplacement vers la corbeille: ${product.name}"
            )
        )
    }

    suspend fun restoreProduct(product: Product, actor: String) = withContext(Dispatchers.IO) {
        database.productDao().restoreProduct(product.id)
        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = actor,
                actionType = "RESTAURATION_PRODUIT",
                details = "Restauration depuis la corbeille: ${product.name}"
            )
        )
    }

    suspend fun permanentDeleteProduct(product: Product, actor: String) = withContext(Dispatchers.IO) {
        database.productDao().permanentDeleteProduct(product.id)
        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = actor,
                actionType = "SUPPRESSION_DEFINITIVE",
                details = "Suppression définitive du produit: ${product.name}"
            )
        )
    }

    // Sales and POS
    suspend fun processSale(
        items: List<CartItem>,
        paymentType: String,
        paymentMethod: String,
        paymentCurrency: String,
        exchangeRate: Double,
        totalAmount: Double,
        amountTendered: Double,
        changeGiven: Double,
        seller: UserAccount,
        customerName: String?,
        customerPhone: String?,
        customerPhone2: String?,
        customerAddress: String?,
        creditDepositAmount: Double,
        creditDepositMethod: String?,
        creditRemainingBalance: Double,
        creditDueDate: Long?
    ): SaleRecord = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault())
        val ref = "DON-" + dateFormat.format(Date())

        var totalPurchaseCost = 0.0

        // Live check & deduct stock
        for (item in items) {
            val prod = database.productDao().getProductById(item.product.id)
            if (prod != null) {
                val newStock = (prod.quantity - item.quantity).coerceAtLeast(0)
                database.productDao().updateStock(prod.id, newStock)

                // Cost in sale payment currency
                val unitCostInSaleCurr = if (prod.currency == paymentCurrency) {
                    prod.purchasePrice
                } else if (prod.currency == "USD" && paymentCurrency == "HTG") {
                    prod.purchasePrice * exchangeRate
                } else {
                    prod.purchasePrice / exchangeRate
                }
                totalPurchaseCost += (unitCostInSaleCurr * item.quantity)
            }
        }

        val creditStatus = if (paymentType == "CREDIT") {
            if (creditRemainingBalance <= 0.001) "PAID" else if (creditDepositAmount > 0) "PARTIAL" else "UNPAID"
        } else {
            "PAID"
        }

        val sale = SaleRecord(
            reference = ref,
            timestamp = System.currentTimeMillis(),
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            paymentCurrency = paymentCurrency,
            exchangeRateApplied = exchangeRate,
            totalAmount = totalAmount,
            amountTendered = amountTendered,
            changeGiven = changeGiven,
            purchaseCostTotal = totalPurchaseCost,
            sellerUsername = seller.username,
            sellerFullName = seller.fullName,
            customerName = customerName?.ifBlank { null },
            customerPhone = customerPhone?.ifBlank { null },
            customerPhone2 = customerPhone2?.ifBlank { null },
            customerAddress = customerAddress?.ifBlank { null },
            creditDepositAmount = creditDepositAmount,
            creditDepositMethod = creditDepositMethod,
            creditRemainingBalance = creditRemainingBalance,
            creditDueDate = creditDueDate,
            creditStatus = creditStatus
        )

        val saleId = database.saleDao().insertSale(sale)

        val saleItems = items.map { cartItem ->
            SaleItemRecord(
                saleId = saleId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                category = cartItem.product.category,
                quantity = cartItem.quantity,
                unitPriceOriginal = cartItem.product.sellingPrice,
                currencyOriginal = cartItem.product.currency,
                unitPriceConverted = cartItem.unitPriceInSaleCurrency,
                purchaseCostUnit = cartItem.product.purchasePrice
            )
        }
        database.saleDao().insertSaleItems(saleItems)

        // If credit had deposit, register deposit payment
        if (paymentType == "CREDIT" && creditDepositAmount > 0) {
            database.creditPaymentDao().insertPayment(
                CreditPaymentRecord(
                    saleId = saleId,
                    amountPaid = creditDepositAmount,
                    currency = paymentCurrency,
                    paymentMethod = creditDepositMethod ?: paymentMethod,
                    recordedBy = seller.fullName,
                    note = "Acompte initial à la commande"
                )
            )
        }

        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = seller.username,
                actionType = "VENTE",
                details = "Vente $ref ($paymentType) pour ${totalAmount} $paymentCurrency effectuée par ${seller.fullName}"
            )
        )

        sale.copy(id = saleId)
    }

    suspend fun recordCreditPayment(
        sale: SaleRecord,
        amount: Double,
        currency: String,
        method: String,
        recordedBy: String,
        note: String?
    ) = withContext(Dispatchers.IO) {
        database.creditPaymentDao().insertPayment(
            CreditPaymentRecord(
                saleId = sale.id,
                amountPaid = amount,
                currency = currency,
                paymentMethod = method,
                recordedBy = recordedBy,
                note = note
            )
        )

        val newRemaining = (sale.creditRemainingBalance - amount).coerceAtLeast(0.0)
        val newStatus = if (newRemaining <= 0.001) "PAID" else "PARTIAL"

        database.saleDao().updateSale(
            sale.copy(
                creditRemainingBalance = newRemaining,
                creditStatus = newStatus
            )
        )

        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = recordedBy,
                actionType = "PAIEMENT_CREDIT",
                details = "Paiement de $amount $currency sur le crédit ${sale.reference} par $recordedBy. Nouveau solde: $newRemaining $currency"
            )
        )
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItemRecord> = withContext(Dispatchers.IO) {
        database.saleDao().getSaleItems(saleId)
    }

    suspend fun getSaleById(saleId: Long): SaleRecord? = withContext(Dispatchers.IO) {
        database.saleDao().getSaleById(saleId)
    }

    // Configs
    suspend fun updateConfig(key: String, value: String, actor: String) = withContext(Dispatchers.IO) {
        database.systemConfigDao().insertOrUpdateConfig(SystemConfigRecord(key, value))
        database.auditLogDao().insertLog(
            AuditLogRecord(
                username = actor,
                actionType = "PARAMETRES",
                details = "Configuration modifiée: $key = $value"
            )
        )
    }

    suspend fun insertDocument(doc: ExportedDocumentRecord): Long = withContext(Dispatchers.IO) {
        database.exportedDocumentDao().insertDocument(doc)
    }

    suspend fun deleteDocument(doc: ExportedDocumentRecord) = withContext(Dispatchers.IO) {
        database.exportedDocumentDao().deleteDocument(doc.id)
    }
}

sealed class LoginResult {
    data class Success(val user: UserAccount) : LoginResult()
    data class Error(val message: String) : LoginResult()
    data class Blocked(val reason: String) : LoginResult()
    data class DeviceConflict(val message: String) : LoginResult()
}

data class CartItem(
    val product: Product,
    val quantity: Int,
    val unitPriceInSaleCurrency: Double
)
