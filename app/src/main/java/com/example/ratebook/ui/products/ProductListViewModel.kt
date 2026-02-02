package com.example.ratebook.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ratebook.data.database.entity.Category
import com.example.ratebook.data.database.entity.Product
import com.example.ratebook.data.repository.CategoryRepository
import com.example.ratebook.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    COST_PRICE_ASC,
    COST_PRICE_DESC,
    SELLING_PRICE_ASC,
    SELLING_PRICE_DESC,
    SKU_ASC,
    SKU_DESC,
    NEWEST,
    OLDEST
}

class ProductListViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _sortOption = MutableStateFlow(SortOption.NEWEST)

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId
    val sortOption: StateFlow<SortOption> = _sortOption

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>> = combine(
        _searchQuery,
        _selectedCategoryId,
        _sortOption
    ) { query, categoryId, sort ->
        Triple(query, categoryId, sort)
    }.flatMapLatest { (query, categoryId, sort) ->
        val productsFlow = when {
            query.isNotBlank() && categoryId != null ->
                productRepository.searchInCategory(query, categoryId)
            query.isNotBlank() ->
                productRepository.search(query)
            categoryId != null ->
                productRepository.getByCategory(categoryId)
            else ->
                productRepository.getAllProducts()
        }

        productsFlow.map { products ->
            sortProducts(products, sort)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun sortProducts(products: List<Product>, sort: SortOption): List<Product> {
        return when (sort) {
            SortOption.NAME_ASC -> products.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> products.sortedByDescending { it.name.lowercase() }
            SortOption.COST_PRICE_ASC -> products.sortedBy { it.costPrice }
            SortOption.COST_PRICE_DESC -> products.sortedByDescending { it.costPrice }
            SortOption.SELLING_PRICE_ASC -> products.sortedBy { it.sellingPrice }
            SortOption.SELLING_PRICE_DESC -> products.sortedByDescending { it.sellingPrice }
            SortOption.SKU_ASC -> products.sortedBy { it.sku }
            SortOption.SKU_DESC -> products.sortedByDescending { it.sku }
            SortOption.NEWEST -> products.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> products.sortedBy { it.createdAt }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    class Factory(
        private val productRepository: ProductRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductListViewModel::class.java)) {
                return ProductListViewModel(productRepository, categoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
