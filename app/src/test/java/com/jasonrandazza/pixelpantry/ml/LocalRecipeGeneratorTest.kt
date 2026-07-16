package com.jasonrandazza.pixelpantry.ml

import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRecipeGeneratorTest {
    @Test
    fun promptListsAllIngredientsAndRequiresOnlyThem() {
        val prompt = LocalRecipeGenerator.buildPrompt(listOf("eggs", "spinach", "cheddar cheese"))

        assertTrue(prompt.contains("eggs, spinach, cheddar cheese"))
        assertTrue(prompt.contains("ONLY"))
        assertTrue(prompt.contains("Title:"))
        assertTrue(prompt.contains("Steps:"))
    }
}
