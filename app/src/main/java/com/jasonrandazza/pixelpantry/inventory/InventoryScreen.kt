package com.jasonrandazza.pixelpantry.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jasonrandazza.pixelpantry.domain.DetectedIngredient

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onGenerateRecipe: (List<DetectedIngredient>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items by viewModel.items.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Inventory", style = MaterialTheme.typography.headlineSmall)
        if (items.isEmpty()) {
            Text("No inventory items yet.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = item.id in selectedIds,
                            onCheckedChange = { viewModel.toggleSelected(item.id) },
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { viewModel.updateName(item.id, it) },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = item.quantityLabel,
                                onValueChange = { viewModel.updateQuantity(item.id, it) },
                                label = { Text("Quantity") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        TextButton(onClick = { viewModel.delete(item.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }

        Text(
            if (selectedIds.isEmpty()) {
                "No items selected — recipe uses everything."
            } else {
                "${selectedIds.size} item(s) selected for the recipe."
            },
            style = MaterialTheme.typography.bodySmall,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.weight(1f)) {
                Text("Add item")
            }
            Button(
                onClick = { onGenerateRecipe(viewModel.selectedOrAllItems()) },
                enabled = items.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Generate recipe")
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, quantity ->
                viewModel.addItem(name, quantity)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, quantity: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add inventory item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, quantity) }, enabled = name.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
