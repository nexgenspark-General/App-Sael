package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserAccount?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserAccount?

    @Query("SELECT * FROM users WHERE role = 'SELLER' AND status != 'DELETED' LIMIT 1")
    fun getSellerUser(): Flow<UserAccount?>

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND status != 'DELETED'")
    suspend fun getAdminCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Update
    suspend fun updateUser(user: UserAccount)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Long)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isTrash = 0 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isTrash = 1 ORDER BY name ASC")
    fun getTrashProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET isTrash = 1 WHERE id = :id")
    suspend fun softDeleteProduct(id: Long)

    @Query("UPDATE products SET isTrash = 0 WHERE id = :id")
    suspend fun restoreProduct(id: Long)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun permanentDeleteProduct(id: Long)

    @Query("UPDATE products SET quantity = :newQuantity WHERE id = :id")
    suspend fun updateStock(id: Long, newQuantity: Int)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleRecord>>

    @Query("SELECT * FROM sales WHERE paymentType = 'CREDIT' ORDER BY timestamp DESC")
    fun getCreditSales(): Flow<List<SaleRecord>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): SaleRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleRecord): Long

    @Update
    suspend fun updateSale(sale: SaleRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemRecord>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItems(saleId: Long): List<SaleItemRecord>
}

@Dao
interface CreditPaymentDao {
    @Query("SELECT * FROM credit_payments WHERE saleId = :saleId ORDER BY timestamp DESC")
    fun getPaymentsForSale(saleId: Long): Flow<List<CreditPaymentRecord>>

    @Query("SELECT * FROM credit_payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<CreditPaymentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CreditPaymentRecord): Long
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogRecord): Long
}

@Dao
interface SystemConfigDao {
    @Query("SELECT * FROM system_configs")
    fun getAllConfigs(): Flow<List<SystemConfigRecord>>

    @Query("SELECT * FROM system_configs WHERE key = :key LIMIT 1")
    suspend fun getConfig(key: String): SystemConfigRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: SystemConfigRecord)
}

@Dao
interface ExportedDocumentDao {
    @Query("SELECT * FROM exported_documents ORDER BY timestamp DESC")
    fun getAllDocuments(): Flow<List<ExportedDocumentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: ExportedDocumentRecord): Long

    @Query("DELETE FROM exported_documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)
}
