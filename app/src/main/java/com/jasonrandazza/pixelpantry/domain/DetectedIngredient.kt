package com.jasonrandazza.pixelpantry.domain

import java.util.UUID

enum class Confidence { HIGH, MEDIUM, LOW, UNKNOWN }

data class DetectedIngredient(
    val name: String,
    val quantityLabel: String = "unknown",
    val confidence: Confidence = Confidence.UNKNOWN,
    val id: String = UUID.randomUUID().toString(),
)
