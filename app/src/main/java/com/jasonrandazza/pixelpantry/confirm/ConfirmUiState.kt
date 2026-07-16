package com.jasonrandazza.pixelpantry.confirm

import com.jasonrandazza.pixelpantry.domain.DetectedIngredient

data class ConfirmUiState(
    val items: List<DetectedIngredient> = emptyList(),
    val saveCompleted: Boolean = false,
    val error: String? = null,
)
