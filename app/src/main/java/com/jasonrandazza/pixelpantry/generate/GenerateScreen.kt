package com.jasonrandazza.pixelpantry.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GenerateScreen(
    viewModel: GenerateViewModel,
    onViewRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsState()

    LaunchedEffect(Unit) { viewModel.start() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Generate recipe", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Using: ${state.ingredientNames.joinToString(", ").ifBlank { "no ingredients" }}",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (state.isLoadingModel) {
            Text("Loading recipe model…", style = MaterialTheme.typography.bodyMedium)
            val progress = state.downloadProgress
            if (progress != null) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (state.isGenerating) {
            Text("Writing your recipe…", style = MaterialTheme.typography.bodyMedium)
        }

        state.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        if (state.recipeText.isNotBlank()) {
            Text(state.recipeText, style = MaterialTheme.typography.bodyMedium)
        }

        state.savedRecipeId?.let { id ->
            Button(onClick = { onViewRecipe(id) }, modifier = Modifier.fillMaxWidth()) {
                Text("View saved recipe")
            }
        }
    }
}
