package com.jasonrandazza.pixelpantry.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class RecipeSource { HOME, ONE_OFF }

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val ingredientSummary: String,
    val createdAtEpochMs: Long,
    val source: String,
)
