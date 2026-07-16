package com.jasonrandazza.pixelpantry.recipe

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jasonrandazza.pixelpantry.data.RecipeRepository
import com.jasonrandazza.pixelpantry.data.db.AppDatabase
import com.jasonrandazza.pixelpantry.data.db.RecipeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    recipeId: String,
    repository: RecipeRepository,
) : ViewModel() {
    private val _recipe = MutableStateFlow<RecipeEntity?>(null)
    val recipe: StateFlow<RecipeEntity?> = _recipe.asStateFlow()

    init {
        viewModelScope.launch { _recipe.value = repository.getById(recipeId) }
    }

    class Factory(context: Context, private val recipeId: String) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(RecipeDetailViewModel::class.java))
            val repository = RecipeRepository(AppDatabase.get(appContext).recipeDao())
            return RecipeDetailViewModel(recipeId, repository) as T
        }
    }
}
