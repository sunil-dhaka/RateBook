package com.example.ratebook.ui.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ratebook.data.database.entity.MeasurementUnit
import com.example.ratebook.data.repository.MeasurementUnitRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UnitListViewModel(
    private val unitRepository: MeasurementUnitRepository
) : ViewModel() {

    val units: StateFlow<List<MeasurementUnit>> = unitRepository.getAllUnits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<UnitEvent>()
    val events: SharedFlow<UnitEvent> = _events

    fun addUnit(code: String, name: String, symbol: String) {
        viewModelScope.launch {
            try {
                unitRepository.insert(code, name, symbol)
                _events.emit(UnitEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(UnitEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun updateUnit(unit: MeasurementUnit, code: String, name: String, symbol: String) {
        viewModelScope.launch {
            try {
                unitRepository.update(unit.copy(code = code, name = name, symbol = symbol))
                _events.emit(UnitEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(UnitEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteUnit(unit: MeasurementUnit) {
        viewModelScope.launch {
            try {
                unitRepository.delete(unit)
                _events.emit(UnitEvent.DeleteSuccess)
            } catch (e: Exception) {
                _events.emit(UnitEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    sealed class UnitEvent {
        data object SaveSuccess : UnitEvent()
        data object DeleteSuccess : UnitEvent()
        data class Error(val message: String) : UnitEvent()
    }

    class Factory(private val unitRepository: MeasurementUnitRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UnitListViewModel::class.java)) {
                return UnitListViewModel(unitRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
