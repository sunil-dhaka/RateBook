package com.example.ratebook.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ratebook.data.database.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Query("SELECT * FROM products ORDER BY updated_at DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY updated_at DESC")
    suspend fun getAllProductsSync(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE sku = :sku")
    suspend fun getBySku(sku: String): Product?

    @Query("SELECT * FROM products WHERE category_id = :categoryId ORDER BY updated_at DESC")
    fun getByCategory(categoryId: Long): Flow<List<Product>>

    @Query("""
        SELECT * FROM products
        WHERE name LIKE '%' || :query || '%'
        OR name_normalized LIKE '%' || :query || '%'
        OR sku LIKE '%' || :query || '%'
        ORDER BY updated_at DESC
    """)
    fun search(query: String): Flow<List<Product>>

    @Query("""
        SELECT * FROM products
        WHERE (name LIKE '%' || :query || '%'
        OR name_normalized LIKE '%' || :query || '%'
        OR sku LIKE '%' || :query || '%')
        AND category_id = :categoryId
        ORDER BY updated_at DESC
    """)
    fun searchInCategory(query: String, categoryId: Long): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM products WHERE category_id = :categoryId")
    suspend fun getCountByCategory(categoryId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM products
        WHERE sku LIKE :skuPrefix || '%'
    """)
    suspend fun getCountBySkuPrefix(skuPrefix: String): Int

    @Query("""
        SELECT MAX(CAST(SUBSTR(sku, -4) AS INTEGER)) FROM products
        WHERE sku LIKE :skuPrefix || '%'
    """)
    suspend fun getMaxSequenceForPrefix(skuPrefix: String): Int?
}
