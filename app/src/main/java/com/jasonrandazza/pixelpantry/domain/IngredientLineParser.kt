package com.jasonrandazza.pixelpantry.domain

object IngredientLineParser {
    fun parse(text: String): List<DetectedIngredient> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.isEmpty()) return@mapNotNull null
                val name = parts[0]
                if (name.isEmpty()) return@mapNotNull null
                val qty = parts.getOrNull(1)?.ifBlank { null } ?: "unknown"
                val conf = parts.getOrNull(2).toConfidence()
                DetectedIngredient(name = name, quantityLabel = qty, confidence = conf)
            }
            .toList()

    private fun String?.toConfidence(): Confidence = when (this?.lowercase()) {
        "high" -> Confidence.HIGH
        "medium" -> Confidence.MEDIUM
        "low" -> Confidence.LOW
        else -> Confidence.UNKNOWN
    }
}
