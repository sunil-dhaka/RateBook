package com.example.ratebook.ui.products

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ratebook.data.database.entity.Category
import com.example.ratebook.data.database.entity.MeasurementUnit
import com.example.ratebook.data.database.entity.Product
import com.example.ratebook.data.repository.CategoryRepository
import com.example.ratebook.data.repository.MeasurementUnitRepository
import com.example.ratebook.data.repository.ProductRepository
import com.example.ratebook.domain.util.PhotoManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductEditViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val measurementUnitRepository: MeasurementUnitRepository,
    private val photoManager: PhotoManager
) : ViewModel() {

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val units: StateFlow<List<MeasurementUnit>> = measurementUnitRepository.getAllUnits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<ProductEditEvent>()
    val events: SharedFlow<ProductEditEvent> = _events

    private var photoPath: String? = null
    private var pendingPhotoUri: Uri? = null
    private var pendingPhotoBitmap: Bitmap? = null

    fun loadProduct(productId: Long) {
        if (productId <= 0) return

        viewModelScope.launch {
            val product = productRepository.getById(productId)
            _product.value = product
            photoPath = product?.photoPath
        }
    }

    fun setPhotoFromUri(uri: Uri) {
        pendingPhotoUri = uri
        pendingPhotoBitmap = null
    }

    fun setPhotoFromBitmap(bitmap: Bitmap) {
        pendingPhotoBitmap = bitmap
        pendingPhotoUri = null
    }

    fun saveProduct(
        name: String,
        costPrice: Double,
        sellingPrice: Double,
        categoryId: Long?,
        unitId: Long?,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                val existingProduct = _product.value
                val category = categoryId?.let { categoryRepository.getById(it) }

                if (existingProduct != null) {
                    pendingPhotoUri?.let { uri ->
                        photoPath = photoManager.savePhoto(uri, existingProduct.id)
                    }
                    pendingPhotoBitmap?.let { bitmap ->
                        photoPath = photoManager.savePhoto(bitmap, existingProduct.id)
                    }

                    val updated = existingProduct.copy(
                        name = name,
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        categoryId = categoryId,
                        measurementUnitId = unitId,
                        notes = notes?.takeIf { it.isNotBlank() },
                        photoPath = photoPath
                    )
                    productRepository.update(updated)
                } else {
                    val productId = productRepository.insert(
                        name = name,
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        category = category,
                        measurementUnitId = unitId,
                        notes = notes?.takeIf { it.isNotBlank() },
                        photoPath = null
                    )

                    pendingPhotoUri?.let { uri ->
                        val path = photoManager.savePhoto(uri, productId)
                        if (path != null) {
                            val product = productRepository.getById(productId)
                            product?.let {
                                productRepository.update(it.copy(photoPath = path))
                            }
                        }
                    }
                    pendingPhotoBitmap?.let { bitmap ->
                        val path = photoManager.savePhoto(bitmap, productId)
                        if (path != null) {
                            val product = productRepository.getById(productId)
                            product?.let {
                                productRepository.update(it.copy(photoPath = path))
                            }
                        }
                    }
                }

                _events.emit(ProductEditEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(ProductEditEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteProduct() {
        viewModelScope.launch {
            try {
                _product.value?.let { product ->
                    productRepository.delete(product)
                    _events.emit(ProductEditEvent.DeleteSuccess)
                }
            } catch (e: Exception) {
                _events.emit(ProductEditEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun getPhotoFile(path: String) = photoManager.getPhotoFile(path)

    sealed class ProductEditEvent {
        data object SaveSuccess : ProductEditEvent()
        data object DeleteSuccess : ProductEditEvent()
        data class Error(val message: String) : ProductEditEvent()
    }

    class Factory(
        private val productRepository: ProductRepository,
        private val categoryRepository: CategoryRepository,
        private val measurementUnitRepository: MeasurementUnitRepository,
        private val photoManager: PhotoManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductEditViewModel::class.java)) {
                return ProductEditViewModel(
                    productRepository,
                    categoryRepository,
                    measurementUnitRepository,
                    photoManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
