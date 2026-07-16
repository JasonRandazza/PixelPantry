package com.jasonrandazza.pixelpantry.recipe

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jasonrandazza.pixelpantry.data.RecipeRepository
import com.jasonrandazza.pixelpantry.data.db.AppDatabase
import com.jasonrandazza.pixelpantry.data.db.RecipeEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RecipeListViewModel(repository: RecipeRepository) : ViewModel() {
    val recipes: StateFlow<List<RecipeEntity>> = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(RecipeListViewModel::class.java))
            val repository = RecipeRepository(AppDatabase.get(appContext).recipeDao())
            return RecipeListViewModel(repository) as T
        }
    }
}
