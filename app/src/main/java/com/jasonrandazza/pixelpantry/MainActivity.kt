package com.jasonrandazza.pixelpantry

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jasonrandazza.pixelpantry.confirm.ConfirmMode
import com.jasonrandazza.pixelpantry.confirm.ConfirmScreen
import com.jasonrandazza.pixelpantry.confirm.ConfirmViewModel
import com.jasonrandazza.pixelpantry.data.db.RecipeSource
import com.jasonrandazza.pixelpantry.generate.GenerateScreen
import com.jasonrandazza.pixelpantry.generate.GenerateViewModel
import com.jasonrandazza.pixelpantry.inventory.InventoryScreen
import com.jasonrandazza.pixelpantry.inventory.InventoryViewModel
import com.jasonrandazza.pixelpantry.nav.Screen
import com.jasonrandazza.pixelpantry.recipe.RecipeDetailScreen
import com.jasonrandazza.pixelpantry.recipe.RecipeDetailViewModel
import com.jasonrandazza.pixelpantry.recipe.RecipeListScreen
import com.jasonrandazza.pixelpantry.recipe.RecipeListViewModel
import com.jasonrandazza.pixelpantry.scan.ScanScreen
import com.jasonrandazza.pixelpantry.scan.ScanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                PixelPantryApp()
            }
        }
    }
}

@Composable
private fun PixelPantryApp() {
    val backStack = remember { mutableStateListOf<Screen>(Screen.ModeChooser) }
    fun push(screen: Screen) = backStack.add(screen)
    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
    fun replaceTop(screen: Screen) {
        pop()
        push(screen)
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    val current = backStack.last()

    Column(modifier = Modifier.fillMaxSize()) {
        if (backStack.size > 1) {
            TextButton(onClick = { pop() }) { Text("< Back") }
        }

        when (val screen = current) {
            Screen.ModeChooser -> ModeChooserScreen(
                onHome = { push(Screen.HomeHub) },
                onOneOff = { push(Screen.OneOffHub) },
            )

            Screen.HomeHub -> HomeHubScreen(
                onInventory = { push(Screen.Inventory) },
                onScan = { push(Screen.Scan(ConfirmMode.HomeSave)) },
                onRecipes = { push(Screen.RecipeList) },
            )

            Screen.OneOffHub -> OneOffHubScreen(
                onManual = { push(Screen.Confirm(ConfirmMode.OneOffSession, "")) },
                onScan = { push(Screen.Scan(ConfirmMode.OneOffSession)) },
                onRecipes = { push(Screen.RecipeList) },
            )

            is Screen.Scan -> {
                val viewModel: ScanViewModel = viewModel(key = screen.toString())
                ScanScreen(
                    viewModel = viewModel,
                    onUseText = { text -> push(Screen.Confirm(screen.mode, text)) },
                )
            }

            is Screen.Confirm -> {
                val viewModel: ConfirmViewModel = viewModel(
                    key = screen.toString(),
                    factory = ConfirmViewModel.Factory(
                        context = LocalContext.current,
                        initialText = screen.initialText,
                        mode = screen.mode,
                    ),
                )
                ConfirmScreen(
                    viewModel = viewModel,
                    onSaved = { replaceTop(Screen.Inventory) },
                    onContinue = { items -> push(Screen.Generate(items, fromOneOff = true)) },
                )
            }

            Screen.Inventory -> {
                val viewModel: InventoryViewModel = viewModel(
                    factory = InventoryViewModel.Factory(LocalContext.current),
                )
                InventoryScreen(
                    viewModel = viewModel,
                    onGenerateRecipe = { items -> push(Screen.Generate(items, fromOneOff = false)) },
                )
            }

            is Screen.Generate -> {
                val application = LocalContext.current.applicationContext as Application
                val source = if (screen.fromOneOff) RecipeSource.ONE_OFF else RecipeSource.HOME
                val viewModel: GenerateViewModel = viewModel(
                    key = screen.toString(),
                    factory = GenerateViewModel.Factory(application, screen.ingredients, source),
                )
                GenerateScreen(
                    viewModel = viewModel,
                    onViewRecipe = { id -> replaceTop(Screen.RecipeDetail(id)) },
                )
            }

            Screen.RecipeList -> {
                val viewModel: RecipeListViewModel = viewModel(
                    factory = RecipeListViewModel.Factory(LocalContext.current),
                )
                RecipeListScreen(
                    viewModel = viewModel,
                    onOpenRecipe = { id -> push(Screen.RecipeDetail(id)) },
                )
            }

            is Screen.RecipeDetail -> {
                val viewModel: RecipeDetailViewModel = viewModel(
                    key = screen.toString(),
                    factory = RecipeDetailViewModel.Factory(
                        LocalContext.current,
                        screen.id,
                    ),
                )
                RecipeDetailScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ModeChooserScreen(onHome: () -> Unit, onOneOff: () -> Unit) {
    HubScreen(title = "PixelPantry") {
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Home") }
        Button(onClick = onOneOff, modifier = Modifier.fillMaxWidth()) { Text("One-off session") }
    }
}

@Composable
private fun HomeHubScreen(onInventory: () -> Unit, onScan: () -> Unit, onRecipes: () -> Unit) {
    HubScreen(title = "Home") {
        Button(onClick = onInventory, modifier = Modifier.fillMaxWidth()) { Text("Inventory") }
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("Scan / Confirm") }
        Button(onClick = onRecipes, modifier = Modifier.fillMaxWidth()) { Text("Recipes") }
    }
}

@Composable
private fun OneOffHubScreen(onManual: () -> Unit, onScan: () -> Unit, onRecipes: () -> Unit) {
    HubScreen(title = "One-off session") {
        Button(onClick = onManual, modifier = Modifier.fillMaxWidth()) { Text("Manual ingredients") }
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("Scan VL") }
        Button(onClick = onRecipes, modifier = Modifier.fillMaxWidth()) { Text("Recipes") }
    }
}

@Composable
private fun HubScreen(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        content()
    }
}
