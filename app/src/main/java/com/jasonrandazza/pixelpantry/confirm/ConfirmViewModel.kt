package com.jasonrandazza.pixelpantry.confirm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jasonrandazza.pixelpantry.data.InventoryRepository
import com.jasonrandazza.pixelpantry.data.db.AppDatabase
import com.jasonrandazza.pixelpantry.domain.DetectedIngredient
import com.jasonrandazza.pixelpantry.domain.IngredientLineParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConfirmViewModel(
    initialText: String,
    private val repository: InventoryRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(ConfirmUiState(items = IngredientLineParser.parse(initialText)))
    val ui = _ui.asStateFlow()

    fun updateName(id: String, name: String) {
        updateItem(id) { it.copy(name = name) }
    }

    fun updateQuantity(id: String, quantityLabel: String) {
        updateItem(id) { it.copy(quantityLabel = quantityLabel) }
    }

    fun remove(id: String) {
        _ui.update { state -> state.copy(items = state.items.filterNot { it.id == id }) }
    }

    fun addBlank() {
        _ui.update { state ->
            state.copy(items = state.items + DetectedIngredient(name = "", quantityLabel = ""))
        }
    }

    fun save() {
        val items = _ui.value.items
        _ui.update { it.copy(saveCompleted = false, error = null) }
        viewModelScope.launch {
            try {
                repository.upsertAll(items)
                _ui.update { it.copy(saveCompleted = true) }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Unable to save inventory.") }
            }
        }
    }

    private fun updateItem(id: String, transform: (DetectedIngredient) -> DetectedIngredient) {
        _ui.update { state ->
            state.copy(items = state.items.map { if (it.id == id) transform(it) else it })
        }
    }

    class Factory(
        context: Context,
        private val initialText: String = FixtureDetections.SAMPLE,
    ) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(ConfirmViewModel::class.java))
            val repository = InventoryRepository(AppDatabase.get(appContext).inventoryDao())
            return ConfirmViewModel(initialText, repository) as T
        }
    }
}
