package com.example.ratebook.data.repository

import com.example.ratebook.data.database.dao.CategoryDao
import com.example.ratebook.data.database.entity.Category
import com.example.ratebook.domain.util.TextNormalizer
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getAllCategoriesSync(): List<Category> = categoryDao.getAllCategoriesSync()

    suspend fun getById(id: Long): Category? = categoryDao.getById(id)

    suspend fun getByName(name: String): Category? = categoryDao.getByName(name)

    fun search(query: String): Flow<List<Category>> {
        val normalizedQuery = TextNormalizer.normalizeForSearch(query)
        return categoryDao.search(normalizedQuery)
    }

    suspend fun insert(name: String): Long {
        val normalizedName = TextNormalizer.normalize(name)
        val maxOrder = categoryDao.getMaxDisplayOrder() ?: 0
        val category = Category(
            name = name,
            nameNormalized = normalizedName,
            displayOrder = maxOrder + 1
        )
        return categoryDao.insert(category)
    }

    suspend fun update(category: Category) {
        val updatedCategory = category.copy(
            nameNormalized = TextNormalizer.normalize(category.name)
        )
        categoryDao.update(updatedCategory)
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }

    suspend fun deleteAll() {
        categoryDao.deleteAll()
    }

    suspend fun insertAll(categories: List<Category>) {
        categoryDao.insertAll(categories)
    }

    suspend fun getCount(): Int = categoryDao.getCount()
}
