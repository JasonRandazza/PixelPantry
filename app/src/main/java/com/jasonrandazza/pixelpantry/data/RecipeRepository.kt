package com.jasonrandazza.pixelpantry.data

import com.jasonrandazza.pixelpantry.data.db.RecipeDao
import com.jasonrandazza.pixelpantry.data.db.RecipeEntity
import com.jasonrandazza.pixelpantry.data.db.RecipeSource
import kotlinx.coroutines.flow.Flow

class RecipeRepository(private val dao: RecipeDao) {
    fun observeAll(): Flow<List<RecipeEntity>> = dao.observeAll()

    suspend fun getById(id: String): RecipeEntity? = dao.getById(id)

    suspend fun save(
        title: String,
        body: String,
        ingredientSummary: String,
        source: RecipeSource,
    ): RecipeEntity {
        val entity = RecipeEntity(
            title = title,
            body = body,
            ingredientSummary = ingredientSummary,
            createdAtEpochMs = System.currentTimeMillis(),
            source = source.name,
        )
        dao.insert(entity)
        return entity
    }
}
