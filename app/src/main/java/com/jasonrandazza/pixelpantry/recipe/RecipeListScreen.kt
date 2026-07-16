package com.jasonrandazza.pixelpantry.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jasonrandazza.pixelpantry.data.db.RecipeEntity

@Composable
fun RecipeListScreen(
    viewModel: RecipeListViewModel,
    onOpenRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipes by viewModel.recipes.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Recipes", style = MaterialTheme.typography.headlineSmall)
        if (recipes.isEmpty()) {
            Text("No recipes generated yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recipes, key = { it.id }) { recipe -> RecipeRow(recipe, onOpenRecipe) }
            }
        }
    }
}

@Composable
private fun RecipeRow(recipe: RecipeEntity, onOpenRecipe: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenRecipe(recipe.id) }
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(recipe.title, style = MaterialTheme.typography.titleMedium)
        Text(recipe.ingredientSummary, style = MaterialTheme.typography.bodySmall)
    }
}
