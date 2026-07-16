package com.jasonrandazza.pixelpantry.generate

data class GenerateUiState(
    val ingredientNames: List<String> = emptyList(),
    val isLoadingModel: Boolean = false,
    val downloadProgress: Float? = null,
    val isGenerating: Boolean = false,
    val recipeText: String = "",
    val savedRecipeId: String? = null,
    val error: String? = null,
)
