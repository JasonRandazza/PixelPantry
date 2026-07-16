package com.jasonrandazza.pixelpantry.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientLineParserTest {
    @Test
    fun parsesPipeSeparatedLines() {
        val text = """
            eggs | 6 | high
            milk | unknown | medium
        """.trimIndent()
        val items = IngredientLineParser.parse(text)
        assertEquals(2, items.size)
        assertEquals("eggs", items[0].name)
        assertEquals("6", items[0].quantityLabel)
        assertEquals(Confidence.HIGH, items[0].confidence)
        assertEquals("milk", items[1].name)
        assertEquals(Confidence.MEDIUM, items[1].confidence)
    }

    @Test
    fun skipsBlankAndNonFoodNoiseLines() {
        val text = "\n\neggs | 1 | low\n"
        assertEquals(1, IngredientLineParser.parse(text).size)
    }

    @Test
    fun nameOnlyLineDefaultsQuantityAndConfidence() {
        val items = IngredientLineParser.parse("butter")
        assertEquals("butter", items.single().name)
        assertEquals("unknown", items.single().quantityLabel)
        assertEquals(Confidence.UNKNOWN, items.single().confidence)
    }
}
