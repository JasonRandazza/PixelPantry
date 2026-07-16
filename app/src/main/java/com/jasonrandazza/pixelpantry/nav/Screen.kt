package com.jasonrandazza.pixelpantry.nav

import com.jasonrandazza.pixelpantry.confirm.ConfirmMode
import com.jasonrandazza.pixelpantry.domain.DetectedIngredient

/** Product navigation shell. A simple back-stack of these replaces a nav-graph dependency. */
sealed class Screen {
    data object ModeChooser : Screen()
    data object HomeHub : Screen()
    data object OneOffHub : Screen()
    data class Scan(val mode: ConfirmMode) : Screen()
    data class Confirm(val mode: ConfirmMode, val initialText: String) : Screen()
    data object Inventory : Screen()
    data class Generate(val ingredients: List<DetectedIngredient>, val fromOneOff: Boolean) : Screen()
    data object RecipeList : Screen()
    data class RecipeDetail(val id: String) : Screen()
}
