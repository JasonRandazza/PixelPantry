package com.jasonrandazza.pixelpantry.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory_items WHERE LOWER(name) IN (:names)")
    suspend fun findByNames(names: List<String>): List<InventoryEntity>

    @Upsert
    suspend fun upsertAll(entities: List<InventoryEntity>)
}
