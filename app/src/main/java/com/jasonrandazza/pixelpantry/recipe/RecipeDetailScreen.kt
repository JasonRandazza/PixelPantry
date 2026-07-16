package com.jasonrandazza.pixelpantry.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    modifier: Modifier = Modifier,
) {
    val recipe by viewModel.recipe.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (recipe == null) {
            Text("Loading recipe…")
        } else {
            val r = recipe!!
            Text(r.title, style = MaterialTheme.typography.headlineSmall)
            Text("Made from: ${r.ingredientSummary}", style = MaterialTheme.typography.bodySmall)
            Text(r.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
