package com.jasonrandazza.pixelpantry.data

import com.jasonrandazza.pixelpantry.data.db.RecipeDao
import com.jasonrandazza.pixelpantry.data.db.RecipeEntity
import com.jasonrandazza.pixelpantry.data.db.RecipeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeRecipeDao : RecipeDao {
    val saved = MutableStateFlow<List<RecipeEntity>>(emptyList())

    override fun observeAll(): Flow<List<RecipeEntity>> = saved
    override suspend fun getById(id: String): RecipeEntity? = saved.value.firstOrNull { it.id == id }
    override suspend fun insert(entity: RecipeEntity) {
        saved.value = saved.value + entity
    }
}

class RecipeRepositoryTest {
    @Test
    fun saveInsertsEntityWithSourceAndReturnsIt() = runBlocking {
        val dao = FakeRecipeDao()
        val repository = RecipeRepository(dao)

        val saved = repository.save(
            title = "Spinach Scramble",
            body = "Title: Spinach Scramble\nSteps:\n1. Cook it.",
            ingredientSummary = "eggs, spinach",
            source = RecipeSource.HOME,
        )

        assertEquals("Spinach Scramble", saved.title)
        assertEquals(RecipeSource.HOME.name, saved.source)
        assertEquals(saved, repository.getById(saved.id))
    }
}
