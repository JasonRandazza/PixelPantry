package com.jasonrandazza.pixelpantry.inventory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jasonrandazza.pixelpantry.data.InventoryRepository
import com.jasonrandazza.pixelpantry.data.db.AppDatabase
import com.jasonrandazza.pixelpantry.domain.DetectedIngredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {
    val items: StateFlow<List<DetectedIngredient>> = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    fun toggleSelected(id: String) {
        _selectedIds.update { current -> if (id in current) current - id else current + id }
    }

    fun updateName(id: String, name: String) {
        val item = items.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { repository.upsert(item.copy(name = name)) }
    }

    fun updateQuantity(id: String, quantityLabel: String) {
        val item = items.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { repository.upsert(item.copy(quantityLabel = quantityLabel)) }
    }

    fun delete(id: String) {
        _selectedIds.update { it - id }
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun addItem(name: String, quantityLabel: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.upsert(DetectedIngredient(name = name, quantityLabel = quantityLabel.ifBlank { "unknown" }))
        }
    }

    /** Items selected for recipe generation, or all items when nothing is selected. */
    fun selectedOrAllItems(): List<DetectedIngredient> {
        val current = items.value
        val selected = _selectedIds.value
        return if (selected.isEmpty()) current else current.filter { it.id in selected }
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(InventoryViewModel::class.java))
            val repository = InventoryRepository(AppDatabase.get(appContext).inventoryDao())
            return InventoryViewModel(repository) as T
        }
    }
}
