package com.example.ratebook.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ratebook.data.database.entity.Category
import com.example.ratebook.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryListViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<CategoryEvent>()
    val events: SharedFlow<CategoryEvent> = _events

    fun addCategory(name: String) {
        viewModelScope.launch {
            try {
                categoryRepository.insert(name)
                _events.emit(CategoryEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(CategoryEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun updateCategory(category: Category, newName: String) {
        viewModelScope.launch {
            try {
                categoryRepository.update(category.copy(name = newName))
                _events.emit(CategoryEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(CategoryEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.delete(category)
                _events.emit(CategoryEvent.DeleteSuccess)
            } catch (e: Exception) {
                _events.emit(CategoryEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    sealed class CategoryEvent {
        data object SaveSuccess : CategoryEvent()
        data object DeleteSuccess : CategoryEvent()
        data class Error(val message: String) : CategoryEvent()
    }

    class Factory(private val categoryRepository: CategoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CategoryListViewModel::class.java)) {
                return CategoryListViewModel(categoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
