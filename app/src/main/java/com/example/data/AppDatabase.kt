package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserAccount::class,
        Product::class,
        SaleRecord::class,
        SaleItemRecord::class,
        CreditPaymentRecord::class,
        AuditLogRecord::class,
        SystemConfigRecord::class,
        ExportedDocumentRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun creditPaymentDao(): CreditPaymentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun systemConfigDao(): SystemConfigDao
    abstract fun exportedDocumentDao(): ExportedDocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "donsael_pos_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val userDao = database.userDao()
                val productDao = database.productDao()
                val configDao = database.systemConfigDao()
                val logDao = database.auditLogDao()

                // Default Admin
                userDao.insertUser(
                    UserAccount(
                        username = "admin",
                        password = "123",
                        fullName = "Arly Séverin (Admin)",
                        role = "ADMIN",
                        status = "ACTIVE",
                        canAccessPos = true,
                        canAccessInventory = true,
                        canAccessReports = true,
                        canModifyExchangeRate = true,
                        canAccessCredits = true,
                        canAccessHistory = true
                    )
                )

                // Single Seller Account
                userDao.insertUser(
                    UserAccount(
                        username = "vendeur",
                        password = "123",
                        fullName = "Jean Baptiste (Vendeur)",
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

                // Configs
                configDao.insertOrUpdateConfig(SystemConfigRecord("exchange_rate_usd_to_htg", "132.0"))
                configDao.insertOrUpdateConfig(SystemConfigRecord("receipt_paper_width_mm", "80"))
                configDao.insertOrUpdateConfig(SystemConfigRecord("message_of_the_day", "Bienvenue chez DONSAEL COMMUNICATION ET MULTI-SERVICES - Belle-Vue, L'Asile"))
                configDao.insertOrUpdateConfig(SystemConfigRecord("is_online", "true"))
                configDao.insertOrUpdateConfig(SystemConfigRecord("pin_code", "1234"))
                configDao.insertOrUpdateConfig(SystemConfigRecord("biometric_enabled", "false"))

                // Initial Products
                val initialProducts = listOf(
                    Product(
                        name = "Tablette Samsung Galaxy Tab A9",
                        category = "Appareil électronique",
                        quantity = 6,
                        currency = "HTG",
                        purchasePrice = 9500.0,
                        sellingPrice = 12500.0,
                        alertThreshold = 4,
                        location = "Vitrine Électronique A"
                    ),
                    Product(
                        name = "Radio Portable FM/AM Sony ICF-P27",
                        category = "Appareil électronique",
                        quantity = 12,
                        currency = "HTG",
                        purchasePrice = 1800.0,
                        sellingPrice = 2500.0,
                        alertThreshold = 4,
                        location = "Étagère 1"
                    ),
                    Product(
                        name = "Casque Audio JBL Tune 510BT",
                        category = "Appareil électronique",
                        quantity = 3, // Low stock <= 4
                        currency = "USD",
                        purchasePrice = 25.0,
                        sellingPrice = 40.0,
                        alertThreshold = 4,
                        location = "Vitrine Audio"
                    ),
                    Product(
                        name = "Téléviseur Smart LED 43\" TCL 4K",
                        category = "Appareil électronique",
                        quantity = 4, // Low stock <= 4
                        currency = "USD",
                        purchasePrice = 220.0,
                        sellingPrice = 295.0,
                        alertThreshold = 4,
                        location = "Rayon Télévision"
                    ),
                    Product(
                        name = "Ventilateur Rechargeable 16\" Reit",
                        category = "Appareil électroménager",
                        quantity = 8,
                        currency = "HTG",
                        purchasePrice = 4200.0,
                        sellingPrice = 6000.0,
                        alertThreshold = 4,
                        location = "Rayon Confort"
                    ),
                    Product(
                        name = "Réfrigérateur Double Porte Midea 240L",
                        category = "Appareil électroménager",
                        quantity = 2, // Low stock <= 4
                        currency = "USD",
                        purchasePrice = 380.0,
                        sellingPrice = 520.0,
                        alertThreshold = 4,
                        location = "Zone Gros Électro"
                    ),
                    Product(
                        name = "Fer à repasser vapeur Oster",
                        category = "Appareil électroménager",
                        quantity = 15,
                        currency = "HTG",
                        purchasePrice = 1500.0,
                        sellingPrice = 2250.0,
                        alertThreshold = 4,
                        location = "Étagère B"
                    ),
                    Product(
                        name = "Mixeur Blender Électrique Hamilton Beach",
                        category = "Appareil électroménager",
                        quantity = 7,
                        currency = "USD",
                        purchasePrice = 32.0,
                        sellingPrice = 48.0,
                        alertThreshold = 4,
                        location = "Étagère Cuisine"
                    )
                )

                for (p in initialProducts) {
                    productDao.insertProduct(p)
                }

                logDao.insertLog(
                    AuditLogRecord(
                        username = "Système",
                        actionType = "INITIALISATION",
                        details = "Initialisation du système DONSAEL POS par NexGen Spark"
                    )
                )
            }
        }
    }
}
