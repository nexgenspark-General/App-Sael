package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val password: String,
    val fullName: String,
    val role: String, // "ADMIN", "SELLER"
    val status: String = "ACTIVE", // "ACTIVE", "BLOCKED", "DELETED"
    val blockReason: String? = null,
    val activeDeviceId: String? = null,
    val lastLoginTimestamp: Long = 0L,
    val canAccessPos: Boolean = true,
    val canAccessInventory: Boolean = true,
    val canAccessReports: Boolean = false,
    val canModifyExchangeRate: Boolean = false,
    val canAccessCredits: Boolean = true,
    val canAccessHistory: Boolean = true
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // "Appareil électroménager", "Appareil électronique", "Accessoires"
    val quantity: Int,
    val currency: String, // "HTG", "USD"
    val purchasePrice: Double,
    val sellingPrice: Double,
    val alertThreshold: Int = 4,
    val dateAdded: Long = System.currentTimeMillis(),
    val location: String = "Magasin Principal",
    val isTrash: Boolean = false
)

@Entity(tableName = "sales")
data class SaleRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val timestamp: Long = System.currentTimeMillis(),
    val paymentType: String, // "COMPTANT", "CREDIT"
    val paymentMethod: String, // "ESPECES", "MONCASH", "NATCASH", "KPLAN", "BNC", "UNIBANK", "SOGEBANK", "CAPITAL_BANK", "ZELLE", "CASH_APP", "WESTERN_UNION", "MONEYGRAM"
    val paymentCurrency: String, // "HTG", "USD"
    val exchangeRateApplied: Double, // 1 USD = X HTG
    val totalAmount: Double,
    val amountTendered: Double,
    val changeGiven: Double,
    val purchaseCostTotal: Double,
    val sellerUsername: String,
    val sellerFullName: String,
    val customerName: String? = null,
    val customerPhone: String? = null,
    // Specific to Credit sales
    val customerPhone2: String? = null,
    val customerAddress: String? = null,
    val creditDepositAmount: Double = 0.0,
    val creditDepositMethod: String? = null,
    val creditRemainingBalance: Double = 0.0,
    val creditDueDate: Long? = null,
    val creditStatus: String = "PAID" // "PAID", "UNPAID", "PARTIAL"
)

@Entity(tableName = "sale_items")
data class SaleItemRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val category: String,
    val quantity: Int,
    val unitPriceOriginal: Double,
    val currencyOriginal: String,
    val unitPriceConverted: Double,
    val purchaseCostUnit: Double
)

@Entity(tableName = "credit_payments")
data class CreditPaymentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val amountPaid: Double,
    val currency: String,
    val paymentMethod: String,
    val recordedBy: String,
    val note: String? = null
)

@Entity(tableName = "audit_logs")
data class AuditLogRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val username: String,
    val actionType: String,
    val details: String
)

@Entity(tableName = "system_configs")
data class SystemConfigRecord(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "exported_documents")
data class ExportedDocumentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val docType: String, // "TICKET_VENTE", "INVENTAIRE_PDF", "RAPPORT_FINANCIER_PDF", "CREDITS_PDF"
    val timestamp: Long = System.currentTimeMillis(),
    val userName: String,
    val filePath: String,
    val summary: String
)
