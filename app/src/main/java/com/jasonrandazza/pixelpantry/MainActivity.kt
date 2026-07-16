package com.jasonrandazza.pixelpantry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jasonrandazza.pixelpantry.confirm.ConfirmScreen
import com.jasonrandazza.pixelpantry.confirm.ConfirmViewModel
import com.jasonrandazza.pixelpantry.inventory.InventoryScreen
import com.jasonrandazza.pixelpantry.inventory.InventoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var screen by remember { mutableStateOf(ProductScreen.Confirm) }
            MaterialTheme {
                when (screen) {
                    ProductScreen.Confirm -> {
                        val viewModel: ConfirmViewModel = viewModel(
                            factory = ConfirmViewModel.Factory(this),
                        )
                        ConfirmScreen(viewModel, onSaved = { screen = ProductScreen.Inventory })
                    }

                    ProductScreen.Inventory -> {
                        val viewModel: InventoryViewModel = viewModel(
                            factory = InventoryViewModel.Factory(this),
                        )
                        InventoryScreen(viewModel)
                    }
                }
            }
        }
    }
}

private enum class ProductScreen { Confirm, Inventory }
