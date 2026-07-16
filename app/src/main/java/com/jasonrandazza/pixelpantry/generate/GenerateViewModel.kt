package com.jasonrandazza.pixelpantry.generate

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jasonrandazza.pixelpantry.data.RecipeRepository
import com.jasonrandazza.pixelpantry.data.db.AppDatabase
import com.jasonrandazza.pixelpantry.data.db.RecipeSource
import com.jasonrandazza.pixelpantry.domain.DetectedIngredient
import com.jasonrandazza.pixelpantry.ml.LocalRecipeGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Generates and persists a Leap-authored recipe from a fixed ingredient list. */
class GenerateViewModel(
    application: Application,
    private val ingredients: List<DetectedIngredient>,
    private val source: RecipeSource,
    private val recipeRepository: RecipeRepository,
) : AndroidViewModel(application) {
    private val generator = LocalRecipeGenerator(application)

    private val _ui = MutableStateFlow(GenerateUiState(ingredientNames = ingredients.map { it.name }))
    val ui: StateFlow<GenerateUiState> = _ui.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            _ui.update { it.copy(isLoadingModel = true, error = null) }
            try {
                generator.loadModel(onProgress = { p -> _ui.update { it.copy(downloadProgress = p) } })
                _ui.update { it.copy(isLoadingModel = false, isGenerating = true) }

                var finalText = ""
                generator.generateRecipe(ingredients.map { it.name }).collect { snapshot ->
                    finalText = snapshot
                    _ui.update { it.copy(recipeText = snapshot) }
                }
                _ui.update { it.copy(isGenerating = false) }
                persist(finalText)
            } catch (e: Exception) {
                Log.e(TAG, "recipe generation failed", e)
                _ui.update {
                    it.copy(
                        isLoadingModel = false,
                        isGenerating = false,
                        error = e.message ?: "Recipe generation failed.",
                    )
                }
            }
        }
    }

    private suspend fun persist(text: String) {
        val title = extractTitle(text) ?: "Generated recipe"
        val summary = ingredients.joinToString(", ") { it.name }
        val entity = recipeRepository.save(
            title = title,
            body = text,
            ingredientSummary = summary,
            source = source,
        )
        _ui.update { it.copy(savedRecipeId = entity.id) }
    }

    private fun extractTitle(text: String): String? =
        text.lineSequence()
            .firstOrNull { it.startsWith("Title:", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.ifBlank { null }

    override fun onCleared() {
        super.onCleared()
        // Unload off the (now-cancelled) viewModelScope so it isn't cut short.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                generator.unload()
            } catch (e: Exception) {
                Log.e(TAG, "unload failed", e)
            }
        }
    }

    class Factory(
        private val application: Application,
        private val ingredients: List<DetectedIngredient>,
        private val source: RecipeSource,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(GenerateViewModel::class.java))
            val repository = RecipeRepository(AppDatabase.get(application).recipeDao())
            return GenerateViewModel(application, ingredients, source, repository) as T
        }
    }

    private companion object {
        const val TAG = "GenerateViewModel"
    }
}
