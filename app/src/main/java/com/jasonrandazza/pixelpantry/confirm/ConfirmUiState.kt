package com.jasonrandazza.pixelpantry.confirm

import com.jasonrandazza.pixelpantry.domain.DetectedIngredient

data class ConfirmUiState(
    val items: List<DetectedIngredient> = emptyList(),
    val mode: ConfirmMode = ConfirmMode.HomeSave,
    val saveCompleted: Boolean = false,
    val confirmedItems: List<DetectedIngredient>? = null,
    val error: String? = null,
)
