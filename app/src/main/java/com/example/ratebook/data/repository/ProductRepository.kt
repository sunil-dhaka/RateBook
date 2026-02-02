package com.example.ratebook.data.repository

import com.example.ratebook.data.database.dao.ProductDao
import com.example.ratebook.data.database.entity.Category
import com.example.ratebook.data.database.entity.Product
import com.example.ratebook.domain.util.PhotoManager
import com.example.ratebook.domain.util.SkuGenerator
import com.example.ratebook.domain.util.TextNormalizer
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val skuGenerator: SkuGenerator,
    private val photoManager: PhotoManager
) {

    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getAllProductsSync(): List<Product> = productDao.getAllProductsSync()

    suspend fun getById(id: Long): Product? = productDao.getById(id)

    suspend fun getBySku(sku: String): Product? = productDao.getBySku(sku)

    fun getByCategory(categoryId: Long): Flow<List<Product>> =
        productDao.getByCategory(categoryId)

    fun search(query: String): Flow<List<Product>> {
        val normalizedQuery = TextNormalizer.normalizeForSearch(query)
        return productDao.search(normalizedQuery)
    }

    fun searchInCategory(query: String, categoryId: Long): Flow<List<Product>> {
        val normalizedQuery = TextNormalizer.normalizeForSearch(query)
        return productDao.searchInCategory(normalizedQuery, categoryId)
    }

    suspend fun insert(
        name: String,
        costPrice: Double,
        sellingPrice: Double,
        category: Category?,
        measurementUnitId: Long?,
        notes: String?,
        photoPath: String?
    ): Long {
        val sku = skuGenerator.generateSku(category)
        val normalizedName = TextNormalizer.normalize(name)
        val now = System.currentTimeMillis()

        val product = Product(
            sku = sku,
            name = name,
            nameNormalized = normalizedName,
            costPrice = costPrice,
            sellingPrice = sellingPrice,
            categoryId = category?.id,
            measurementUnitId = measurementUnitId,
            notes = notes,
            photoPath = photoPath,
            createdAt = now,
            updatedAt = now
        )
        return productDao.insert(product)
    }

    suspend fun update(product: Product) {
        val updatedProduct = product.copy(
            nameNormalized = TextNormalizer.normalize(product.name),
            updatedAt = System.currentTimeMillis()
        )
        productDao.update(updatedProduct)
    }

    suspend fun delete(product: Product) {
        product.photoPath?.let { path ->
            photoManager.deletePhoto(path)
        }
        productDao.delete(product)
    }

    suspend fun deleteAll() {
        photoManager.clearAllPhotos()
        productDao.deleteAll()
    }

    suspend fun insertAll(products: List<Product>) {
        productDao.insertAll(products)
    }

    suspend fun getCount(): Int = productDao.getCount()

    suspend fun getCountByCategory(categoryId: Long): Int =
        productDao.getCountByCategory(categoryId)
}
