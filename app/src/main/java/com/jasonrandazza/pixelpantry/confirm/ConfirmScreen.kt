package com.jasonrandazza.pixelpantry.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jasonrandazza.pixelpantry.domain.DetectedIngredient

@Composable
fun ConfirmScreen(
    viewModel: ConfirmViewModel,
    onSaved: () -> Unit,
    onContinue: (List<DetectedIngredient>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsState()

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onSaved()
    }
    LaunchedEffect(state.confirmedItems) {
        state.confirmedItems?.let(onContinue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Confirm detected items", style = MaterialTheme.typography.headlineSmall)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.items, key = { it.id }) { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { viewModel.updateName(item.id, it) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = item.quantityLabel,
                            onValueChange = { viewModel.updateQuantity(item.id, it) },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.remove(item.id) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }

        state.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        TextButton(onClick = viewModel::addBlank, modifier = Modifier.fillMaxWidth()) {
            Text("Add item")
        }
        Button(onClick = viewModel::confirm, modifier = Modifier.fillMaxWidth()) {
            Text(
                when (state.mode) {
                    ConfirmMode.HomeSave -> "Save to inventory"
                    ConfirmMode.OneOffSession -> "Continue to recipe"
                },
            )
        }
    }
}
